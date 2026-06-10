import api from './client';

/**
 * 上传单张封面图片
 * @param {File} file - 图片文件
 * @returns {Promise<string>} 返回封面路径
 */
export function uploadCover(file) {
  const fd = new FormData();
  fd.append('file', file);
  return api.post('/upload/cover', fd).then((r) => {
    const url = r.data.url;
    return url.startsWith('/') ? url.slice(1) : url;
  });
}

/**
 * 上传音频文件
 * @param {File} file - 音频文件
 * @returns {Promise<string>} 返回音频路径
 */
export function uploadAudio(file) {
  const fd = new FormData();
  fd.append('file', file);
  return api.post('/upload/audio', fd).then((r) => {
    const url = r.data.url;
    return url.startsWith('/') ? url.slice(1) : url;
  });
}
