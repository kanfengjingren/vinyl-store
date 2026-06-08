import { Controller, Get, Post, Patch, Delete, Body, Param, ParseIntPipe, UseGuards, Req } from '@nestjs/common';
import { CartService } from './cart.service';
import { AddToCartDto } from './dto/add-to-cart.dto';
import { UpdateCartItemDto } from './dto/update-cart-item.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@UseGuards(JwtAuthGuard)
@Controller('cart')
export class CartController {
  constructor(private readonly cartService: CartService) {}

  @Get()
  getCart(@Req() req: any) {
    return this.cartService.getCart(req.user.userId);
  }

  @Post('items')
  addItem(@Req() req: any, @Body() dto: AddToCartDto) {
    return this.cartService.addItem(req.user.userId, dto);
  }

  @Patch('items/:id')
  updateItem(@Req() req: any, @Param('id', ParseIntPipe) id: number, @Body() dto: UpdateCartItemDto) {
    return this.cartService.updateItem(req.user.userId, id, dto);
  }

  @Delete('items/:id')
  removeItem(@Req() req: any, @Param('id', ParseIntPipe) id: number) {
    return this.cartService.removeItem(req.user.userId, id);
  }
}
