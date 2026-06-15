import { Controller, Get, Patch, Post, Param, Body, ParseIntPipe, Req, UseGuards } from '@nestjs/common';
import { UsersService } from './users.service';
import { UpdateUserDto } from './dto/update-user.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@Controller('users')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  // ── 需要认证的路由（固定路径优先，避免被 :id 捕获） ──

  @UseGuards(JwtAuthGuard)
  @Get('me/purchases')
  findPurchases(@Req() req: any) {
    return this.usersService.findPurchases(req.user.userId);
  }

  @UseGuards(JwtAuthGuard)
  @Patch('me/privacy')
  updatePrivacy(@Req() req: any, @Body() dto: { showPurchases?: boolean; showFavorites?: boolean }) {
    return this.usersService.updatePrivacy(req.user.userId, dto);
  }

  @UseGuards(JwtAuthGuard)
  @Patch('me/avatar')
  updateAvatar(@Req() req: any, @Body('avatar') avatar: string) {
    return this.usersService.updateAvatar(req.user.userId, avatar);
  }

  @UseGuards(JwtAuthGuard)
  @Post('recharge')
  recharge(@Req() req: any, @Body() body: { amount: number }) {
    return this.usersService.recharge(req.user.userId, body.amount);
  }

  // ── 公开路由（:id 参数化路径） ──

  @Get(':id/profile')
  findPublicProfile(@Param('id', ParseIntPipe) id: number) {
    return this.usersService.findPublicProfile(id);
  }

  @Get(':id/purchases')
  findPublicPurchases(@Param('id', ParseIntPipe) id: number) {
    return this.usersService.findPublicPurchases(id);
  }

  @Get(':id/favorites')
  findPublicFavorites(@Param('id', ParseIntPipe) id: number) {
    return this.usersService.findPublicFavorites(id);
  }

  @Get(':id/seller-albums')
  findPublicSellerAlbums(@Param('id', ParseIntPipe) id: number) {
    return this.usersService.findPublicSellerAlbums(id);
  }

  @Get(':id')
  findById(@Param('id', ParseIntPipe) id: number) {
    return this.usersService.findById(id);
  }

  @UseGuards(JwtAuthGuard)
  @Patch(':id')
  update(@Param('id', ParseIntPipe) id: number, @Body() dto: UpdateUserDto) {
    return this.usersService.update(id, dto);
  }
}
