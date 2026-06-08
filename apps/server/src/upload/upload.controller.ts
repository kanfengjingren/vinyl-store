import { Controller, Post, UseGuards, UseInterceptors, UploadedFile } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { diskStorage } from 'multer';
import { extname } from 'path';
import { randomUUID } from 'crypto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { RolesGuard } from '../auth/roles.guard';
import { Roles } from '../auth/roles.decorator';
import { Role } from '@prisma/client';

// @UseGuards(JwtAuthGuard, RolesGuard)
// @Roles(Role.ADMIN)
@Controller('upload')   //   /api/upload/cover
export class UploadController {
  @Post('cover')
  
  //拦截器 FileInterceptor用来处理单文件上传
  @UseInterceptors(
    FileInterceptor('file', {

      //存储方式diskStorage存到硬盘
      storage: diskStorage({
        destination: './uploads/covers',  //存到哪个位置

        filename: (_req, file, cb) => {
          const name = file.originalname;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              
          cb(null, name);
        },

      }),

      //过滤器，白名单
      fileFilter: (_req, file, cb) => {
        const allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
        cb(null, allowed.includes(file.mimetype) ? true : false);
      },
      limits: { fileSize: 5 * 1024 * 1024 }, // 5MB
    }),
  )


  //@UploadedFile() 拦截器拿到就给这个
  uploadCover(@UploadedFile() file: Express.Multer.File) {
    if (!file) {
      return { message: '请上传图片文件 (jpg/png/webp/gif, ≤5MB)' };
    }
    return { url: `/uploads/covers/${file.filename}` };
  }
}
