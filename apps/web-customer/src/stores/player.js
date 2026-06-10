import { reactive } from 'vue'

export const player = reactive({
  track: null,
  src: '',
})

function coverSrc(url) {
  if (!url) return ''
  return url.startsWith('http') ? url : `/${url}`
}

export function usePlayer() {
  function play(track, artistName) {
    if (!track.audioUrl) return
    player.track = { ...track, artist: artistName }
    player.src = coverSrc(track.audioUrl)
  }

  function stop() {
    player.track = null
    player.src = ''
  }

  return { player, play, stop }
}
