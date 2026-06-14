import { Injectable, NotFoundException, ForbiddenException, forwardRef, Inject } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateCommentDto } from './dto/create-comment.dto';
import { QueryCommentsDto } from './dto/query-comments.dto';
import { ChatGateway } from '../chat/chat.gateway';

@Injectable()
export class CommentsService {
  constructor(
    private prisma: PrismaService,
    @Inject(forwardRef(() => ChatGateway)) private chatGateway: ChatGateway,
  ) {}

  // 获取某张专辑的根评论（分页，每条附带前3条子回复）
  async findByAlbum(albumId: number, query: QueryCommentsDto) {
    const page = query.page ?? 1;
    const limit = query.limit ?? 10;
    const skip = (page - 1) * limit;

    const [data, total] = await Promise.all([
      this.prisma.comment.findMany({
        where: { albumId, parentId: null },
        include: {
          user: { select: { id: true, name: true, role: true, avatar: true } },
          replies: {
            include: {
              user: { select: { id: true, name: true, role: true, avatar: true } },
            },
            orderBy: { createdAt: 'asc' },
            take: 3,
          },
          _count: { select: { replies: true } },
        },
        orderBy: { createdAt: 'desc' },
        skip,
        take: limit,
      }),
      this.prisma.comment.count({ where: { albumId, parentId: null } }),
    ]);

    return {
      data,
      pagination: {
        page,
        limit,
        total,
        totalPages: Math.ceil(total / limit),
      },
    };
  }

  // 获取某条根评论的全部子回复
  async findReplies(commentId: number) {
    return this.prisma.comment.findMany({
      where: { parentId: commentId },
      include: {
        user: { select: { id: true, name: true, role: true, avatar: true } },
      },
      orderBy: { createdAt: 'asc' },
    });
  }

  // 发表评论或回复
  async create(userId: number, albumId: number, dto: CreateCommentDto) {
    if (dto.parentId) {
      // 找到被回复的评论
      const parent = await this.prisma.comment.findUnique({
        where: { id: dto.parentId },
        include: { album: { select: { id: true, title: true, slug: true } } },
      });
      if (!parent) throw new NotFoundException('父评论不存在');

      // 保持两级嵌套：回复子回复时提升到根评论
      const actualParentId = parent.parentId ?? parent.id;
      // 被回复人信息
      const replyToUserId = parent.userId;
      const replyToUserName = (await this.prisma.user.findUnique({ where: { id: parent.userId }, select: { name: true } }))?.name || '匿名';

      const comment = await this.prisma.comment.create({
        data: {
          albumId,
          userId,
          parentId: actualParentId,
          replyToUserId,
          replyToUserName,
          content: dto.content,
        },
        include: {
          user: { select: { id: true, name: true, role: true, avatar: true } },
        },
      });

      // 如果回复的不是自己的评论，发送消息通知
      if (replyToUserId !== userId) {
        await this.prisma.message.create({
          data: {
            senderId: userId,
            receiverId: replyToUserId,
            content: JSON.stringify({
              type: 'comment_reply',
              albumId: parent.album.id,
              albumTitle: parent.album.title,
              albumSlug: parent.album.slug,
              commentId: actualParentId,
              replyContent: dto.content,
            }),
          },
        });

        // 实时推送未读数给被回复者
        this.chatGateway.pushUnreadCount(replyToUserId);
      }

      return comment;
    }

    // 根评论
    return this.prisma.comment.create({
      data: {
        albumId,
        userId,
        content: dto.content,
      },
      include: {
        user: { select: { id: true, name: true, role: true, avatar: true } },
      },
    });
  }

  // 删除评论（只能删自己的）
  async delete(commentId: number, userId: number) {
    const comment = await this.prisma.comment.findUnique({
      where: { id: commentId },
    });
    if (!comment) throw new NotFoundException('评论不存在');
    if (comment.userId !== userId) throw new ForbiddenException('只能删除自己的评论');

    await this.prisma.comment.delete({ where: { id: commentId } });
    return { success: true };
  }
}
