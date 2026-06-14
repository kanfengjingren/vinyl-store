import { Module } from '@nestjs/common';
import { PlayHistoryController } from './play-history.controller';
import { PlayHistoryService } from './play-history.service';
import { PrismaModule } from '../prisma/prisma.module';

@Module({
  imports: [PrismaModule],
  controllers: [PlayHistoryController],
  providers: [PlayHistoryService],
})
export class PlayHistoryModule {}
