import { ref } from 'vue'
import { fetchPurchases } from '@vinyl-store/shared'
import { useAuthStore } from '../stores/auth'

const purchasedIds = ref(new Set())
const loaded = ref(false)

export function usePurchased() {
  const auth = useAuthStore()

  async function load() {
    if (!auth.isLoggedIn) return
    if (loaded.value) return
    try {
      const data = await fetchPurchases()
      purchasedIds.value = new Set(data.map((a) => a.id))
      loaded.value = true
    } catch {}
  }

  function isPurchased(albumId) {
    return purchasedIds.value.has(albumId)
  }

  return { purchasedIds, loaded, load, isPurchased }
}
