import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  // Global prefix: all routes start with /api
  app.setGlobalPrefix('api');

  // Auto-validate all incoming DTOs
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,        // strip unknown properties
      forbidNonWhitelisted: true, // throw if unknown props sent
      transform: true,        // auto-transform types (e.g. string "1" → number 1)
    }),
  );

  // Allow frontend dev server (Vite on :5173) to call API
  app.enableCors({ origin: 'http://localhost:5173', credentials: true });

  await app.listen(3000);
  console.log('Server running on http://localhost:3000/api');
}
bootstrap();
