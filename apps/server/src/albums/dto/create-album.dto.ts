import { IsNotEmpty, IsString, IsNumber, IsOptional, Min, Max } from 'class-validator';

export class CreateAlbumDto {
    @IsNotEmpty({ message: '专辑名称不能为空' })
    @IsString()
    title!: string;

    @IsOptional()
    @IsString()
    artist?: string;

    @IsOptional()
    @IsNumber()
    artistId?: number;

    @IsOptional()
    @IsString()
    coverUrl!: string;

    @IsNotEmpty()
    @IsNumber()
    @Min(0)
    price!: number;

    @IsOptional()
    @IsNumber()
    @Min(1900)
    @Max(new Date().getFullYear())
    year?: number;

    @IsOptional()
    @IsString()
    label?: string

    @IsOptional()
    @IsString()
    country?: string

    @IsOptional()
    @IsString()
    badge?: string

    @IsOptional()
    @IsString()
    description?: string

    @IsOptional()
    @IsString()
    gradient?: string


    @IsNotEmpty()
    @IsString()
    slug!: string

    @IsNotEmpty()
    @IsNumber()
    stock!: number

    @IsOptional()
    categories?: string[]
}