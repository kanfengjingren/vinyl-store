import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

function slugify(artist: string, title: string): string {
  return (artist + ' ' + title)
    .toLowerCase()
    .replace(/[&',().]/g, '')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '');
}

async function main() {
  console.log('Seeding database...');

  // ── Clear existing data (order matters for FK constraints) ──
  await prisma.orderItem.deleteMany();
  await prisma.order.deleteMany();
  await prisma.cartItem.deleteMany();
  await prisma.cart.deleteMany();
  await prisma.albumCategory.deleteMany();
  await prisma.track.deleteMany();
  await prisma.album.deleteMany();
  await prisma.category.deleteMany();
  await prisma.user.deleteMany();

  // ════════════════════════════════════════
  // CATEGORIES
  // ════════════════════════════════════════
  const catUk        = await prisma.category.create({ data: { name: '英国摇滚',  slug: 'uk'        } });
  const catConcept   = await prisma.category.create({ data: { name: '概念专辑',  slug: 'concept'   } });
  const catSymphonic = await prisma.category.create({ data: { name: '交响前卫',  slug: 'symphonic' } });
  const catJapan     = await prisma.category.create({ data: { name: '日本摇滚',  slug: 'japan'     } });
  const catChinese   = await prisma.category.create({ data: { name: '华语独立',  slug: 'chinese'   } });

  console.log('Categories created: 5');

  // ════════════════════════════════════════
  // HELPER: create an album with tracks & categories
  // ════════════════════════════════════════
  async function createAlbum(
    data: {
      artist: string; title: string; year?: number; label?: string; country?: string;
      price: number; badge?: string; description: string;
      coverUrl?: string; gradient?: string;
      stock?: number;
    },
    catSlugs: string[],
    tracks: [string, string?, boolean?][],
  ) {
    const slug = slugify(data.artist, data.title);

    // connect categories by slug
    const categories = catSlugs.map((s) => {
      const map: Record<string, number> = {
        uk: catUk.id, concept: catConcept.id, symphonic: catSymphonic.id,
        japan: catJapan.id, chinese: catChinese.id,
      };
      return { categoryId: map[s] };
    });

    const album = await prisma.album.create({
      data: {
        ...data,
        slug,
        categories: { create: categories },
        tracks: {
          create: tracks.map((t, i) => ({
            title: t[0],
            duration: t[1] || null,
            isSection: t[2] || false,
            position: i + 1,
          })),
        },
      },
    });
    return album;
  }

  // ════════════════════════════════════════
  // 16 ALBUMS
  // ════════════════════════════════════════

  // 1. Pink Floyd – The Dark Side of the Moon
  await createAlbum(
    {
      artist: 'Pink Floyd', title: 'The Dark Side of the Moon',
      year: 1973, label: 'Harvest Records', country: '英国',
      price: 380, badge: '推荐',
      description: '定义了前卫摇滚乃至整个摇滚乐史的不朽杰作。Pink Floyd 用合成器、磁带循环与哲学性的歌词，构建了一场关于疯狂、时间与死亡的沉浸式声景。在 Billboard 200 停留 741 周，至今无人超越。',
      coverUrl: 'cover/moon.jpg',
      gradient: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 40%, #0f3460 70%, #533483 100%)',
    },
    ['uk', 'concept'],
    [
      ['Speak to Me', '1:07'], ['Breathe (In the Air)', '2:49'], ['On the Run', '3:45'],
      ['Time', '6:53'], ['The Great Gig in the Sky', '4:44'], ['Money', '6:23'],
      ['Us and Them', '7:49'], ['Any Colour You Like', '3:26'],
      ['Brain Damage', '3:50'], ['Eclipse', '2:04'],
    ],
  );

  // 2. Yes – Close to the Edge
  await createAlbum(
    {
      artist: 'Yes', title: 'Close to the Edge',
      year: 1972, label: 'Atlantic Records', country: '英国',
      price: 320, badge: '日版',
      description: '前卫摇滚的顶峰之作，仅三首曲目便将交响化摇滚推向极致。Jon Anderson 受赫尔曼·黑塞《悉达多》启发的歌词，与乐队精密而澎湃的器乐编排交织，创造出将近 19 分钟的同名史诗。',
      gradient: 'linear-gradient(150deg,#0d1b0f 0%,#1a3a1f 35%,#2d5a30 65%,#4a7c4f 100%)',
    },
    ['uk', 'concept', 'symphonic'],
    [
      ['Close to the Edge', '18:43'],
      ['  I. The Solid Time of Change', '', true], ['  II. Total Mass Retain', '', true],
      ['  III. I Get Up, I Get Down', '', true], ['  IV. Seasons of Man', '', true],
      ['And You and I', '10:08'], ['Siberian Khatru', '8:55'],
    ],
  );

  // 3. Genesis – Selling England by the Pound
  await createAlbum(
    {
      artist: 'Genesis', title: 'Selling England by the Pound',
      year: 1973, label: 'Charisma Records', country: '英国',
      price: 350, badge: '限量',
      description: 'Peter Gabriel 时期的 Genesis 最具文学性的作品。专辑以英国文化身份的失落为主题，将田园诗意、神话隐喻与社会批判编织一体。',
      gradient: 'linear-gradient(150deg,#1c0f0a 0%,#3d1c0f 35%,#6b2f1a 65%,#a0522d 100%)',
    },
    ['uk', 'concept', 'symphonic'],
    [
      ['Dancing with the Moonlit Knight', '8:04'], ['I Know What I Like (In Your Wardrobe)', '4:10'],
      ['Firth of Fifth', '9:35'], ['More Fool Me', '3:10'],
      ['The Battle of Epping Forest', '11:49'], ['After the Ordeal', '4:15'],
      ['The Cinema Show', '11:06'], ['Aisle of Plenty', '1:32'],
    ],
  );

  // 4. King Crimson – In the Court of the Crimson King
  await createAlbum(
    {
      artist: 'King Crimson', title: 'In the Court of the Crimson King',
      year: 1969, label: 'Island Records', country: '英国',
      price: 420, badge: '经典',
      description: '公认的前卫摇滚开山之作。Robert Fripp 带领的 King Crimson 以这张专辑一举颠覆摇滚乐的边界 — 爵士的即兴、古典的恢弘、金属的暴烈与诗的晦涩在此融为一体。',
      gradient: 'linear-gradient(150deg,#1a0000 0%,#4a0000 35%,#8b0000 65%,#cc1111 100%)',
    },
    ['uk', 'concept'],
    [
      ['21st Century Schizoid Man', '7:24'], ['I Talk to the Wind', '6:05'],
      ['Epitaph', '8:47'], ['Moonchild', '12:13'],
      ['The Court of the Crimson King', '9:25'],
    ],
  );

  // 5. Rush – 2112
  await createAlbum(
    {
      artist: 'Rush', title: '2112',
      year: 1976, label: 'Anthem Records', country: '加拿大',
      price: 290, badge: '加版',
      description: 'Rush 的突破性专辑，也是前卫硬摇滚的里程碑。长达 20 分钟的同名组曲讲述了一个反乌托邦世界中个体对抗集体极权的科幻故事。',
      gradient: 'linear-gradient(150deg,#0a0a15 0%,#1a1040 35%,#2d1a6e 65%,#4b2d9e 100%)',
    },
    ['concept'],
    [
      ['2112', '20:34'],
      ['  I. Overture', '', true], ['  II. The Temples of Syrinx', '', true],
      ['  III. Discovery', '', true], ['  IV. Presentation', '', true],
      ['  V. Oracle: The Dream', '', true], ['  VI. Soliloquy', '', true],
      ['  VII. Grand Finale', '', true],
      ['A Passage to Bangkok', '3:34'], ['The Twilight Zone', '3:17'],
      ['Lessons', '3:51'], ['Tears', '3:31'], ['Something for Nothing', '3:59'],
    ],
  );

  // 6. Jethro Tull – Thick as a Brick
  await createAlbum(
    {
      artist: 'Jethro Tull', title: 'Thick as a Brick',
      year: 1972, label: 'Chrysalis Records', country: '英国',
      price: 310, badge: '英版',
      description: '摇滚史上最具野心的概念专辑之一 — 全专只有一首长达 43 分钟的连续曲目，伪装成一首由 8 岁天才少年 Gerald Bostock 创作的史诗诗歌。',
      gradient: 'linear-gradient(150deg,#1a1208 0%,#3d2a12 35%,#6b4a1f 65%,#a0732d 100%)',
    },
    ['uk', 'concept'],
    [
      ['Thick as a Brick, Part I', '22:40'], ['Thick as a Brick, Part II', '21:06'],
    ],
  );

  // 7. Pink Floyd – Wish You Were Here
  await createAlbum(
    {
      artist: 'Pink Floyd', title: 'Wish You Were Here',
      year: 1975, label: 'Harvest Records', country: '英国',
      price: 360, badge: '限量',
      description: 'Pink Floyd 献给创始成员 Syd Barrett 的一封情书与挽歌。九乐章的《Shine On You Crazy Diamond》如一座声音纪念碑，而标题曲的木吉他前奏则成为摇滚乐中最令人心碎的和弦进行之一。',
      gradient: 'linear-gradient(150deg,#1a0a00 0%,#4d1a00 35%,#993300 65%,#e05500 100%)',
    },
    ['uk', 'concept'],
    [
      ['Shine On You Crazy Diamond (Parts I–V)', '13:34'], ['Welcome to the Machine', '7:31'],
      ['Have a Cigar', '5:08'], ['Wish You Were Here', '5:34'],
      ['Shine On You Crazy Diamond (Parts VI–IX)', '12:31'],
    ],
  );

  // 8. Yes – Fragile
  await createAlbum(
    {
      artist: 'Yes', title: 'Fragile',
      year: 1971, label: 'Atlantic Records', country: '英国',
      price: 280, badge: '日版',
      description: 'Rick Wakeman 加入后的首张 Yes 专辑，也是他们的突破之作。专辑巧妙地将全员合奏的史诗曲目与每位成员的独奏小作品交替排列。',
      gradient: 'linear-gradient(150deg,#0f1a0f 0%,#1a3d1a 35%,#2d6b2d 65%,#4a9e4a 100%)',
    },
    ['uk', 'symphonic'],
    [
      ['Roundabout', '8:30'], ['Cans and Brahms', '1:38'], ['We Have Heaven', '1:40'],
      ['South Side of the Sky', '8:02'], ['Five Per Cent for Nothing', '0:35'],
      ['Long Distance Runaround', '3:30'], ['The Fish (Schindleria Praematurus)', '2:39'],
      ['Mood for a Day', '3:00'], ['Heart of the Sunrise', '11:27'],
    ],
  );

  // 9. Emerson, Lake & Palmer – Brain Salad Surgery
  await createAlbum(
    {
      artist: 'Emerson, Lake & Palmer', title: 'Brain Salad Surgery',
      year: 1973, label: 'Manticore Records', country: '英国',
      price: 330, badge: '经典',
      description: 'ELP 最具野心也最疯狂的作品。封面由 H.R. Giger 设计，内容则从宗教赞美诗跨越到将近 30 分钟的科幻史诗《Karn Evil 9》。',
      gradient: 'linear-gradient(150deg,#1a0a1a 0%,#3d0f3d 35%,#6b1a6b 65%,#9e2d9e 100%)',
    },
    ['uk', 'concept', 'symphonic'],
    [
      ['Jerusalem', '2:44'], ['Toccata', '7:23'], ['Still...You Turn Me On', '2:53'],
      ['Benny the Bouncer', '2:21'], ['Karn Evil 9', '29:54'],
      ['  1st Impression', '', true], ['  2nd Impression', '', true], ['  3rd Impression', '', true],
    ],
  );

  // 10. King Crimson – Red
  await createAlbum(
    {
      artist: 'King Crimson', title: 'Red',
      year: 1974, label: 'Island Records', country: '英国',
      price: 480, badge: '绝版',
      description: '1974 年，King Crimson 在解散前夕录制了这张前卫摇滚最黑暗、最沉重也最完美的专辑。Kurt Cobain 曾称其为对他影响最深的专辑之一。',
      coverUrl: 'cover/red.png',
      gradient: 'linear-gradient(150deg,#141414 0%,#2a2a2a 35%,#4a4a4a 65%,#6e6e6e 100%)',
    },
    ['uk'],
    [
      ['Red', '6:16'], ['Fallen Angel', '6:03'], ['One More Red Nightmare', '7:10'],
      ['Providence', '8:10'], ['Starless', '12:16'],
    ],
  );

  // 11. Mike Oldfield – Tubular Bells
  await createAlbum(
    {
      artist: 'Mike Oldfield', title: 'Tubular Bells',
      year: 1973, label: 'Virgin Records', country: '英国',
      price: 260, badge: '欧版',
      description: '一个 19 岁的少年在 Richard Branson 新开的录音室里演奏了 20 多种乐器，录制出这张改变独立音乐史的专辑。它既是 Virgin Records 的第一张发行，也是电影《驱魔人》的主题音乐来源。',
      gradient: 'linear-gradient(150deg,#0a0f1a 0%,#101f3d 35%,#1a356b 65%,#2d509e 100%)',
    },
    ['uk', 'symphonic'],
    [
      ['Tubular Bells, Part One', '25:30'], ['Tubular Bells, Part Two', '23:20'],
    ],
  );

  // 12. Pink Floyd – Animals
  await createAlbum(
    {
      artist: 'Pink Floyd', title: 'Animals',
      year: 1977, label: 'Harvest Records', country: '英国',
      price: 300, badge: '英版',
      description: 'Pink Floyd 最愤怒也最具政治性的一张专辑。以乔治·奥威尔《动物农场》为灵感，Roger Waters 将社会分为狗（权贵）、猪（政客）和羊（盲从的群众）三类。',
      gradient: 'linear-gradient(150deg,#1a1400 0%,#3d2e00 35%,#6b5000 65%,#9e7a00 100%)',
    },
    ['uk', 'concept'],
    [
      ['Pigs on the Wing 1', '1:25'], ['Dogs', '17:04'],
      ['Pigs (Three Different Ones)', '11:28'], ['Sheep', '10:20'],
      ['Pigs on the Wing 2', '1:25'],
    ],
  );

  // 13. Genesis – The Lamb Lies Down on Broadway
  await createAlbum(
    {
      artist: 'Genesis', title: 'The Lamb Lies Down on Broadway',
      year: 1974, label: 'Charisma Records', country: '英国',
      price: 390, badge: '限量',
      description: 'Peter Gabriel 离队前的最后一张 Genesis 专辑，也是前卫摇滚最宏大、最超现实的概念双专辑。讲述纽约波多黎各裔青年 Rael 的地下冒险之旅。',
      gradient: 'linear-gradient(150deg,#0f0a1a 0%,#2d0f3d 35%,#4f1a6b 65%,#752d9e 100%)',
    },
    ['uk', 'concept', 'symphonic'],
    [
      ['The Lamb Lies Down on Broadway', '4:55'], ['Fly on a Windshield', '2:47'],
      ['Broadway Melody of 1974', '2:11'], ['Cuckoo Cocoon', '2:14'],
      ['In the Cage', '8:15'], ['The Grand Parade of Lifeless Packaging', '2:45'],
      ['Back in N.Y.C.', '5:49'], ['Hairless Heart', '2:13'],
      ['Counting Out Time', '3:45'], ['The Carpet Crawlers', '5:16'],
      ['The Chamber of 32 Doors', '5:40'],
    ],
  );

  // 14. Fishmans – Long Season
  await createAlbum(
    {
      artist: 'Fishmans', title: 'Long Season',
      year: 1996, label: 'Polydor Records', country: '日本',
      price: 420, badge: '日版',
      description: '日本梦幻流行与前卫摇滚交会处的奇迹之作。全专仅一首 35 分钟的单曲，却在重复与变奏中构建出令人恍惚的声景迷宫。',
      coverUrl: 'cover/ls.png',
      gradient: 'linear-gradient(150deg,#1a2a3a 0%,#2a4a5a 35%,#3a6a7a 65%,#4a8a9a 100%)',
    },
    ['japan'],
    [['Long Season', '35:16']],
  );

  // 15. American Football – American Football
  await createAlbum(
    {
      artist: 'American Football', title: 'American Football',
      year: 1999, label: 'Polyvinyl Records', country: '美国',
      price: 290, badge: '美版',
      description: '一张定义了 Midwest Emo 美学的专辑。复杂的数学摇滚节拍与开放吉他调弦交织出秋日黄昏般的声音质感，封面上的白色老宅至今仍是独立摇滚最经典的视觉图腾。',
      coverUrl: 'cover/af.jpg',
      gradient: 'linear-gradient(150deg,#2a3520 0%,#4a5530 35%,#6a7540 65%,#8a9550 100%)',
    },
    [], // no specific category — shows in "all" only
    [
      ['Never Meant', '4:28'], ['The Summer Ends', '4:46'], ['Honestly?', '6:10'],
      ['For Sure', '3:16'], ['You Know I Should Be Leaving Soon', '3:43'],
      ['But the Regrets Are Killing Me', '4:08'],
      ["I'll See You When We're Both Not So Emotional", '3:50'],
      ['Stay Home', '8:10'], ['The One with the Wurlitzer', '2:43'],
    ],
  );

  // 16. Rain – Rain
  await createAlbum(
    {
      artist: 'Rain', title: 'Rain',
      label: '独立发行',
      price: 260, badge: '独立',
      description: '氛围感极强的独立作品，以声音纹理而非传统结构为叙事方式，层层叠加的吉他混响与人声碎片如同一场无法醒来的雨中幻觉。',
      coverUrl: 'cover/rain.jpg',
      gradient: 'linear-gradient(150deg,#1a2a3a 0%,#2a4a5a 35%,#3a6a7a 65%,#4a8a9a 100%)',
    },
    [],
    [],
  );

  // ════════════════════════════════════════
  // Create a demo admin user (email: admin@vinyl.com, password: admin123)
  // ════════════════════════════════════════
  const bcrypt = require('bcryptjs');
  const hashedPassword = await bcrypt.hash('admin123', 10);
  await prisma.user.create({
    data: {
      email: 'admin@vinyl.com',
      password: hashedPassword,
      name: '管理员',
      role: 'ADMIN',
    },
  });

  console.log('Seed complete: 16 albums, 5 categories, 1 admin user');
  console.log('Admin login: admin@vinyl.com / admin123');
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
