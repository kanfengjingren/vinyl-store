import api from './client';

/**
 * 上传单张封面图片
 * @param {File} file - 图片文件
 * @returns {Promise<string>} 返回封面路径（如 uploads/covers/xxx.jpg，无前导 /）
 */
export function uploadCover(file) {
  const fd = new FormData();
  fd.append('file', file);
  return api.post('/upload/cover', fd).then((r) => {
    const url = r.data.url;
    // 去掉前导 /，与 coverSrc() 的 `/${url}` 拼接逻辑一致
    return url.startsWith('/') ? url.slice(1) : url;
  });
}
