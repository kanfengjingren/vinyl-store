import { Injectable, BadRequestException, NotFoundException, ForbiddenException, Inject, forwardRef } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { ChatGateway } from '../chat/chat.gateway';

@Injectable()
export class FriendsService {
  constructor(
    private prisma: PrismaService,
    @Inject(forwardRef(() => ChatGateway))
    private chatGateway: ChatGateway,
  ) {}

  /** 发送好友申请 */
  async sendRequest(senderId: number, receiverId: number) {
    if (senderId === receiverId) throw new BadRequestException('不能添加自己为好友');

    // 检查对方是否普通用户（不能添加商家或管理员）
    const receiver = await this.prisma.user.findUnique({ where: { id: receiverId } });
    if (!receiver) throw new NotFoundException('用户不存在');
    if (receiver.role !== 'CUSTOMER') throw new BadRequestException('只能添加普通用户为好友');

    // 检查是否已有好友关系
    const existing = await this.prisma.friendship.findUnique({
      where: { senderId_receiverId: { senderId, receiverId } },
    });
    if (existing) {
      if (existing.status === 'PENDING') throw new BadRequestException('已发送过好友申请，请等待对方处理');
      if (existing.status === 'ACCEPTED') throw new BadRequestException('你们已经是好友了');
      // REJECTED → 允许重新发送
      if (existing.status === 'REJECTED') {
        const updated = await this.prisma.friendship.update({
          where: { id: existing.id },
          data: { status: 'PENDING' },
        });
        await this.sendFriendRequestMessage(senderId, receiverId, updated.id, 'pending');
        return updated;
      }
    }

    // 检查反向关系（对方已经向我发过申请）
    const reverse = await this.prisma.friendship.findUnique({
      where: { senderId_receiverId: { senderId: receiverId, receiverId: senderId } },
    });
    if (reverse) {
      if (reverse.status === 'PENDING') throw new BadRequestException('对方已向你发送好友申请，请先处理');
      if (reverse.status === 'ACCEPTED') throw new BadRequestException('你们已经是好友了');
    }

    const friendship = await this.prisma.friendship.create({
      data: { senderId, receiverId },
    });

    // 发送消息通知
    await this.sendFriendRequestMessage(senderId, receiverId, friendship.id, 'pending');

    return friendship;
  }

  /** 接受好友申请 */
  async acceptRequest(friendshipId: number, userId: number) {
    const friendship = await this.prisma.friendship.findUnique({
      where: { id: friendshipId },
    });
    if (!friendship) throw new NotFoundException('好友申请不存在');
    if (friendship.receiverId !== userId) throw new ForbiddenException('只能处理发给你的好友申请');
    if (friendship.status !== 'PENDING') throw new BadRequestException('该申请已处理');

    const updated = await this.prisma.friendship.update({
      where: { id: friendshipId },
      data: { status: 'ACCEPTED' },
      include: {
        sender: { select: { id: true, name: true, avatar: true } },
        receiver: { select: { id: true, name: true, avatar: true } },
      },
    });

    // 更新原始申请消息的状态（防止刷新后按钮再生效）
    await this.updateRequestMessage(friendship.senderId, friendship.receiverId, friendshipId, 'accepted');

    // 发消息通知申请人
    await this.sendFriendRequestMessage(userId, friendship.senderId, friendshipId, 'accepted');

    return updated;
  }

  /** 拒绝好友申请 */
  async rejectRequest(friendshipId: number, userId: number) {
    const friendship = await this.prisma.friendship.findUnique({
      where: { id: friendshipId },
    });
    if (!friendship) throw new NotFoundException('好友申请不存在');
    if (friendship.receiverId !== userId) throw new ForbiddenException('只能处理发给你的好友申请');
    if (friendship.status !== 'PENDING') throw new BadRequestException('该申请已处理');

    const updated = await this.prisma.friendship.update({
      where: { id: friendshipId },
      data: { status: 'REJECTED' },
    });

    // 更新原始申请消息的状态
    await this.updateRequestMessage(friendship.senderId, friendship.receiverId, friendshipId, 'rejected');

    return updated;
  }

  /** 获取好友列表 */
  async getFriends(userId: number) {
    const friendships = await this.prisma.friendship.findMany({
      where: {
        status: 'ACCEPTED',
        OR: [{ senderId: userId }, { receiverId: userId }],
      },
      include: {
        sender: { select: { id: true, name: true, avatar: true } },
        receiver: { select: { id: true, name: true, avatar: true } },
      },
      orderBy: { updatedAt: 'desc' },
    });

    return friendships.map((f) => {
      const friend = f.senderId === userId ? f.receiver : f.sender;
      return { friendshipId: f.id, friend, createdAt: f.createdAt };
    });
  }

  /** 获取待处理的好友申请（我收到的） */
  async getPendingRequests(userId: number) {
    return this.prisma.friendship.findMany({
      where: { receiverId: userId, status: 'PENDING' },
      include: {
        sender: { select: { id: true, name: true, avatar: true } },
      },
      orderBy: { createdAt: 'desc' },
    });
  }

  /** 获取与某用户的好友状态 */
  async getFriendshipStatus(userId: number, targetId: number) {
    if (userId === targetId) return { status: 'self' };

    const [f1, f2] = await Promise.all([
      this.prisma.friendship.findUnique({ where: { senderId_receiverId: { senderId: userId, receiverId: targetId } } }),
      this.prisma.friendship.findUnique({ where: { senderId_receiverId: { senderId: targetId, receiverId: userId } } }),
    ]);

    const friendship = f1 || f2;
    if (!friendship) return { status: 'none' };
    return {
      status: friendship.status.toLowerCase(),
      friendshipId: friendship.id,
      isSender: f1 ? true : false,
    };
  }

  /** 更新原始好友申请消息的状态 */
  private async updateRequestMessage(
    senderId: number,
    receiverId: number,
    friendshipId: number,
    status: 'accepted' | 'rejected',
  ) {
    const oldContent = JSON.stringify({ type: 'friend_request', friendshipId, status: 'pending' });
    const newContent = JSON.stringify({ type: 'friend_request', friendshipId, status });

    await this.prisma.message.updateMany({
      where: {
        senderId,
        receiverId,
        content: oldContent,
      },
      data: { content: newContent },
    });
  }

  /** 发送好友申请消息 */
  private async sendFriendRequestMessage(
    senderId: number,
    receiverId: number,
    friendshipId: number,
    status: 'pending' | 'accepted',
  ) {
    const data = { type: 'friend_request', friendshipId, status };
    const content = JSON.stringify(data);

    const message = await this.prisma.message.create({
      data: { senderId, receiverId, content },
      include: {
        sender: { select: { id: true, name: true, avatar: true } },
      },
    });

    // Socket.IO 实时推送
    this.chatGateway.server.to(`user:${receiverId}`).emit('newMessage', message);
    this.chatGateway.pushUnreadCount(receiverId);

    return message;
  }
}
