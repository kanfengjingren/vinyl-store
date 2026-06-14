import { reactive, watch } from 'vue'

const STORAGE_KEY = 'vinyl_player_state'

// 从 sessionStorage 恢复状态
function loadState() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const saved = JSON.parse(raw)
    // 不恢复播放状态和 UI 状态
    saved.playing = false
    saved.showFullPlayer = false
    saved.audioEl = null
    // 音频 URL 过期检查：如果是相对路径，加上当前 origin
    if (saved.src && !saved.src.startsWith('http') && !saved.src.startsWith('/')) {
      saved.src = '/' + saved.src
    }
    return saved
  } catch {
    return null
  }
}

// 保存状态到 sessionStorage
function saveState(state) {
  try {
    const toSave = {
      track: state.track,
      src: state.src,
      album: state.album,
      currentSeconds: state.currentSeconds,
      duration: state.duration,
    }
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(toSave))
  } catch {
    // sessionStorage 不可用时静默失败
  }
}

// 初始化：尝试从 sessionStorage 恢复
const saved = loadState()

export const player = reactive({
  track: saved?.track || null,
  src: saved?.src || '',
  album: saved?.album || null,
  showFullPlayer: false,
  playing: false,
  currentSeconds: saved?.currentSeconds || 0,
  duration: saved?.duration || 0,
  audioEl: null,
})

// 监听关键字段变化，自动保存（进度每秒最多存一次）
let saveTimer = null
watch(
  () => ({
    track: player.track,
    src: player.src,
    album: player.album,
    currentSeconds: player.currentSeconds,
    duration: player.duration,
  }),
  (state) => {
    if (state.track || state.src) {
      // track/album/duration 变化立即存，currentSeconds 变化防抖
      clearTimeout(saveTimer)
      saveTimer = setTimeout(() => saveState(state), 800)
    }
  },
  { deep: true }
)

function coverSrc(url) {
  if (!url) return ''
  return url.startsWith('http') ? url : `/${url}`
}

export function usePlayer() {
  function play(track, artistName, albumInfo) {
    if (!track.audioUrl) return
    player.track = { ...track, artist: artistName }
    player.src = coverSrc(track.audioUrl)
    if (albumInfo) {
      player.album = {
        title: albumInfo.title,
        coverUrl: albumInfo.coverUrl,
        gradient: albumInfo.gradient,
        description: albumInfo.description,
        categories: albumInfo.categories || [],
        artist: albumInfo.artist,
        artistInfo: albumInfo.artistInfo || null,
      }
    }
  }

  function stop() {
    player.track = null
    player.src = ''
    player.album = null
    player.showFullPlayer = false
    player.playing = false
    // 清除持久化
    sessionStorage.removeItem(STORAGE_KEY)
  }

  function openFullPlayer() {
    if (player.track) {
      player.showFullPlayer = true
    }
  }

  function closeFullPlayer() {
    player.showFullPlayer = false
  }

  return { player, play, stop, openFullPlayer, closeFullPlayer }
}
