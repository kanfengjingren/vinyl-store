import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { QueryAlbumsDto } from './dto/query-albums.dto';
import { CreateAlbumDto } from './dto/create-album.dto';
import { UpdateAlbumDto } from './dto/update-album.dto';
import { extractColorFromImage } from '../common/color';
import * as path from 'path';

@Injectable()
export class AlbumsService {
  constructor(private prisma: PrismaService) { }

  async findAll(query: QueryAlbumsDto) {
    const { category, country, search, sort, order = 'asc', page = 1, limit = 12 } = query;

    const where: any = { status: 'ACTIVE' };

    if (category) {
      where.categories = { some: { category: { slug: category } } };
    }

    if (country) {
      where.country = country;
    }

    if (query.color) {
      where.color = query.color;
    }

    if (search) {
      where.OR = [
        { artist: { contains: search } },
        { title: { contains: search } },
      ];
    }

    const orderBy: any = {};
    if (sort) {
      orderBy[sort] = order;
    }

    const [albums, total] = await Promise.all([
      this.prisma.album.findMany({
        where,
        include: {
          artistRel: { select: { id: true, name: true, slug: true } },
          seller: { select: { id: true, storeName: true } },
          categories: { include: { category: true } },
          _count: { select: { tracks: true } },
        },
        orderBy: sort ? orderBy : { id: 'asc' },
        skip: (page - 1) * limit,
        take: limit,
      }),
      this.prisma.album.count({ where }),
    ]);

    return {
      data: albums.map(({ categories, _count, artistRel, ...album }) => ({
        ...album,
        artistInfo: artistRel,
        categories: categories.map((ac) => ac.category),
        trackCount: _count.tracks,
      })),
      pagination: {
        page,
        limit,
        total,
        totalPages: Math.ceil(total / limit),
      },
    };
  }

  async findBySlug(slug: string) {
    const album = await this.prisma.album.findUnique({
      where: { slug },
      include: {
        artistRel: { select: { id: true, name: true, slug: true, photo: true } },
        seller: { select: { id: true, storeName: true } },
        tracks: { orderBy: { position: 'asc' } },
        categories: { include: { category: true } },
      },
    });

    if (!album || album.status === 'DELISTED') {
      throw new NotFoundException(`Album "${slug}" not found`);
    }

    const { artistRel, ...rest } = album;
    return {
      ...rest,
      artistInfo: artistRel,
      categories: album.categories.map((ac) => ac.category),
    };
  }

  async getCountries() {
    const rows = await this.prisma.album.findMany({
      where: { status: 'ACTIVE', country: { not: '' } },
      select: { country: true },
      distinct: ['country'],
      orderBy: { country: 'asc' },
    });
    return rows.map((r) => r.country).filter(Boolean);
  }

  async suggest(q: string, limit = 5) {
    if (!q?.trim()) return [];
    return this.prisma.album.findMany({
      where: {
        status: 'ACTIVE',
        OR: [
          { artist: { contains: q.trim() } },
          { title: { contains: q.trim() } },
        ],
      },
      select: {
        id: true, artist: true, title: true, coverUrl: true, slug: true,
        artistRel: { select: { id: true, name: true, slug: true } },
      },
      take: limit,
      orderBy: { id: 'desc' },
    });
  }

  /** 根据 artist+title 生成 slug，如果重复则追加 -2, -3, ... */
  private async generateSlug(artist: string, title: string): Promise<string> {
    const base = `${artist}-${title}`
      .toLowerCase()
      .replace(/[^a-z0-9一-鿿]+/g, '-')
      .replace(/^-|-$/g, '')
      .replace(/-+/g, '-')
      || 'album';

    let slug = base;
    let counter = 2;
    while (await this.prisma.album.findUnique({ where: { slug } })) {
      slug = `${base}-${counter}`;
      counter++;
    }
    return slug;
  }

