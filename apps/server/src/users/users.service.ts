import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { UpdateUserDto } from './dto/update-user.dto';

@Injectable()
export class UsersService {
  constructor(private prisma: PrismaService) {}

  async findById(id: number) {
    const user = await this.prisma.user.findUnique({ where: { id } });
    if (!user) throw new NotFoundException('用户不存在');
    return { id: user.id, email: user.email, name: user.name, role: user.role, balance: user.balance };
  }

  async update(id: number, dto: UpdateUserDto) {
    await this.findById(id);
    const user = await this.prisma.user.update({ where: { id }, data: dto });
    return { id: user.id, email: user.email, name: user.name, role: user.role, balance: user.balance };
  }

  async recharge(userId: number, amount: number) {
    if (!amount || amount <= 0) throw new BadRequestException('充值金额必须大于 0');
    const user = await this.prisma.user.update({
      where: { id: userId },
      data: { balance: { increment: amount } },
    });
    return { balance: user.balance };
  }
}
