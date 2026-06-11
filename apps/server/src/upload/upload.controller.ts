import { Controller, Post, UseGuards, UseInterceptors, UploadedFile } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { diskStorage } from 'multer';
import { randomUUID } from 'crypto';
import { join } from 'path';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { RolesGuard } from '../auth/roles.guard';
import { Roles } from '../auth/roles.decorator';

// 上传根目录：优先用 env，否则 fallback 到 apps/server/uploads/
const uploadsPath = process.env.UPLOADS_BASE_PATH || join(__dirname, '..', '..', 'uploads');

@Controller('upload')
export class UploadController {

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('SELLER', 'ADMIN')
  @Post('cover')
  @UseInterceptors(
    FileInterceptor('file', {
      storage: diskStorage({
        destination: join(uploadsPath, 'covers'),
        filename: (_req, file, cb) => {
          const ext = file.originalname.split('.').pop();
          cb(null, `${randomUUID()}.${ext}`);
        },
      }),
      fileFilter: (_req, file, cb) => {
        const allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
        cb(null, allowed.includes(file.mimetype));
      },
      limits: { fileSize: 5 * 1024 * 1024 },
    }),
  )
  uploadCover(@UploadedFile() file: Express.Multer.File) {
    if (!file) {
      return { message: '请上传图片文件 (jpg/png/webp/gif, ≤5MB)' };
    }
    return { url: `/uploads/covers/${file.filename}` };
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('SELLER', 'ADMIN')
  @Post('audio')
  @UseInterceptors(
    FileInterceptor('file', {
      storage: diskStorage({
        destination: join(uploadsPath, 'audio'),
        filename: (_req, file, cb) => {
          const ext = file.originalname.split('.').pop();
          cb(null, `${randomUUID()}.${ext}`);
        },
      }),
      fileFilter: (_req, file, cb) => {
        const allowed = ['audio/mpeg', 'audio/mp3', 'audio/flac', 'audio/wav', 'audio/ogg', 'audio/x-wav', 'audio/wave'];
        cb(null, allowed.includes(file.mimetype));
      },
      limits: { fileSize: 500 * 1024 * 1024 }, // 500MB
    }),
  )
  uploadAudio(@UploadedFile() file: Express.Multer.File) {
    if (!file) {
      return { message: '请上传音频文件 (mp3/flac/wav/ogg, ≤30MB)' };
    }
    return { url: `/uploads/audio/${file.filename}` };
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('SELLER', 'ADMIN')
  @Post('artist-photo')
  @UseInterceptors(
    FileInterceptor('file', {
      storage: diskStorage({
        destination: join(uploadsPath, 'artists'),
        filename: (_req, file, cb) => {
          const ext = file.originalname.split('.').pop();
          cb(null, `${randomUUID()}.${ext}`);
        },
      }),
      fileFilter: (_req, file, cb) => {
        const allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
        cb(null, allowed.includes(file.mimetype));
      },
      limits: { fileSize: 5 * 1024 * 1024 },
    }),
  )
  uploadArtistPhoto(@UploadedFile() file: Express.Multer.File) {
    if (!file) {
      return { message: '请上传图片文件 (jpg/png/webp/gif, ≤5MB)' };
    }
    return { url: `/uploads/artists/${file.filename}` };
  }
}
