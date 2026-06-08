import { Injectable, UnauthorizedException, ConflictException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcryptjs';
import { PrismaService } from '../prisma/prisma.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';

@Injectable()
export class AuthService {
  constructor(
    private prisma: PrismaService,
    private jwt: JwtService,
  ) {}

  async register(dto: RegisterDto) {
    const existing = await this.prisma.user.findUnique({ where: { email: dto.email } });
    if (existing) throw new ConflictException('邮箱已被注册');

    const role = dto.role ?? 'CUSTOMER';
    if (role === 'SELLER' && !dto.storeName) {
      throw new ConflictException('卖家注册需填写厂牌名称');
    }

    const hashedPassword = await bcrypt.hash(dto.password, 10);

    const user = await this.prisma.user.create({
      data: {
        email: dto.email,
        password: hashedPassword,
        name: dto.name,
        role,
        seller: role === 'SELLER'
          ? { create: { storeName: dto.storeName!, contactEmail: dto.email, contactPhone: dto.contactPhone, description: dto.description, status: 'PENDING' } }
          : undefined,
      },
    });

    const token = this.signToken(user);
    return { user: { id: user.id, email: user.email, name: user.name, role: user.role, balance: user.balance }, token };
  }

  async login(dto: LoginDto) {
    const user = await this.prisma.user.findUnique({ where: { email: dto.email } });
    if (!user) throw new UnauthorizedException('邮箱或密码错误');

    const passwordValid = await this.verifyPassword(dto.password, user.password);
    if (!passwordValid) throw new UnauthorizedException('邮箱或密码错误');

    // 老用户明文密码自动升级为 bcrypt
    if (!user.password.startsWith('$2')) {
      const hashed = await bcrypt.hash(dto.password, 10);
      await this.prisma.user.update({ where: { id: user.id }, data: { password: hashed } });
    }

    const token = this.signToken(user);
    return { user: { id: user.id, email: user.email, name: user.name, role: user.role, balance: user.balance }, token };
  }

  async getMe(userId: number) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      include: { seller: { select: { id: true, storeName: true, contactEmail: true, contactPhone: true, description: true, status: true, balance: true } } },
    });
    if (!user) throw new UnauthorizedException('用户不存在');
    return { id: user.id, email: user.email, name: user.name, role: user.role, defaultAddress: user.defaultAddress, balance: user.balance, seller: user.seller };
  }

  async getProfile(userId: number) {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user) throw new UnauthorizedException('用户不存在');
    return { id: user.id, email: user.email, name: user.name, role: user.role, defaultAddress: user.defaultAddress, balance: user.balance };
  }

  async changePassword(userId: number, oldPassword: string, newPassword: string) {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user) throw new UnauthorizedException('用户不存在');

    const valid = await this.verifyPassword(oldPassword, user.password);
    if (!valid) throw new UnauthorizedException('原密码错误');

    const hashed = await bcrypt.hash(newPassword, 10);
    await this.prisma.user.update({ where: { id: userId }, data: { password: hashed } });
    return { message: '密码修改成功' };
  }

  private async verifyPassword(plain: string, stored: string): Promise<boolean> {
    // bcrypt hash 以 $2a$、$2b$、$2y$ 开头
    if (stored.startsWith('$2')) {
      return bcrypt.compare(plain, stored);
    }
    // 老数据：明文比较
    return plain === stored;
  }

  async updateProfile(userId: number, dto: { defaultAddress: string }) {
    const user = await this.prisma.user.update({
      where: { id: userId },
      data: { defaultAddress: dto.defaultAddress },
    });
    return { id: user.id, email: user.email, name: user.name, role: user.role, defaultAddress: user.defaultAddress };
  }

  private signToken(user: { id: number; email: string; role: string }) {
    return this.jwt.sign({ sub: user.id, email: user.email, role: user.role });
  }
}
