import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class ChatService {
  constructor(private prisma: PrismaService) {}

  async saveMessage(senderId: number, receiverId: number, content: string, imageUrl?: string) {
    return this.prisma.message.create({
      data: { senderId, receiverId, content: content || '', imageUrl: imageUrl || null },
      include: {
        sender: { select: { id: true, name: true } },
      },
    });
  }

  async getConversation(userId1: number, userId2: number) {
    return this.prisma.message.findMany({
      where: {
        OR: [
          { senderId: userId1, receiverId: userId2 },
          { senderId: userId2, receiverId: userId1 },
        ],
      },
      include: {
        sender: { select: { id: true, name: true } },
      },
      orderBy: { createdAt: 'asc' },
      take: 100,
    });
  }

  async getConversationList(userId: number) {
    // 找所有跟我有消息往来的用户
    const sent = await this.prisma.message.findMany({
      where: { senderId: userId },
      select: { receiverId: true, receiver: { select: { id: true, name: true, email: true } } },
      distinct: ['receiverId'],
    });

    const received = await this.prisma.message.findMany({
      where: { receiverId: userId },
      select: { senderId: true, sender: { select: { id: true, name: true, email: true } } },
      distinct: ['senderId'],
    });

    const partnerMap = new Map<number, { id: number; name: string | null; email: string }>();
    for (const r of sent) {
      partnerMap.set(r.receiverId, r.receiver);
    }
    for (const r of received) {
      if (!partnerMap.has(r.senderId)) {
        partnerMap.set(r.senderId, r.sender);
      }
    }

    // 查哪些 partner 是卖家，拿厂牌名
    const partnerIds = [...partnerMap.keys()];
    const sellers = partnerIds.length > 0
      ? await this.prisma.seller.findMany({
          where: { userId: { in: partnerIds } },
          select: { userId: true, storeName: true },
        })
      : [];
    const sellerMap = new Map(sellers.map((s) => [s.userId, s.storeName]));

    // 为每个 partner 获取最后一条消息和未读数
    const result: Array<{
      partner: { id: number; name: string | null; email: string; storeName: string | null };
      lastMsg: { content: string; createdAt: Date; senderId: number } | null;
      unreadCount: number;
    }> = [];

    for (const [partnerId, partner] of partnerMap) {
      const lastMsg = await this.prisma.message.findFirst({
        where: {
          OR: [
            { senderId: userId, receiverId: partnerId },
            { senderId: partnerId, receiverId: userId },
          ],
        },
        orderBy: { createdAt: 'desc' },
        select: { content: true, createdAt: true, senderId: true },
      });

      const unreadCount = await this.prisma.message.count({
        where: { senderId: partnerId, receiverId: userId, read: false },
      });

      result.push({
        partner: {
          id: partner.id,
          name: partner.name,
          email: partner.email,
          storeName: sellerMap.get(partner.id) || null,
        },
        lastMsg,
        unreadCount,
      });
    }

    // 按最后消息时间排序
    result.sort((a, b) => {
      const ta = a.lastMsg?.createdAt ? new Date(a.lastMsg.createdAt).getTime() : 0;
      const tb = b.lastMsg?.createdAt ? new Date(b.lastMsg.createdAt).getTime() : 0;
      return tb - ta;
    });

    return result;
  }

  async markAsRead(senderId: number, receiverId: number) {
    return this.prisma.message.updateMany({
      where: { senderId, receiverId, read: false },
      data: { read: true },
    });
  }

  async getUnreadCount(userId: number) {
    return this.prisma.message.count({
      where: { receiverId: userId, read: false },
    });
  }

  async markAllAsRead(userId: number) {
    return this.prisma.message.updateMany({
      where: { receiverId: userId, read: false },
      data: { read: true },
    });
  }
}
