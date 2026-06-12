import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { IoAdapter } from '@nestjs/platform-socket.io';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  // Socket.IO adapter — 必须配置，否则 WebSocket 网关不工作
  app.useWebSocketAdapter(new IoAdapter(app));

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
  app.enableCors({ origin: true, credentials: true });

  await app.listen(3000);
  console.log('Server running on http://localhost:3000/api');
  console.log('[Socket.IO] IoAdapter 已启用，WebSocket 网关可用');
  console.log('[Socket.IO] 聊天命名空间: /chat');
}
bootstrap();
