import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

function slugify(text: string): string {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9一-鿿]+/g, '-')
    .replace(/^-|-$/g, '')
    .replace(/-+/g, '-')
    || 'artist';
}

async function main() {
  console.log('=== Artist 数据迁移开始 ===\n');

  // 1. 提取所有 distinct artist 值
  const rows = await prisma.album.findMany({
    select: { artist: true },
    distinct: ['artist'],
  });
  const artistNames = rows.map((r) => r.artist).filter(Boolean);
  console.log(`找到 ${artistNames.length} 个 distinct artist:\n  ${artistNames.join('\n  ')}\n`);

  // 2. 为每个 artist 创建 Artist 记录
  const artistMap = new Map<string, number>(); // name -> id

  for (const name of artistNames) {
    let slug = slugify(name);

    // 处理 slug 冲突（同名不同乐队）
    let counter = 2;
    while (await prisma.artist.findUnique({ where: { slug } })) {
      slug = `${slugify(name)}-${counter}`;
      counter++;
    }

    const artist = await prisma.artist.create({
      data: { name, slug },
    });
    artistMap.set(name, artist.id);
    console.log(`  ✅ 创建 Artist: "${name}" (id=${artist.id}, slug=${slug})`);
  }

  // 3. 更新 album.artistId
  console.log('\n更新 album.artistId...');
  for (const [name, artistId] of artistMap) {
    const result = await prisma.album.updateMany({
      where: { artist: name },
      data: { artistId },
    });
    console.log(`  📀 "${name}" → artistId=${artistId} (${result.count} 张专辑)`);
  }

  console.log('\n=== 迁移完成 ===');
  console.log(`共创建 ${artistMap.size} 个 Artist，关联了 ${rows.length} 个不同的 artist 值`);
}

main()
  .catch((e) => {
    console.error('❌ 迁移失败：', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
