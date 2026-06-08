import { Controller, Get, Patch, Post, Param, ParseIntPipe, UseGuards, Req, Body } from '@nestjs/common';
import { OrdersService } from './orders.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { RolesGuard } from '../auth/roles.guard';
import { Roles } from '../auth/roles.decorator';

@UseGuards(JwtAuthGuard)
@Controller('orders')
export class OrdersController {
  constructor(private readonly ordersService: OrdersService) {}

  @Post()
  checkout(@Req() req: any, @Body() body: { shippingAddress?: string }) {
    return this.ordersService.checkout(req.user.userId, body.shippingAddress);
  }

  // ⚠️ 静态路由必须放在 :id 前面，否则 "seller" 会被 ParseIntPipe 拦截

  @UseGuards(RolesGuard)
  @Roles('SELLER')
  @Get('seller')
  getSellerOrders(@Req() req: any) {
    return this.ordersService.getSellerOrders(req.user.userId);
  }

  @Get()
  findAll(@Req() req: any) {
    return this.ordersService.findAll(req.user.userId);
  }

  @Get(':id')
  findById(@Req() req: any, @Param('id', ParseIntPipe) id: number) {
    return this.ordersService.findById(req.user.userId, id);
  }

  @Patch(':id/cancel')
  cancelOrder(@Req() req: any, @Param('id', ParseIntPipe) id: number) {
    return this.ordersService.cancelOrder(req.user.userId, id);
  }

  @Patch(':id/pay')
  payOrder(@Req() req: any, @Param('id', ParseIntPipe) id: number) {
    return this.ordersService.payOrder(req.user.userId, id);
  }

  @UseGuards(RolesGuard)
  @Roles('SELLER')
  @Patch(':id/items/:itemId/refund')
  refundOrderItem(@Req() req: any, @Param('id', ParseIntPipe) id: number, @Param('itemId', ParseIntPipe) itemId: number) {
    return this.ordersService.refundOrderItem(req.user.userId, id, itemId);
  }

  @UseGuards(RolesGuard)
  @Roles('SELLER')
  @Patch(':id/ship')
  shipOrder(@Req() req: any, @Param('id', ParseIntPipe) id: number) {
    return this.ordersService.shipOrder(req.user.userId, id);
  }
}
