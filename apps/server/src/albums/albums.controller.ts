import { Body, Controller, Delete, Get, Param, ParseIntPipe, Patch, Post, Query, UseGuards, Req } from '@nestjs/common';
import { AlbumsService } from './albums.service';
import { QueryAlbumsDto } from './dto/query-albums.dto';
import { CreateAlbumDto } from './dto/create-album.dto';
import { UpdateAlbumDto } from './dto/update-album.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { RolesGuard } from '../auth/roles.guard';
import { Roles } from '../auth/roles.decorator';
import { ALL_COLORS } from '../common/color';

@Controller('albums')
export class AlbumsController {

  constructor(private readonly albumsService: AlbumsService) {}

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('SELLER')
  @Get('mine')
  findMyAlbums(@Req() req: any, @Query() query: QueryAlbumsDto) {
    return this.albumsService.findMyAlbums(req.user.userId, query.page, query.limit);
  }

  @Get('colors')
  getColors() {
    return ALL_COLORS;
  }

  @Get('suggest')
  suggest(@Query('q') q: string) {
    return this.albumsService.suggest(q || '');
  }

  @Get('countries')
  getCountries() {
    return this.albumsService.getCountries();
  }

  @Get()
  findAll(@Query() query: QueryAlbumsDto) {
    return this.albumsService.findAll(query);
  }

  @Get('hot')
  findHotAlbums(@Query('limit') limit?: string) {
    const n = limit ? parseInt(limit, 10) || 12 : 12;
    return this.albumsService.findHotAlbums(Math.min(n, 50));
  }

  @UseGuards(JwtAuthGuard)
  @Get('recommendations')
  getRecommendations(@Req() req: any, @Query('limit') limit?: string) {
    const n = limit ? parseInt(limit, 10) || 12 : 12;
    return this.albumsService.getRecommendations(req.user.userId, Math.min(n, 24));
  }

  @Get(':slug')
  findBySlug(@Param('slug') slug: string) {
    return this.albumsService.findBySlug(slug);
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('SELLER')
  @Post()
  createAlbum(@Req() req: any, @Body() body: CreateAlbumDto) {
    return this.albumsService.createAlbum(req.user.userId, body);
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('SELLER')
  @Patch(':id')
  updateAlbum(@Req() req: any, @Param('id', ParseIntPipe) id: number, @Body() body: UpdateAlbumDto) {
    return this.albumsService.updateAlbum(req.user.userId, id, body);
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('SELLER')
  @Delete(':id')
  deleteAlbum(@Req() req: any, @Param('id', ParseIntPipe) id: number) {
    return this.albumsService.deleteAlbum(req.user.userId, id);
  }
}
