import { Controller, Get, Param, ParseIntPipe, Patch, Req, UseGuards } from '@nestjs/common';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { ChatService } from './chat.service';

@UseGuards(JwtAuthGuard)
@Controller('chat')
export class ChatController {
  constructor(private readonly chatService: ChatService) {}

  @Get('conversations')
  getConversations(@Req() req: any) {
    return this.chatService.getConversationList(req.user.userId);
  }

  @Get('messages/:userId')
  getMessages(@Req() req: any, @Param('userId', ParseIntPipe) userId: number) {
    return this.chatService.getConversation(req.user.userId, userId);
  }

  @Patch('read/:userId')
  markRead(@Req() req: any, @Param('userId', ParseIntPipe) userId: number) {
    return this.chatService.markAsRead(userId, req.user.userId);
  }
}
