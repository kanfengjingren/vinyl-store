import { Body, Controller, Get,Post, Param, ParseIntPipe } from '@nestjs/common';
import { TracksService } from './tracks.service';
import {CreateTracksDto} from './dto/create-tracks.dto'
@Controller('albums/:albumId/tracks')
export class TracksController {
  constructor(private readonly tracksService: TracksService) {}

  @Get()
  findByAlbum(@Param('albumId', ParseIntPipe) albumId: number) {
    return this.tracksService.findByAlbum(albumId);
  }

  @Post()
  createByAlbum(@Param('albumId', ParseIntPipe) albumId: number,
  @Body() body:any){
    return this.tracksService.createByAlbum(albumId,body)
  }
}
