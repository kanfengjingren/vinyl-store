import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

function slugify(text: string): string {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9一-鿿]+/g, '-')
    .replace(/^-|-$/g, '')
    .replace(/-+/g, '-')
    || 'artist';
}

@Injectable()
export class ArtistsService {
  constructor(private prisma: PrismaService) { }

  async findAll() {
    return this.prisma.artist.findMany({
      orderBy: { name: 'asc' },
      include: { _count: { select: { albums: true } } },
    });
  }

  async findBySlug(slug: string) {
    const artist = await this.prisma.artist.findUnique({
      where: { slug },
      include: {
        albums: {
          where: { status: 'ACTIVE' },
          include: {
            categories: { include: { category: true } },
          },
          orderBy: { createdAt: 'desc' },
        },
        _count: { select: { albums: true } },
      },
    });

    if (!artist) throw new NotFoundException(`Artist "${slug}" not found`);

    return {
      ...artist,
      albums: artist.albums.map(({ categories, ...album }) => ({
        ...album,
        categories: categories.map((ac) => ac.category),
      })),
    };
  }

  async search(q: string) {
    if (!q?.trim()) return [];
    return this.prisma.artist.findMany({
      where: { name: { contains: q.trim() } },
      select: { id: true, name: true, slug: true, photo: true },
      take: 10,
      orderBy: { name: 'asc' },
    });
  }

  async create(data: { name: string; photo?: string; foundedYear?: number; country?: string; description?: string }) {
    let slug = slugify(data.name);
    let counter = 2;
    while (await this.prisma.artist.findUnique({ where: { slug } })) {
      slug = `${slugify(data.name)}-${counter}`;
      counter++;
    }
    return this.prisma.artist.create({
      data: {
        name: data.name,
        slug,
        photo: data.photo ?? null,
        foundedYear: data.foundedYear ?? null,
        country: data.country ?? null,
        description: data.description ?? null,
      },
    });
  }

  async update(id: number, data: { name?: string; photo?: string; foundedYear?: number; country?: string; description?: string }) {
    const artist = await this.prisma.artist.findUnique({ where: { id } });
    if (!artist) throw new NotFoundException(`Artist #${id} not found`);

    const updateData: any = { ...data };
    if (data.name) {
      let slug = slugify(data.name);
      let counter = 2;
      while (await this.prisma.artist.findFirst({ where: { slug, id: { not: id } } })) {
        slug = `${slugify(data.name)}-${counter}`;
        counter++;
      }
      updateData.slug = slug;
    }

    return this.prisma.artist.update({ where: { id }, data: updateData });
  }
}
