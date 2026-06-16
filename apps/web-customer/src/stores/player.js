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
      playlist: state.playlist,
      currentIndex: state.currentIndex,
    }
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(toSave))
  } catch {
    // sessionStorage 不可用时静默失败
  }
}

// 从 playlist 中过滤出可播放的曲目（有 audioUrl 且非 section）
function getPlayableTracks(tracks) {
  if (!tracks || !tracks.length) return []
  return tracks.filter(t => t.audioUrl && !t.isSection)
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
  playlist: saved?.playlist || [],
  currentIndex: saved?.currentIndex ?? -1,
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
    playlist: player.playlist,
    currentIndex: player.currentIndex,
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
  function play(track, artistName, albumInfo, trackList) {
    if (!track.audioUrl) return
    player.track = { ...track, artist: artistName }
    player.src = coverSrc(track.audioUrl)
    if (albumInfo) {
      player.album = {
        title: albumInfo.title,
        coverUrl: albumInfo.coverUrl,
        gradient: albumInfo.gradient,
        color: albumInfo.color || null,
        description: albumInfo.description,
        categories: albumInfo.categories || [],
        artist: albumInfo.artist,
        artistInfo: albumInfo.artistInfo || null,
      }
    }
    // 设置播放列表（同一个专辑的可播放曲目）
    if (trackList && trackList.length) {
      const playable = getPlayableTracks(trackList)
      player.playlist = playable
      player.currentIndex = playable.findIndex(t => t.id === track.id)
    } else {
      // 没有传列表时清空（从历史记录等单独播放）
      player.playlist = []
      player.currentIndex = -1
    }
  }

  function nextTrack() {
    if (!player.playlist.length || player.currentIndex < 0) return
    const next = (player.currentIndex + 1) % player.playlist.length
    const track = player.playlist[next]
    if (!track) return
    player.currentIndex = next
    player.track = { ...track, artist: player.album?.artist || player.track?.artist }
    player.src = coverSrc(track.audioUrl)
    player.currentSeconds = 0
    player.duration = 0
  }

  function prevTrack() {
    if (!player.playlist.length || player.currentIndex < 0) return
    const prev = (player.currentIndex - 1 + player.playlist.length) % player.playlist.length
    const track = player.playlist[prev]
    if (!track) return
    player.currentIndex = prev
    player.track = { ...track, artist: player.album?.artist || player.track?.artist }
    player.src = coverSrc(track.audioUrl)
    player.currentSeconds = 0
    player.duration = 0
  }

  function stop() {
    player.track = null
    player.src = ''
    player.album = null
    player.showFullPlayer = false
    player.playing = false
    player.playlist = []
    player.currentIndex = -1
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

  return { player, play, stop, openFullPlayer, closeFullPlayer, nextTrack, prevTrack }
}
