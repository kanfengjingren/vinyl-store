import sharp from 'sharp';

/** 9 种颜色分类 */
export type ColorLabel = 'red' | 'orange' | 'yellow' | 'green' | 'cyan' | 'blue' | 'purple' | 'black' | 'white';

export const ALL_COLORS: { label: ColorLabel; name: string; hex: string }[] = [
  { label: 'red',     name: '红色', hex: '#e74c3c' },
  { label: 'orange',  name: '橙色', hex: '#f39c12' },
  { label: 'yellow',  name: '黄色', hex: '#f1c40f' },
  { label: 'green',   name: '绿色', hex: '#2ecc71' },
  { label: 'cyan',    name: '青色', hex: '#1abc9c' },
  { label: 'blue',    name: '蓝色', hex: '#3498db' },
  { label: 'purple',  name: '紫色', hex: '#9b59b6' },
  { label: 'black',   name: '黑色', hex: '#2c3e50' },
  { label: 'white',   name: '白色', hex: '#ecf0f1' },
];

/** RGB → HSL → 9 色分类 */
export function classifyColor(r: number, g: number, b: number): ColorLabel {
  const rNorm = r / 255, gNorm = g / 255, bNorm = b / 255;
  const max = Math.max(rNorm, gNorm, bNorm);
  const min = Math.min(rNorm, gNorm, bNorm);
  const l = (max + min) / 2;

  // 亮度极低 → 黑色
  if (l < 0.12) return 'black';
  // 亮度极高 + 低饱和 → 白色
  if (l > 0.88) return 'white';

  // 计算饱和度
  const s = max === min ? 0 : l > 0.5 ? (max - min) / (2 - max - min) : (max - min) / (max + min);

  // 低饱和 → 根据亮度归到黑或白
  if (s < 0.12) return l < 0.4 ? 'black' : 'white';

  // 计算色相 (0-360)
  let h = 0;
  if (max === rNorm) {
    h = ((gNorm - bNorm) / (max - min)) * 60;
  } else if (max === gNorm) {
    h = ((bNorm - rNorm) / (max - min)) + 120;
  } else {
    h = ((rNorm - gNorm) / (max - min)) + 240;
  }
  if (h < 0) h += 360;

  // 色相 → 颜色
  if (h < 22)  return 'red';
  if (h < 45)  return 'orange';
  if (h < 68)  return 'yellow';
  if (h < 165) return 'green';
  if (h < 200) return 'cyan';
  if (h < 260) return 'blue';
  if (h < 320) return 'purple';
  return 'red'; // 320-360 也是红色
}

/** 从本地图片文件提取主色调：逐像素分类后投票取众数 */
export async function extractColorFromImage(filePath: string): Promise<ColorLabel | null> {
  try {
    // 缩小到 100×100=10000 像素，足够统计
    const { data, info } = await sharp(filePath)
      .resize(100, 100, { fit: 'fill' })
      .removeAlpha()
      .raw()
      .toBuffer({ resolveWithObject: true });

    // 逐像素分类并计数
    const votes: Record<string, number> = {};
    const pixelCount = info.width * info.height;
    for (let i = 0; i < pixelCount; i++) {
      const r = data[i * 3];
      const g = data[i * 3 + 1];
      const b = data[i * 3 + 2];
      const label = classifyColor(r, g, b);
      votes[label] = (votes[label] || 0) + 1;
    }

    // 取出现次数最多的颜色
    const winner = Object.entries(votes).sort((a, b) => b[1] - a[1])[0];
    return winner[0] as ColorLabel;
  } catch {
    return null;
  }
}

/** 从 CSS gradient 中解析 hex 颜色，取最亮色作为主色 */
export function parseGradientColor(gradient: string): ColorLabel | null {
  if (!gradient) return null;
  const hexes = gradient.match(/#[0-9a-fA-F]{6}/g);
  if (!hexes || hexes.length === 0) return null;

  // 取最后一个颜色（通常是 100% 位置，最亮）
  const lastHex = hexes[hexes.length - 1];
  const r = parseInt(lastHex.slice(1, 3), 16);
  const g = parseInt(lastHex.slice(3, 5), 16);
  const b = parseInt(lastHex.slice(5, 7), 16);

  return classifyColor(r, g, b);
}
