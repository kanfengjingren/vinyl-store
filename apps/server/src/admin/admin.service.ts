import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { SellerStatus } from '@prisma/client';

@Injectable()
export class AdminService {
  constructor(private prisma: PrismaService) {}

  async listSellers(status?: SellerStatus) {
    const where = status ? { status } : {};
    return this.prisma.seller.findMany({
      where,
      include: {
        user: { select: { id: true, email: true, name: true } },
        _count: { select: { albums: true } },
      },
      orderBy: { createdAt: 'desc' },
    });
  }

  async approveSeller(sellerId: number) {
    const seller = await this.prisma.seller.findUnique({ where: { id: sellerId } });
    if (!seller) throw new NotFoundException('卖家不存在');
    if (seller.status !== 'PENDING') {
      throw new BadRequestException('仅可审核待处理的卖家');
    }
    return this.prisma.seller.update({
      where: { id: sellerId },
      data: { status: 'APPROVED' },
      include: { user: { select: { id: true, email: true, name: true } } },
    });
  }

  async rejectSeller(sellerId: number) {
    const seller = await this.prisma.seller.findUnique({ where: { id: sellerId } });
    if (!seller) throw new NotFoundException('卖家不存在');
    if (seller.status !== 'PENDING') {
      throw new BadRequestException('仅可审核待处理的卖家');
    }
    return this.prisma.seller.update({
      where: { id: sellerId },
      data: { status: 'REJECTED' },
      include: { user: { select: { id: true, email: true, name: true } } },
    });
  }

  // ════════════════════════════════════════
  // 数据看板统计
  // ════════════════════════════════════════

  async getDashboardStats() {
    const todayStart = new Date();
    todayStart.setHours(0, 0, 0, 0);

    const [revenueResult, todayOrders, pendingSellers, totalAlbums] = await Promise.all([
      this.prisma.order.aggregate({
        _sum: { totalAmount: true },
        where: { status: { not: 'CANCELLED' } },
      }),
      this.prisma.order.count({
        where: { createdAt: { gte: todayStart } },
      }),
      this.prisma.seller.count({
        where: { status: 'PENDING' },
      }),
      this.prisma.album.count(),
    ]);

    return {
      totalRevenue: revenueResult._sum.totalAmount ?? 0,
      todayOrders,
      pendingSellers,
      totalAlbums,
    };
  }

  async getSalesTrend() {
    // 用原生 SQL 按日期聚合近 30 天销售额
    const rows: any[] = await this.prisma.$queryRaw`
      SELECT DATE(createdAt) as date, SUM(totalAmount) as amount
      FROM orders
      WHERE status != 'CANCELLED' AND createdAt >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
      GROUP BY DATE(createdAt)
      ORDER BY date ASC
    `;

    // 补齐缺失的日期
    const days: string[] = [];
    const amounts: number[] = [];
    const now = new Date();
    for (let i = 29; i >= 0; i--) {
      const d = new Date(now);
      d.setDate(d.getDate() - i);
      const key = d.toISOString().slice(0, 10); // YYYY-MM-DD
      days.push(key);
      const found = rows.find((r: any) => {
        const rd = r.date instanceof Date ? r.date.toISOString().slice(0, 10) : String(r.date).slice(0, 10);
        return rd === key;
      });
      amounts.push(found ? Number(found.amount) : 0);
    }

    return { days, amounts };
  }

  async getCategorySales() {
    const items = await this.prisma.orderItem.findMany({
      where: { order: { status: { not: 'CANCELLED' } } },
      include: {
        album: {
          include: { categories: { include: { category: true } } },
        },
      },
    });

    // 按分类汇总销售额
    const map: Record<string, number> = {};
    for (const item of items) {
      const cats = item.album?.categories ?? [];
      const amount = item.quantity * item.unitPrice;
      if (cats.length === 0) {
        map['未分类'] = (map['未分类'] ?? 0) + amount;
      } else {
        for (const ac of cats) {
          const name = ac.category.name;
          map[name] = (map[name] ?? 0) + amount;
        }
      }
    }

    const entries = Object.entries(map).sort((a, b) => b[1] - a[1]);
    return {
      categories: entries.map((e) => e[0]),
      amounts: entries.map((e) => e[1]),
    };
  }

  async getTopAlbums() {
    const groups = await this.prisma.orderItem.groupBy({
      by: ['albumId'],
      where: { order: { status: { not: 'CANCELLED' } } },
      _sum: { quantity: true },
      orderBy: { _sum: { quantity: 'desc' } },
      take: 10,
    });

    const albumIds = groups.map((g) => g.albumId).filter(Boolean) as number[];
    const albums = await this.prisma.album.findMany({
      where: { id: { in: albumIds } },
      select: { id: true, title: true, artist: true, coverUrl: true, price: true },
    });
    const albumMap = new Map(albums.map((a) => [a.id, a]));

    return groups
      .filter((g) => g.albumId && albumMap.has(g.albumId))
      .map((g) => {
        const album = albumMap.get(g.albumId!)!;
        return {
          albumId: g.albumId,
          title: album.title,
          artist: album.artist,
          coverUrl: album.coverUrl,
          totalSold: g._sum.quantity ?? 0,
          revenue: (g._sum.quantity ?? 0) * album.price,
        };
      });
  }
}
