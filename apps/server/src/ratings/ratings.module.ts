import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { RatingsController } from './ratings.controller';
import { RatingsService } from './ratings.service';

@Module({
  imports: [
    JwtModule.register({
      secret: process.env.JWT_SECRET ?? 'dev-secret',
      signOptions: { expiresIn: '7d' },
    }),
  ],
  controllers: [RatingsController],
  providers: [RatingsService],
})
export class RatingsModule {}
