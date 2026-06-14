import { Body, Controller, Get, Post, Query, UseGuards, Req } from '@nestjs/common';
import { PlayHistoryService } from './play-history.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@UseGuards(JwtAuthGuard)
@Controller('play-history')
export class PlayHistoryController {
  constructor(private readonly playHistoryService: PlayHistoryService) {}

  @Post()
  record(@Req() req: any, @Body() body: { trackId: number; albumId: number }) {
    return this.playHistoryService.record(req.user.userId, body.trackId, body.albumId);
  }

  @Get()
  findAll(@Req() req: any, @Query('limit') limit?: string) {
    const n = limit ? parseInt(limit, 10) || 20 : 20;
    return this.playHistoryService.findAll(req.user.userId, Math.min(n, 50));
  }
}
