import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class PlayHistoryService {
  constructor(private prisma: PrismaService) {}

  /** 记录一次播放 */
  async record(userId: number, trackId: number, albumId: number) {
    return this.prisma.playHistory.create({
      data: { userId, trackId, albumId },
    });
  }

  /** 获取用户最近播放历史 */
  async findAll(userId: number, limit: number = 20) {
    const history = await this.prisma.playHistory.findMany({
      where: { userId },
      include: {
        track: {
          select: { id: true, title: true, duration: true, position: true, audioUrl: true, albumId: true },
        },
        album: {
          select: { id: true, artist: true, title: true, coverUrl: true, gradient: true, slug: true },
        },
      },
      orderBy: { playedAt: 'desc' },
      take: limit,
    });

    return history;
  }
}
