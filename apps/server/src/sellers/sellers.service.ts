import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class SellersService {
  constructor(private prisma: PrismaService) {}

  async findById(id: number) {
    const seller = await this.prisma.seller.findUnique({
      where: { id, status: 'APPROVED' },
      include: {
        albums: {
          where: { status: 'ACTIVE' },
          include: {
            categories: { include: { category: true } },
            artistRel: { select: { id: true, name: true, slug: true } },
          },
          orderBy: { createdAt: 'desc' },
        },
        _count: { select: { albums: true } },
      },
    });

    if (!seller) {
      throw new NotFoundException('卖家不存在或未通过审核');
    }

    // 跟 Artist 页面一样，把 categories join 表拍平
    const { albums, _count, ...rest } = seller;
    return {
      ...rest,
      albumCount: _count.albums,
      albums: albums.map(({ categories, artistRel, ...album }) => ({
        ...album,
        artistInfo: artistRel,
        categories: categories.map((ac) => ac.category),
      })),
    };
  }

  // ── 卖家数据统计 ──
  async getSalesTrend(userId: number) {
    const seller = await this.prisma.seller.findUnique({ where: { userId } });
    if (!seller) throw new NotFoundException('卖家不存在');

    const rows: any[] = await this.prisma.$queryRaw`
      SELECT DATE(o.createdAt) as date, SUM(oi.quantity * oi.unitPrice) as amount
      FROM order_items oi
      JOIN \`orders\` o ON oi.orderId = o.id
      JOIN albums a ON oi.albumId = a.id
      WHERE o.status != 'CANCELLED'
        AND a.sellerId = ${seller.id}
        AND o.createdAt >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
      GROUP BY DATE(o.createdAt)
      ORDER BY date ASC
    `;

    // 补齐缺失的日期
    const days: string[] = [];
    const amounts: number[] = [];
    const now = new Date();
    for (let i = 29; i >= 0; i--) {
      const d = new Date(now);
      d.setDate(d.getDate() - i);
      const key = d.toISOString().slice(0, 10);
      days.push(key);
      const found = rows.find((r: any) => {
        const rd = r.date instanceof Date ? r.date.toISOString().slice(0, 10) : String(r.date).slice(0, 10);
        return rd === key;
      });
      amounts.push(found ? Number(found.amount) : 0);
    }

    return { days, amounts };
  }

  // ── 卖家专辑分类分布 ──
  async getCategoryDistribution(userId: number) {
    const seller = await this.prisma.seller.findUnique({ where: { userId } });
    if (!seller) throw new NotFoundException('卖家不存在');

    const albums = await this.prisma.album.findMany({
      where: { sellerId: seller.id, status: 'ACTIVE' },
      include: { categories: { include: { category: true } } },
    });

    // 按分类统计专辑数（一专多分类时各算一次）
    const map: Record<string, number> = {};
    for (const album of albums) {
      const cats = album.categories;
      if (cats.length === 0) {
        map['未分类'] = (map['未分类'] ?? 0) + 1;
      } else {
        for (const ac of cats) {
          const name = ac.category.name;
          map[name] = (map[name] ?? 0) + 1;
        }
      }
    }

    const entries = Object.entries(map).sort((a, b) => b[1] - a[1]);
    return {
      categories: entries.map((e) => e[0]),
      counts: entries.map((e) => e[1]),
    };
  }
}
