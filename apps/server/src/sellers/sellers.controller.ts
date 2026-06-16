import { Body, Controller, Get, Param, ParseIntPipe, Patch, Req, UseGuards } from '@nestjs/common';
import { SellersService } from './sellers.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { RolesGuard } from '../auth/roles.guard';
import { Roles } from '../auth/roles.decorator';

@Controller('sellers')
export class SellersController {
  constructor(private readonly sellersService: SellersService) {}

  // ── 卖家更新自己的主页信息 ──
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('SELLER')
  @Patch('profile')
  updateProfile(@Req() req: any, @Body() body: any) {
    return this.sellersService.updateProfile(req.user.userId, body);
  }

  // ── 卖家统计（需登录为 SELLER）—— 放在 :id 前面避免路由冲突 ──
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('SELLER')
  @Get('stats/sales-trend')
  getSalesTrend(@Req() req: any) {
    return this.sellersService.getSalesTrend(req.user.userId);
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('SELLER')
  @Get('stats/category-distribution')
  getCategoryDistribution(@Req() req: any) {
    return this.sellersService.getCategoryDistribution(req.user.userId);
  }

  @Get(':id')
  findById(@Param('id', ParseIntPipe) id: number) {
    return this.sellersService.findById(id);
  }
}
