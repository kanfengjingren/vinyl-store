import { Controller, Get, Patch, Post, Param, Body, ParseIntPipe, Req, UseGuards } from '@nestjs/common';
import { UsersService } from './users.service';
import { UpdateUserDto } from './dto/update-user.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@Controller('users')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @UseGuards(JwtAuthGuard)
  @Get('me/purchases')
  findPurchases(@Req() req: any) {
    return this.usersService.findPurchases(req.user.userId);
  }

  @Get(':id')
  findById(@Param('id', ParseIntPipe) id: number) {
    return this.usersService.findById(id);
  }

  @UseGuards(JwtAuthGuard)
  @Patch(':id')
  update(@Param('id', ParseIntPipe) id: number, @Body() dto: UpdateUserDto) {
    return this.usersService.update(id, dto);
  }

  @UseGuards(JwtAuthGuard)
  @Post('recharge')
  recharge(@Req() req: any, @Body() body: { amount: number }) {
    return this.usersService.recharge(req.user.userId, body.amount);
  }
}
