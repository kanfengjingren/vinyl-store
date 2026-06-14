import { Body, Controller, Get, Post, UseGuards, Req } from '@nestjs/common';
import { FavoritesService } from './favorites.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@UseGuards(JwtAuthGuard)
@Controller('favorites')
export class FavoritesController {
  constructor(private readonly favoritesService: FavoritesService) {}

  @Post()
  toggle(@Req() req: any, @Body('albumId') albumId: number) {
    return this.favoritesService.toggle(req.user.userId, albumId);
  }

  @Get()
  findAll(@Req() req: any) {
    return this.favoritesService.findAll(req.user.userId);
  }
}
