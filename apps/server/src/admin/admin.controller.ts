import { Controller, Get, Patch, Param, ParseIntPipe, Query, UseGuards } from '@nestjs/common';
import { AdminService } from './admin.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { RolesGuard } from '../auth/roles.guard';
import { Roles } from '../auth/roles.decorator';
import { SellerStatus } from '@prisma/client';

@UseGuards(JwtAuthGuard, RolesGuard)
@Roles('ADMIN')
@Controller('admin')
export class AdminController {
  constructor(private readonly adminService: AdminService) {}

  @Get('sellers')
  listSellers(@Query('status') status?: SellerStatus) {
    return this.adminService.listSellers(status);
  }

  @Patch('sellers/:id/approve')
  approveSeller(@Param('id', ParseIntPipe) id: number) {
    return this.adminService.approveSeller(id);
  }

  @Patch('sellers/:id/reject')
  rejectSeller(@Param('id', ParseIntPipe) id: number) {
    return this.adminService.rejectSeller(id);
  }
}