  async createAlbum(userId: number, body: CreateAlbumDto) {
    const seller = await this.prisma.seller.findUnique({ where: { userId } });
    if (!seller) throw new ForbiddenException('只有已入驻的卖家才能上架专辑');
    if (seller.status !== 'APPROVED') throw new ForbiddenException('卖家审核通过后才能上架专辑');

    // 如果传了 artistId，从 Artist 表取 name 填充 artist 字符串（向后兼容）
    let artistName = body.artist;
    if (!artistName && body.artistId) {
      const artist = await this.prisma.artist.findUnique({ where: { id: body.artistId } });
      if (artist) artistName = artist.name;
    }

    let slug = body.slug?.trim() || await this.generateSlug(artistName || 'unknown', body.title);

    const coverUrl = body.coverUrl ?? '';

    // 重试最多 3 次，避免竞态条件导致的 slug 冲突
    for (let attempt = 0; attempt < 3; attempt++) {
      try {
        const album = await this.prisma.album.create({
          data: {
            artist: artistName || 'Unknown',
            artistId: body.artistId ?? null,
            title: body.title,
            year: body.year ?? null,
            price: body.price,
            slug,
            stock: body.stock ?? 10,
            country: body.country ?? '',
            badge: body.badge ?? '',
            description: body.description ?? '',
            coverUrl,
            gradient: body.gradient ?? '',
            label: seller.storeName,
            sellerId: seller.id,
            status: 'ACTIVE',
            categories: body.categories?.length
              ? { create: body.categories.map((slug) => ({ category: { connect: { slug } } })) }
              : undefined,
          },
        });

        // 异步提取封面颜色（不阻塞返回）
        if (coverUrl) {
          // coverUrl 可能是 "/uploads/covers/file.png" 或 "uploads/covers/file.png"
          // 统一去掉开头的 / 和 uploads/ 前缀，只保留 "covers/file.png"
          const relativeUrl = coverUrl.replace(/^\/?uploads\//, '');
          const base = process.env.UPLOADS_BASE_PATH || path.join(__dirname, '..', '..', 'uploads');
          const filePath = path.join(base, relativeUrl);
          extractColorFromImage(filePath).then((color) => {
            if (color) {
              this.prisma.album.update({ where: { id: album.id }, data: { color } }).catch((e) => {
                console.error('更新 album.color 失败:', e.message);
              });
            }
          }).catch((e) => {
            console.error('提取封面颜色失败:', e.message);
          });
        }

        return album;
      } catch (err: any) {
        if (err?.code === 'P2002' && attempt < 2) {
          slug = `${slug.replace(/-\\d{13}$/, '')}-${Date.now()}`;
          continue;
        }
        throw err;
      }
    }
    throw new Error('创建专辑失败，请重试');
  }

  async updateAlbum(userId: number, id: number, dto: UpdateAlbumDto) {
    const album = await this.prisma.album.findUnique({ where: { id }, include: { seller: true } });
    if (!album) throw new NotFoundException(`Album #${id} not found`);
    if (album.status === 'DELISTED') throw new ForbiddenException('已下架的专辑无法编辑');
    if (!album.seller || album.seller.userId !== userId) {
      throw new ForbiddenException('你只能编辑自己厂牌下的专辑');
    }
    return this.prisma.album.update({ where: { id }, data: dto });
  }

  async deleteAlbum(userId: number, id: number) {
    const album = await this.prisma.album.findUnique({ where: { id }, include: { seller: true } });
    if (!album) throw new NotFoundException(`Album #${id} not found`);
    if (album.status === 'DELISTED') throw new ForbiddenException('该专辑已下架');
    if (!album.seller || album.seller.userId !== userId) {
      throw new ForbiddenException('你只能删除自己厂牌下的专辑');
    }

    // 软删除：清购物车 + 标记为 DELISTED，保留数据供订单引用
    return this.prisma.$transaction([
      this.prisma.cartItem.deleteMany({ where: { albumId: id } }),
      this.prisma.album.update({ where: { id }, data: { status: 'DELISTED' } }),
    ]);
  }

  /** 卖家查看自己的专辑（含已下架） */
  async findMyAlbums(userId: number, page = 1, limit = 12) {
    const seller = await this.prisma.seller.findUnique({ where: { userId } });
    if (!seller) throw new ForbiddenException('只有入驻卖家可查看');

    const where = { sellerId: seller.id };

    const [albums, total] = await Promise.all([
      this.prisma.album.findMany({
        where,
        include: {
          artistRel: { select: { id: true, name: true, slug: true } },
          categories: { include: { category: true } },
          _count: { select: { tracks: true } },
        },
        orderBy: { createdAt: 'desc' },
        skip: (page - 1) * limit,
        take: limit,
      }),
      this.prisma.album.count({ where }),
    ]);

    return {
      data: albums.map(({ categories, _count, artistRel, ...album }) => ({
        ...album,
        artistInfo: artistRel,
        categories: categories.map((ac) => ac.category),
        trackCount: _count.tracks,
      })),
      pagination: { page, limit, total, totalPages: Math.ceil(total / limit) },
    };
  }
}
