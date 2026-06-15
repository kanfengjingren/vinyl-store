import {
  Body, Controller, Get, Headers, Param, ParseIntPipe,
  Post, Req, UseGuards,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { RatingsService } from './ratings.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@Controller()
export class RatingsController {
  constructor(
    private readonly ratingsService: RatingsService,
    private readonly jwtService: JwtService,
  ) {}

  /** 获取专辑评分统计（公开；如已登录则返回当前用户评分） */
  @Get('albums/:albumId/rating')
  async getRating(
    @Param('albumId', ParseIntPipe) albumId: number,
    @Headers('authorization') authHeader?: string,
  ) {
    let userId: number | undefined;
    if (authHeader?.startsWith('Bearer ')) {
      try {
        const payload = this.jwtService.verify(authHeader.slice(7));
        userId = payload.sub;
      } catch {
        // token 无效或过期，忽略
      }
    }
    return this.ratingsService.getAlbumRating(albumId, userId);
  }

  /** 评分或修改评分（需登录） */
  @UseGuards(JwtAuthGuard)
  @Post('albums/:albumId/rating')
  rate(
    @Req() req: any,
    @Param('albumId', ParseIntPipe) albumId: number,
    @Body('score') score: number,
  ) {
    return this.ratingsService.upsertRating(req.user.userId, albumId, score);
  }
}
