import { IsInt, Min, Max } from 'class-validator';
import { Type } from 'class-transformer';

export class AddToCartDto {
  @Type(() => Number)
  @IsInt()
  albumId: number;

  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(99)
  quantity: number = 1;
}
