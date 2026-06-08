import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { AddToCartDto } from './dto/add-to-cart.dto';
import { UpdateCartItemDto } from './dto/update-cart-item.dto';

@Injectable()
export class CartService {
  constructor(private prisma: PrismaService) {}

  async getCart(userId: number) {
    const cart = await this.prisma.cart.findUnique({
      where: { userId },
      include: {
        items: {
          include: { album: { include: { categories: { include: { category: true } } } } },
        },
      },
    });

    if (!cart) return { items: [], total: 0 };

    const items = cart.items.map((item) => ({
      id: item.id,
      quantity: item.quantity,
      album: {
        ...item.album,
        categories: item.album.categories.map((ac) => ac.category),
      },
    }));

    const total = items.reduce((sum, item) => sum + item.album.price * item.quantity, 0);

    return { items, total };
  }

  async addItem(userId: number, dto: AddToCartDto) {
    const album = await this.prisma.album.findUnique({ where: { id: dto.albumId } });
    if (!album) throw new NotFoundException('专辑不存在');
    if (album.stock < dto.quantity) throw new BadRequestException('库存不足');

    const cart = await this.ensureCart(userId);

    const existing = await this.prisma.cartItem.findFirst({
      where: { cartId: cart.id, albumId: dto.albumId },
    });

    if (existing) {
      const newQty = existing.quantity + dto.quantity;
      if (album.stock < newQty) throw new BadRequestException('库存不足');
      await this.prisma.cartItem.update({
        where: { id: existing.id },
        data: { quantity: newQty },
      });
    } else {
      await this.prisma.cartItem.create({
        data: { cartId: cart.id, albumId: dto.albumId, quantity: dto.quantity },
      });
    }

    return this.getCart(userId);
  }

  async updateItem(userId: number, itemId: number, dto: UpdateCartItemDto) {
    const cart = await this.ensureCart(userId);
    const item = await this.prisma.cartItem.findFirst({
      where: { id: itemId, cartId: cart.id },
      include: { album: true },
    });

    if (!item) throw new NotFoundException('购物车条目不存在');

    if (dto.quantity === 0) {
      await this.prisma.cartItem.delete({ where: { id: itemId } });
    } else {
      if (item.album.stock < dto.quantity) throw new BadRequestException('库存不足');
      await this.prisma.cartItem.update({
        where: { id: itemId },
        data: { quantity: dto.quantity },
      });
    }

    return this.getCart(userId);
  }

  async removeItem(userId: number, itemId: number) {
    const cart = await this.ensureCart(userId);
    const item = await this.prisma.cartItem.findFirst({
      where: { id: itemId, cartId: cart.id },
    });

    if (!item) throw new NotFoundException('购物车条目不存在');

    await this.prisma.cartItem.delete({ where: { id: itemId } });
    return this.getCart(userId);
  }

  private async ensureCart(userId: number) {
    let cart = await this.prisma.cart.findUnique({ where: { userId } });
    if (!cart) {
      cart = await this.prisma.cart.create({ data: { userId } });
    }
    return cart;
  }
}
