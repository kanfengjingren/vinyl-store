import { Controller, Post, Patch, Get, Param, ParseIntPipe, Body, Req, UseGuards } from '@nestjs/common';
import { FriendsService } from './friends.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@UseGuards(JwtAuthGuard)
@Controller('friends')
export class FriendsController {
  constructor(private readonly friendsService: FriendsService) {}

  @Post('request')
  sendRequest(@Req() req: any, @Body('receiverId') receiverId: number) {
    return this.friendsService.sendRequest(req.user.userId, receiverId);
  }

  @Patch(':id/accept')
  acceptRequest(@Param('id', ParseIntPipe) id: number, @Req() req: any) {
    return this.friendsService.acceptRequest(id, req.user.userId);
  }

  @Patch(':id/reject')
  rejectRequest(@Param('id', ParseIntPipe) id: number, @Req() req: any) {
    return this.friendsService.rejectRequest(id, req.user.userId);
  }

  @Get()
  getFriends(@Req() req: any) {
    return this.friendsService.getFriends(req.user.userId);
  }

  @Get('pending')
  getPendingRequests(@Req() req: any) {
    return this.friendsService.getPendingRequests(req.user.userId);
  }

  @Get('status/:userId')
  getFriendshipStatus(@Req() req: any, @Param('userId', ParseIntPipe) userId: number) {
    return this.friendsService.getFriendshipStatus(req.user.userId, userId);
  }
}
