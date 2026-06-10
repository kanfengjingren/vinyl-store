import { Module } from '@nestjs/common';
import { ServeStaticModule } from '@nestjs/serve-static';
import { join } from 'path';
import { PrismaModule } from './prisma/prisma.module';
import { CategoriesModule } from './categories/categories.module';
import { AlbumsModule } from './albums/albums.module';
import { TracksModule } from './tracks/tracks.module';
import { AuthModule } from './auth/auth.module';
import { UsersModule } from './users/users.module';
import { CartModule } from './cart/cart.module';
import { OrdersModule } from './orders/orders.module';
import { UploadModule } from './upload/upload.module';
import { AdminModule } from './admin/admin.module';
import { ArtistsModule } from './artists/artists.module';

@Module({
  imports: [
    ServeStaticModule.forRoot({
      rootPath: join(__dirname, '..', '..', 'uploads'),
      serveRoot: '/uploads',
      serveStaticOptions: {
        index: false,   // 禁用目录索引
      },
    }),
    PrismaModule,
    CategoriesModule,
    AlbumsModule,
    TracksModule,
    AuthModule,
    UsersModule,
    CartModule,
    OrdersModule,
    UploadModule,
    AdminModule,
    ArtistsModule,
  ],
})
export class AppModule { }
