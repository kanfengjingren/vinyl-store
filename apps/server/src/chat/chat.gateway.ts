import {
  WebSocketGateway, WebSocketServer, SubscribeMessage,
  OnGatewayConnection, OnGatewayDisconnect,
  ConnectedSocket, MessageBody,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { JwtService } from '@nestjs/jwt';
import { ChatService } from './chat.service';

@WebSocketGateway({ namespace: '/chat', cors: { origin: '*' } })
export class ChatGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer() server!: Server;

  private userSockets = new Map<number, Set<string>>();

  constructor(
    private readonly chatService: ChatService,
    private readonly jwtService: JwtService,
  ) {}

  async handleConnection(client: Socket) {
    try {
      const token = client.handshake.auth?.token || client.handshake.query?.token;
      if (!token) {
        console.log('[Chat] 连接被拒：缺少 token');
        client.disconnect();
        return;
      }

      const clean = String(token).replace('Bearer ', '');
      const payload = this.jwtService.verify(clean);
      const userId = payload.sub;

      client.data.userId = userId;
      console.log(`[Chat] 用户 ${userId} 已连接 (socket: ${client.id})`);

      if (!this.userSockets.has(userId)) {
        this.userSockets.set(userId, new Set());
      }
      this.userSockets.get(userId)!.add(client.id);
      client.join(`user:${userId}`);
    } catch (err: any) {
      console.log(`[Chat] 连接鉴权失败: ${err?.message || err}`);
      client.disconnect();
    }
  }

  handleDisconnect(client: Socket) {
    const userId = client.data?.userId;
    if (!userId) return;
    const sockets = this.userSockets.get(userId);
    if (sockets) {
      sockets.delete(client.id);
      if (sockets.size === 0) this.userSockets.delete(userId);
    }
  }

  @SubscribeMessage('sendMessage')
  async handleMessage(
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { receiverId: number; content?: string; imageUrl?: string },
  ) {
    const senderId = client.data.userId;
    const hasContent = data.content?.trim();
    const hasImage = !!data.imageUrl;

    if (!senderId || !data.receiverId || (!hasContent && !hasImage)) {
      console.log('[Chat] sendMessage 参数不完整', { senderId, receiverId: data.receiverId });
      return;
    }

    const message = await this.chatService.saveMessage(
      senderId,
      data.receiverId,
      data.content?.trim() || '',
      data.imageUrl,
    );
    console.log(`[Chat] 消息已存储: ${senderId} → ${data.receiverId}: ${hasImage ? '[图片]' : data.content?.trim().slice(0, 30)}`);

    // 同时发给发送者和接收者
    this.server.to(`user:${data.receiverId}`).emit('newMessage', message);
    this.server.to(`user:${senderId}`).emit('newMessage', message);
    return message;
  }
}
