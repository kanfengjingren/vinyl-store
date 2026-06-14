import {
  Body, Controller, Delete, Get, Param, ParseIntPipe,
  Post, Query, Req, UseGuards,
} from '@nestjs/common';
import { CommentsService } from './comments.service';
import { CreateCommentDto } from './dto/create-comment.dto';
import { QueryCommentsDto } from './dto/query-comments.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@Controller()
export class CommentsController {
  constructor(private readonly commentsService: CommentsService) {}

  // GET /api/albums/:albumId/comments — 分页获取根评论
  @Get('albums/:albumId/comments')
  findByAlbum(
    @Param('albumId', ParseIntPipe) albumId: number,
    @Query() query: QueryCommentsDto,
  ) {
    return this.commentsService.findByAlbum(albumId, query);
  }

  // GET /api/comments/:id/replies — 获取全部子回复
  @Get('comments/:id/replies')
  findReplies(@Param('id', ParseIntPipe) id: number) {
    return this.commentsService.findReplies(id);
  }

  // POST /api/albums/:albumId/comments — 发表评论
  @UseGuards(JwtAuthGuard)
  @Post('albums/:albumId/comments')
  create(
    @Req() req: any,
    @Param('albumId', ParseIntPipe) albumId: number,
    @Body() body: CreateCommentDto,
  ) {
    return this.commentsService.create(req.user.userId, albumId, body);
  }

  // DELETE /api/comments/:id — 删除评论
  @UseGuards(JwtAuthGuard)
  @Delete('comments/:id')
  delete(@Req() req: any, @Param('id', ParseIntPipe) id: number) {
    return this.commentsService.delete(id, req.user.userId);
  }
}
