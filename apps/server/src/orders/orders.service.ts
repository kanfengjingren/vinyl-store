import { Injectable, BadRequestException, NotFoundException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class OrdersService {
  constructor(private prisma: PrismaService) {}

  async checkout(userId: number, shippingAddress?: string) {
    // 1. Read cart with items
    const cart = await this.prisma.cart.findUnique({
      where: { userId },
      include: { items: { include: { album: true } } },
    });

    if (!cart || cart.items.length === 0) {
      throw new BadRequestException('购物车为空');
    }

    // Resolve shipping address
    if (!shippingAddress) {
      const user = await this.prisma.user.findUnique({ where: { id: userId } });
      shippingAddress = user?.defaultAddress || undefined;
    }
    if (!shippingAddress) {
      throw new BadRequestException('请填写收货地址');
    }
    

    // 2. Validate stock & ownership for all items
    for (const item of cart.items) {
      if (item.album.stock < item.quantity) {
        throw new BadRequestException(`"${item.album.title}" 库存不足`);
      }
      // 检查是否已购买且已发货
      const owned = await this.prisma.orderItem.findFirst({
        where: {
          albumId: item.albumId,
          status: { in: ['ACTIVE', 'SHIPPED'] },
          order: { userId, status: { in: ['PAID', 'DELIVERED'] } },
        },
      });
      if (owned) {
        throw new BadRequestException(`"${item.album.title}" 已购买且已发货，不可重复购买`);
      }
    }

    // 3. Execute transaction: create order → snapshot prices → decrement stock → clear cart
    const order = await this.prisma.$transaction(async (tx) => {
      const totalAmount = cart.items.reduce(
        (sum, item) => sum + item.album.price * item.quantity,
        0,
      );

      const expiresAt = new Date(Date.now() + 15 * 60 * 1000);

      const created = await tx.order.create({
        data: {
          userId,
          totalAmount,
          shippingAddress,
          expiresAt,
          items: {
            create: cart.items.map((item) => ({
              albumId: item.albumId,
              quantity: item.quantity,
              unitPrice: item.album.price,
            })),
          },
        },
        include: { items: true },
      });

      // Decrement stock for each album
      for (const item of cart.items) {
        await tx.album.update({
          where: { id: item.albumId },
          data: { stock: { decrement: item.quantity } },
        });
      }

      // Clear cart
      await tx.cartItem.deleteMany({ where: { cartId: cart.id } });

      return created;
    });

    return order;
  }

  async findAll(userId: number) {
    // Lazy-cancel expired PENDING orders
    const now = new Date();
    const expiredOrders = await this.prisma.order.findMany({
      where: { userId, status: 'PENDING', expiresAt: { lt: now } },
      include: { items: true },
    });
    for (const o of expiredOrders) {
      await this.prisma.$transaction(async (tx) => {
        for (const item of o.items) {
          await tx.album.update({
            where: { id: item.albumId! },
            data: { stock: { increment: item.quantity } },
          });
        }
        await tx.order.update({
          where: { id: o.id },
          data: { status: 'CANCELLED' },
        });
      });
    }

    return this.prisma.order.findMany({
      where: { userId },
      include: {
        items: {
          include: {
            album: { select: { id: true, artist: true, title: true, coverUrl: true, gradient: true, slug: true, status: true } },
          },
        },
      },
      orderBy: { createdAt: 'desc' },
    });
  }

  async findById(userId: number, orderId: number) {
    const order = await this.prisma.order.findFirst({
      where: { id: orderId, userId },
      include: {
        items: {
          include: {
            album: { select: { id: true, artist: true, title: true, coverUrl: true, gradient: true, slug: true, status: true } },
          },
        },
      },
    });

    if (!order) throw new NotFoundException('订单不存在');
    return order;
  }

  async cancelOrder(userId: number, orderId: number) {
    const order = await this.prisma.order.findFirst({
      where: { id: orderId, userId },
      include: { items: true },
    });

    if (!order) throw new NotFoundException('订单不存在');
    if (order.status !== 'PENDING') {
      throw new BadRequestException('仅待付款订单可取消');
    }

    return this.prisma.$transaction(async (tx) => {
      for (const item of order.items) {
        await tx.album.update({
          where: { id: item.albumId! },
          data: { stock: { increment: item.quantity } },
        });
      }
      return tx.order.update({
        where: { id: orderId },
        data: { status: 'CANCELLED' },
      });
    });
  }

  async payOrder(userId: number, orderId: number) {
    const order = await this.prisma.order.findFirst({
      where: { id: orderId, userId },
      include: { items: { include: { album: true } } },
    });

    if (!order) throw new NotFoundException('订单不存在');
    if (order.status !== 'PENDING') {
      throw new BadRequestException('仅待付款订单可支付');
    }
    if (order.expiresAt && new Date() > order.expiresAt) {
      throw new BadRequestException('订单已超时，请重新下单');
    }

    // ── 二次校验：逐项检查商品状态 ──
    const validItems: Array<{ id: number; albumId: number; quantity: number; unitPrice: number; priceChanged: boolean }> = [];
    const invalidItems: Array<{ id: number; albumId: number | null; quantity: number; reason: string }> = [];
    let newTotal = 0;

    for (const item of order.items) {
      const album = item.album;

      // 专辑已被硬删除
      if (!album) {
        invalidItems.push({ id: item.id, albumId: item.albumId, quantity: item.quantity, reason: '专辑已不存在' });
        continue;
      }

      // 已下架
      if (album.status === 'DELISTED') {
        invalidItems.push({ id: item.id, albumId: album.id, quantity: item.quantity, reason: `「${album.title}」已下架` });
        continue;
      }

      // 库存不足
      if (album.stock < item.quantity) {
        invalidItems.push({ id: item.id, albumId: album.id, quantity: item.quantity, reason: `「${album.title}」库存不足` });
        continue;
      }

      // 价格检查：用当前价格重新计算
      const priceChanged = album.price !== item.unitPrice;
      newTotal += album.price * item.quantity;
      validItems.push({ id: item.id, albumId: album.id, quantity: item.quantity, unitPrice: album.price, priceChanged });
    }

    // 全部无效 → 直接取消订单
    if (validItems.length === 0) {
      return this.prisma.$transaction(async (tx) => {
        for (const item of invalidItems) {
          if (item.albumId) {
            await tx.album.update({ where: { id: item.albumId }, data: { stock: { increment: item.quantity } } });
          }
          await tx.orderItem.update({ where: { id: item.id }, data: { status: 'REFUNDED' } });
        }
        return tx.order.update({ where: { id: orderId }, data: { status: 'CANCELLED', totalAmount: 0 } });
      });
    }

    // 检查余额
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user || user.balance < newTotal) {
      throw new BadRequestException(`账户余额不足（需 ¥${newTotal}，当前 ¥${user?.balance ?? 0}）`);
    }

    // 执行付款
    return this.prisma.$transaction(async (tx) => {
      // 扣余额
      await tx.user.update({ where: { id: userId }, data: { balance: { decrement: newTotal } } });

      // 退款无效项（恢复库存）
      for (const item of invalidItems) {
        if (item.albumId) {
          await tx.album.update({ where: { id: item.albumId }, data: { stock: { increment: item.quantity } } });
        }
        await tx.orderItem.update({ where: { id: item.id }, data: { status: 'REFUNDED' } });
      }

      // 更新变价项的 unitPrice
      for (const item of validItems) {
        if (item.priceChanged) {
          await tx.orderItem.update({ where: { id: item.id }, data: { unitPrice: item.unitPrice } });
        }
      }

      // 更新订单
      return tx.order.update({
        where: { id: orderId },
        data: { status: 'PAID', totalAmount: newTotal },
        include: { items: true },
      });
    });
  }

  async refundOrderItem(userId: number, orderId: number, itemId: number) {
    const seller = await this.prisma.seller.findUnique({ where: { userId } });
    if (!seller) throw new ForbiddenException('只有入驻卖家可退款');

    const order = await this.prisma.order.findUnique({
      where: { id: orderId },
      include: {
        items: {
          include: { album: { select: { id: true, sellerId: true, status: true, stock: true } } },
        },
      },
    });
    if (!order) throw new NotFoundException('订单不存在');
    if (order.status !== 'PAID') {
      throw new BadRequestException('仅已付款未发货的订单可退款');
    }

    const item = order.items.find((i) => i.id === itemId);
    if (!item) throw new NotFoundException('订单项不存在');
    if (!item.album) throw new BadRequestException('该专辑已不存在');
    if (item.album.sellerId !== seller.id) throw new ForbiddenException('只能退款自己厂牌的专辑');
    if (item.album.status !== 'DELISTED') throw new BadRequestException('仅已下架的专辑可退款');
    if (item.status === 'REFUNDED') throw new BadRequestException('该商品已退款');

    const refundAmount = item.unitPrice * item.quantity;

    return this.prisma.$transaction(async (tx) => {
      await tx.orderItem.update({
        where: { id: itemId },
        data: { status: 'REFUNDED' },
      });
      await tx.album.update({
        where: { id: item.albumId! },
        data: { stock: { increment: item.quantity } },
      });
      // 退款退回买家余额
      await tx.user.update({
        where: { id: order.userId },
        data: { balance: { increment: refundAmount } },
      });
      await tx.order.update({
        where: { id: orderId },
        data: { totalAmount: { decrement: refundAmount } },
      });
      // 如果该订单所有 item 都已退款 → 订单标记为 CANCELLED
      const remainingActive = await tx.orderItem.count({
        where: { orderId, status: 'ACTIVE' },
      });
      if (remainingActive === 0) {
        await tx.order.update({
          where: { id: orderId },
          data: { status: 'CANCELLED' },
        });
      }
    });

    return { message: '退款成功', refundAmount };
  }

  async getSellerOrders(userId: number) {
    const seller = await this.prisma.seller.findUnique({ where: { userId } });
    if (!seller) throw new ForbiddenException('只有入驻卖家可查看订单');

    const orders = await this.prisma.order.findMany({
      where: {
        status: { in: ['PAID', 'DELIVERED'] },
        items: { some: { album: { sellerId: seller.id } } },
      },
      include: {
        user: { select: { id: true, name: true, email: true } },
        items: {
          include: {
            album: { select: { id: true, artist: true, title: true, coverUrl: true, gradient: true, slug: true, status: true, sellerId: true } },
          },
        },
      },
      orderBy: { createdAt: 'desc' },
    });

    // 只保留当前卖家的 orderItem，过滤掉其他卖家的 item
    return orders.map((order) => ({
      ...order,
      items: order.items.filter((item) => item.album?.sellerId === seller.id),
    }));
  }

  async shipOrder(userId: number, orderId: number) {
    const seller = await this.prisma.seller.findUnique({ where: { userId } });
    if (!seller) throw new ForbiddenException('只有入驻卖家可发货');

    //订单：根据订单id去找，
    const order = await this.prisma.order.findUnique({
      where: { id: orderId },
      include: { items: { include: { album: { select: { sellerId: true, status: true } } } } },
    });

    if (!order) throw new NotFoundException('订单不存在');
    if (order.status !== 'PAID') {
      throw new BadRequestException('仅已付款订单可发货');
    }

    // 卖家在此订单中的 item  同时筛选出只含有sellerid的这一部分item
    const myItems = order.items.filter((item) => item.album?.sellerId === seller.id);
    if (!myItems.length) throw new ForbiddenException('你只能发自己厂牌专辑的订单');

    // 必须有至少一个 ACTIVE 的 item 才能发货（全部已退款或已发货则不能发）
    const hasActive = myItems.some((item) => item.status === 'ACTIVE');
    if (!hasActive) throw new BadRequestException('该订单中你的商品已全部处理，无法重复发货');

    // 已下架但未退款的专辑必须先退款，不能直接发货
    const delistedUnrefunded = myItems.some(
      (item) => item.status === 'ACTIVE' && item.album?.status === 'DELISTED',
    );
    if (delistedUnrefunded) {
      throw new BadRequestException('该订单中包含已下架专辑，请先完成退款后再发货');
    }

    
    return this.prisma.$transaction(async (tx) => {
      // 将该卖家的 ACTIVE item 标记为 SHIPPED，并计算货款
      let sellerRevenue = 0;
      for (const item of myItems) {
        if (item.status === 'ACTIVE') {
          await tx.orderItem.update({
            where: { id: item.id },
            data: { status: 'SHIPPED' },
          });
          sellerRevenue += item.unitPrice * item.quantity;
        }
      }

      // 卖家余额到账
      if (sellerRevenue > 0) {
        await tx.seller.update({
          where: { id: seller.id },
          data: { balance: { increment: sellerRevenue } },
        });
      }

      // 检查是否所有 item 都已 SHIPPED 或 REFUNDED（无 ACTIVE 剩余）
      const remainingActive = await tx.orderItem.count({
        where: { orderId, status: 'ACTIVE' },
      });
      if (remainingActive === 0) {
        await tx.order.update({
          where: { id: orderId },
          data: { status: 'DELIVERED' },
        });
      }

      return tx.order.findUnique({
        where: { id: orderId },
        include: { items: true },
      });
    });
  }
}
