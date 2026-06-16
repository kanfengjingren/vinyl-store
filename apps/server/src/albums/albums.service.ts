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
    const { category, country, search, sort, order = 'asc', page = 1, limit = 12, date } = query;

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

    if (date) {
      const startOfDay = new Date(date);
      const endOfDay = new Date(date);
      endOfDay.setDate(endOfDay.getDate() + 1);
      where.createdAt = { gte: startOfDay, lt: endOfDay };
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

  /** 热销专辑：近30天销量排行 */
  async findHotAlbums(limit: number = 12) {
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

    const result = await this.prisma.orderItem.groupBy({
      by: ['albumId'],
      where: {
        status: { in: ['ACTIVE', 'SHIPPED'] },
        albumId: { not: null },
        order: {
          status: { in: ['PAID', 'DELIVERED'] },
          createdAt: { gte: thirtyDaysAgo },
        },
      },
      _sum: { quantity: true },
      orderBy: { _sum: { quantity: 'desc' } },
      take: limit,
    });

    const albumIds = result.map((r) => r.albumId).filter(Boolean) as number[];

    if (albumIds.length === 0) {
      return { data: [] };
    }

    const albums = await this.prisma.album.findMany({
      where: { id: { in: albumIds }, status: 'ACTIVE' },
      include: {
        artistRel: { select: { id: true, name: true, slug: true } },
        seller: { select: { id: true, storeName: true } },
        categories: { include: { category: true } },
        _count: { select: { tracks: true } },
      },
    });

    // albumId → 销量
    const salesMap = new Map(
      result.map((r) => [r.albumId!, r._sum.quantity ?? 0]),
    );

    // 按销量排序
    const albumMap = new Map(albums.map((a) => [a.id, a]));
    const sorted = albumIds
      .map((id) => albumMap.get(id))
      .filter((a): a is typeof albums[number] => a != null)
      .map(({ categories, _count, artistRel, ...album }) => ({
        ...album,
        artistInfo: artistRel,
        categories: categories.map((ac) => ac.category),
        trackCount: _count.tracks,
        hotSales: salesMap.get(album.id) ?? 0,
      }));

    return { data: sorted };
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

  /** 个性化推荐：基于用户的播放/收藏/评分/购买记录 */
  async getRecommendations(userId: number, limit: number = 12) {
    // 1. 收集用户交互过的专辑 ID
    const [playedAlbums, favoritedAlbums, ratedAlbums, purchasedAlbums] = await Promise.all([
      this.prisma.playHistory.findMany({
        where: { userId },
        select: { albumId: true },
        distinct: ['albumId'],
        take: 30,
      }),
      this.prisma.favorite.findMany({
        where: { userId },
        select: { albumId: true },
      }),
      this.prisma.rating.findMany({
        where: { userId },
        select: { albumId: true },
      }),
      this.prisma.orderItem.findMany({
        where: { order: { userId, status: { in: ['PAID', 'DELIVERED'] } } },
        select: { albumId: true },
        distinct: ['albumId'],
      }),
    ]);

    const interactedIds = [
      ...new Set([
        ...playedAlbums.map((p) => p.albumId),
        ...favoritedAlbums.map((f) => f.albumId),
        ...ratedAlbums.map((r) => r.albumId),
        ...purchasedAlbums.filter((p) => p.albumId != null).map((p) => p.albumId!),
      ]),
    ];

    // 2. 如果用户没有任何交互，回退到热门推荐
    if (interactedIds.length === 0) {
      const hot = await this.findHotAlbums(limit);
      return { data: hot.data, reason: 'hot' };
    }

    // 3. 提取用户偏好的分类和艺人
    const interactedAlbums = await this.prisma.album.findMany({
      where: { id: { in: interactedIds } },
      select: {
        artist: true,
        categories: { include: { category: { select: { slug: true } } } },
      },
    });

    const categoryScores = new Map<string, number>(); // slug -> weight
    const artistSet = new Set<string>();
    for (const album of interactedAlbums) {
      artistSet.add(album.artist);
      for (const ac of album.categories) {
        categoryScores.set(
          ac.category.slug,
          (categoryScores.get(ac.category.slug) || 0) + 1,
        );
      }
    }
    const topCategories = [...categoryScores.entries()]
      .sort((a, b) => b[1] - a[1])
      .slice(0, 5)
      .map(([slug]) => slug);
    const topArtists = [...artistSet].slice(0, 10);

    // 4. 候选专辑：同分类 or 同艺人，排除已交互
    const candidates = await this.prisma.album.findMany({
      where: {
        status: 'ACTIVE',
        id: { notIn: interactedIds },
        OR: [
          { categories: { some: { category: { slug: { in: topCategories } } } } },
          { artist: { in: topArtists } },
        ],
      },
      include: {
        artistRel: { select: { id: true, name: true, slug: true } },
        categories: { include: { category: true } },
        _count: { select: { tracks: true } },
        ratings: { select: { score: true } },
      },
      take: 60,
    });

    // 5. 评分加权
    const scored = candidates.map((album) => {
      const albumCats = album.categories.map((ac) => ac.category.slug);
      const catMatch = albumCats.filter((c) => topCategories.includes(c)).length;
      const artistMatch = topArtists.includes(album.artist) ? 1 : 0;
      const avgRating =
        album.ratings.length > 0
          ? album.ratings.reduce((s, r) => s + r.score, 0) / album.ratings.length
          : 0;

      const score = catMatch * 10 + artistMatch * 20 + avgRating * 2;

      const { ratings, categories, _count, artistRel, ...rest } = album;
      return {
        ...rest,
        artistInfo: artistRel,
        categories: categories.map((ac) => ac.category),
        trackCount: _count.tracks,
        avgRating: Math.round(avgRating * 10) / 10,
        score,
      };
    });

    // 6. 按分数降序 + 随机微调避免每次相同
    scored.sort((a, b) => b.score - a.score);
    const top = scored.slice(0, limit);

    // 随机打乱前6个位置让推荐看起来有新鲜感
    for (let i = Math.min(5, top.length - 1); i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [top[i], top[j]] = [top[j], top[i]];
    }

    return { data: top, reason: 'personalized' };
  }
}
