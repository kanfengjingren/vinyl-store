import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class FavoritesService {
  constructor(private prisma: PrismaService) {}

  /** 切换收藏状态，返回 { favorited: boolean } */
  async toggle(userId: number, albumId: number) {
    const existing = await this.prisma.favorite.findFirst({
      where: { userId, albumId },
    });

    if (existing) {
      await this.prisma.favorite.delete({ where: { id: existing.id } });
      return { favorited: false };
    }

    await this.prisma.favorite.create({ data: { userId, albumId } });
    return { favorited: true };
  }

  /** 获取用户收藏列表（含专辑详情） */
  async findAll(userId: number) {
    const favorites = await this.prisma.favorite.findMany({
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

    return favorites.map((f) => {
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
    });
  }
}
