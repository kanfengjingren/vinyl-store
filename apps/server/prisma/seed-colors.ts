import { PrismaClient } from '@prisma/client';
import { parseGradientColor } from '../src/common/color';

const prisma = new PrismaClient();

async function main() {
  console.log('=== 回填 album.color ===\n');

  const albums = await prisma.album.findMany({
    where: { color: null },
    select: { id: true, title: true, gradient: true, coverUrl: true },
  });

  console.log(`找到 ${albums.length} 张无 color 的专辑\n`);

  let updated = 0;
  for (const album of albums) {
    let color = parseGradientColor(album.gradient || '');
    // 如果 gradient 解析不到且没有封面，跳过
    if (!color) {
      console.log(`  ⏭️  #${album.id} "${album.title}" — 无法识别颜色`);
      continue;
    }

    await prisma.album.update({
      where: { id: album.id },
      data: { color },
    });
    console.log(`  ✅ #${album.id} "${album.title}" → ${color}  (gradient: ${album.gradient?.slice(0, 40)}...)`);
    updated++;
  }

  console.log(`\n=== 完成：${updated} 张专辑已填色 ===`);
}

main()
  .catch((e) => {
    console.error('❌ 失败：', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
