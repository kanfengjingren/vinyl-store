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
}
