import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class CategoriesService {
  constructor(private prisma: PrismaService) {}

  findAll() {
    return this.prisma.category.findMany({ orderBy: { name: 'asc' } });
  }

  findBySlug(slug: string) {
    return this.prisma.category.findUnique({ where: { slug } });
  }

  async findOrCreate(name: string) {
    const slug = name
      .toLowerCase()
      .replace(/[^a-z0-9一-鿿]+/g, '-')
      .replace(/^-|-$/g, '')
      .replace(/-+/g, '-')
      || 'uncategorized';

    const existing = await this.prisma.category.findFirst({
      where: { OR: [{ slug }, { name }] },
    });
    if (existing) return existing;

    return this.prisma.category.create({ data: { name, slug } });
  }
}
