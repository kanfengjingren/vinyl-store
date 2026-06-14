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
      select: { id: true, name: true, avatar: true, createdAt: true },
    });
    if (!user) throw new NotFoundException('用户不存在');
    return user;
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
