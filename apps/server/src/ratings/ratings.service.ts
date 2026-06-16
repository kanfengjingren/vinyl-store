import { Injectable, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class RatingsService {
  constructor(private prisma: PrismaService) {}

  /** 评分或修改评分 */
  async upsertRating(userId: number, albumId: number, score: number) {
    if (!Number.isInteger(score) || score < 1 || score > 5) {
      throw new BadRequestException('评分必须为 1-5 的整数');
    }

    return this.prisma.rating.upsert({
      where: { userId_albumId: { userId, albumId } },
      update: { score },
      create: { userId, albumId, score },
    });
  }

  /** 获取专辑评分统计（含可选当前用户评分） */
  async getAlbumRating(albumId: number, userId?: number) {
    const [aggregate, userRating] = await Promise.all([
      this.prisma.rating.aggregate({
        where: { albumId },
        _avg: { score: true },
        _count: true,
      }),
      userId
        ? this.prisma.rating.findUnique({
            where: { userId_albumId: { userId, albumId } },
            select: { score: true },
          })
        : Promise.resolve(null),
    ]);

    return {
      avgScore: aggregate._avg.score
        ? Math.round(aggregate._avg.score * 10) / 10
        : 0,
      count: aggregate._count,
      userRating: userRating?.score ?? null,
    };
  }
}
