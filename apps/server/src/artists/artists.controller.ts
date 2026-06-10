import { Controller, Get, Post, Patch, Param, Query, Body } from '@nestjs/common';
import { ArtistsService } from './artists.service';

@Controller('artists')
export class ArtistsController {
  constructor(private readonly artistsService: ArtistsService) { }

  @Get('search')
  search(@Query('q') q: string) {
    return this.artistsService.search(q);
  }

  @Get()
  findAll() {
    return this.artistsService.findAll();
  }

  @Get(':slug')
  findBySlug(@Param('slug') slug: string) {
    return this.artistsService.findBySlug(slug);
  }

  @Post()
  create(@Body() body: { name: string; photo?: string; foundedYear?: number; country?: string; description?: string }) {
    return this.artistsService.create(body);
  }

  @Patch(':id')
  update(
    @Param('id') id: string,
    @Body() body: { name?: string; photo?: string; foundedYear?: number; country?: string; description?: string },
  ) {
    return this.artistsService.update(+id, body);
  }
}
