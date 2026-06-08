import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class TracksService {
  constructor(private prisma: PrismaService) { }

  async findByAlbum(albumId: number) {
    const album = await this.prisma.album.findUnique({
      where: { id: albumId },
      select: { id: true },
    });
    if (!album) throw new NotFoundException(`Album #${albumId} not found`);

    return this.prisma.track.findMany({
      where: { albumId },
      orderBy: { position: 'asc' },
    });
  }

  async createByAlbum(albumId: number,body: any) {
    const {  tracks } = body
    const album = await this.prisma.album.findUnique({ where: { id: albumId } });
    if (!album) throw new NotFoundException(`Album #${albumId} not found`);
    
    
    const data = tracks.map(track => ({
      ...track,
      albumId,
    }));

    await this.prisma.track.createMany({ data });

    return "创建成功"
  }
}
