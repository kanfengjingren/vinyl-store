import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { UpdateUserDto } from './dto/update-user.dto';

@Injectable()
export class UsersService {
  constructor(private prisma: PrismaService) {}

  async findById(id: number) {
    const user = await this.prisma.user.findUnique({ where: { id } });
    if (!user) throw new NotFoundException('用户不存在');
    return { id: user.id, email: user.email, name: user.name, role: user.role, balance: user.balance, avatar: user.avatar };
  }

  async update(id: number, dto: UpdateUserDto) {
    await this.findById(id);
    const user = await this.prisma.user.update({ where: { id }, data: dto });
    return { id: user.id, email: user.email, name: user.name, role: user.role, balance: user.balance };
  }

  async findPublicProfile(userId: number) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: {
        id: true, name: true, avatar: true, createdAt: true, role: true,
        seller: { select: { id: true, storeName: true, description: true, status: true } },
      },
    });
    if (!user) throw new NotFoundException('用户不存在');
    return user;
  }

  /** 公开获取某商家的专辑列表（仅已通过审核的卖家） */
  async findPublicSellerAlbums(userId: number) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { role: true, seller: { select: { id: true, status: true } } },
    });
    if (!user || user.role !== 'SELLER' || !user.seller || user.seller.status !== 'APPROVED') {
      return { data: [] };
    }

    const albums = await this.prisma.album.findMany({
      where: { sellerId: user.seller.id, status: 'ACTIVE' },
      include: {
        artistRel: { select: { id: true, name: true, slug: true } },
        categories: { include: { category: true } },
        _count: { select: { tracks: true } },
      },
      orderBy: { createdAt: 'desc' },
    });

    return {
      data: albums.map(({ categories, _count, artistRel, ...album }) => ({
        ...album,
        artistInfo: artistRel,
        categories: categories.map((ac) => ac.category),
        trackCount: _count.tracks,
      })),
    };
  }

  async updateAvatar(userId: number, avatar: string) {
    await this.findById(userId);
    const user = await this.prisma.user.update({ where: { id: userId }, data: { avatar } });
    return { avatar: user.avatar };
  }

  async recharge(userId: number, amount: number) {
    if (!amount || amount <= 0) throw new BadRequestException('充值金额必须大于 0');
    const user = await this.prisma.user.update({
      where: { id: userId },
      data: { balance: { increment: amount } },
    });
    return { balance: user.balance };
  }

  /** 公开获取某用户的已购专辑（尊重隐私设置） */
  async findPublicPurchases(userId: number) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { showPurchases: true },
    });
    if (!user) throw new NotFoundException('用户不存在');
    if (!user.showPurchases) return { visible: false, data: [] };

    const purchases = await this.findPurchases(userId);
    return { visible: true, data: purchases };
  }

  /** 公开获取某用户的收藏专辑（尊重隐私设置） */
  async findPublicFavorites(userId: number) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: { showFavorites: true },
    });
    if (!user) throw new NotFoundException('用户不存在');
    if (!user.showFavorites) return { visible: false, data: [] };

    const favs = await this.prisma.favorite.findMany({
      where: { userId },
      include: {
        album: {
          include: {
            artistRel: { select: { id: true, name: true, slug: true } },
            seller: { select: { id: true, storeName: true } },
            _count: { select: { tracks: true } },
          },
        },
      },
      orderBy: { createdAt: 'desc' },
    });

    return {
      visible: true,
      data: favs.map((f) => {
        const { artistRel, _count, ...album } = f.album;
        return {
          id: f.id,
          createdAt: f.createdAt,
          album: {
            ...album,
            artistInfo: artistRel,
            trackCount: _count.tracks,
          },
        };
      }),
    };
  }

  /** 更新隐私设置 */
  async updatePrivacy(userId: number, dto: { showPurchases?: boolean; showFavorites?: boolean }) {
    await this.findById(userId);
    const data: any = {};
    if (dto.showPurchases !== undefined) data.showPurchases = dto.showPurchases;
    if (dto.showFavorites !== undefined) data.showFavorites = dto.showFavorites;
    const user = await this.prisma.user.update({ where: { id: userId }, data });
    return {
      showPurchases: user.showPurchases,
      showFavorites: user.showFavorites,
    };
  }

  /** 获取用户已购买的专辑（PAID/DELIVERED 订单中的去重专辑 + 曲目列表） */
  async findPurchases(userId: number) {
    const orders = await this.prisma.order.findMany({
      where: {
        userId,
        status: { in: ['PAID', 'DELIVERED'] },
      },
      include: {
        items: {
          where: { status: { in: ['ACTIVE', 'SHIPPED'] } },
          include: {
            album: {
              include: {
                artistRel: { select: { id: true, name: true, slug: true } },
                seller: { select: { id: true, storeName: true } },
                tracks: { orderBy: { position: 'asc' } },
              },
            },
          },
        },
      },
    });

    // 去重：同一张专辑买了多次只保留一份
    const seen = new Map<number, any>();
    for (const order of orders) {
      for (const item of order.items) {
        if (!item.album || item.album.status === 'DELISTED') continue;
        if (!seen.has(item.album.id)) {
          const { tracks, artistRel, ...album } = item.album;
          seen.set(item.album.id, {
            ...album,
            artistInfo: artistRel,
            tracks,
          });
        }
      }
    }

    return [...seen.values()];
  }
}
