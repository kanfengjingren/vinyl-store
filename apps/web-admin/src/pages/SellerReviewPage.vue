<template>
  <div>
    <h2 class="text-xl font-semibold tracking-[-0.02em] mb-6">卖家审核</h2>

    <!-- Tabs -->
    <div class="flex gap-1 mb-6 border-b border-black/10">
      <button v-for="tab in tabs" :key="tab.key" @click="activeTab = tab.key" :class="[
        'px-5 py-2.5 text-sm font-medium transition-colors relative -mb-[1px]',
        activeTab === tab.key
          ? 'text-black border-b-2 border-[rgb(196,147,51)]'
          : 'text-black/50 hover:text-black/80'
      ]">
        {{ tab.label }}
        <span v-if="tab.key === 'PENDING' && pendingCount" class="ml-1.5 px-1.5 py-0.5 rounded-full bg-red-500 text-white text-[11px]">{{ pendingCount }}</span>
      </button>
    </div>

    <div v-if="loading" class="text-center py-16 text-black/40">加载中...</div>

    <div v-else-if="!filteredSellers.length" class="text-center py-16 text-black/40">
      <p class="text-[15px]">暂无{{ activeTab === 'PENDING' ? '待审核' : activeTab === 'APPROVED' ? '已通过' : '已拒绝' }}的卖家</p>
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="seller in filteredSellers"
        :key="seller.id"
        class="border border-black/10 rounded-xl bg-white px-5 py-4 flex items-center justify-between hover:shadow-sm transition-shadow"
      >
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-3 mb-1">
            <p class="text-[15px] font-semibold text-black">{{ seller.storeName }}</p>
            <span :class="[
              'text-[11px] font-medium px-2 py-0.5 rounded-full',
              seller.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' :
                seller.status === 'APPROVED' ? 'bg-green-100 text-green-700' :
                  'bg-red-100 text-red-700'
            ]">
              {{ seller.status === 'PENDING' ? '待审核' : seller.status === 'APPROVED' ? '已通过' : '已拒绝' }}
            </span>
          </div>
          <p class="text-sm text-black/50">
            {{ seller.user?.name || seller.user?.email }}
            <span class="text-black/25 mx-1.5">|</span>
            {{ seller.contactEmail || seller.user?.email }}
            <span v-if="seller.contactPhone" class="text-black/25 mx-1.5">|</span>
            <span v-if="seller.contactPhone">{{ seller.contactPhone }}</span>
            <span class="text-black/25 mx-1.5">|</span>
            <span>{{ seller._count?.albums ?? 0 }} 张专辑</span>
          </p>
          <p v-if="seller.description" class="text-xs text-black/30 mt-1 line-clamp-2">{{ seller.description }}</p>
          <p class="text-xs text-black/25 mt-1">入驻时间: {{ formatDate(seller.createdAt) }}</p>
        </div>

        <div v-if="seller.status === 'PENDING'" class="flex items-center gap-2 shrink-0 ml-4">
          <button
            class="px-4 py-2 rounded-lg border border-green-300 bg-green-50 text-green-600 text-sm font-medium hover:bg-green-100 transition-colors"
            @click="onApprove(seller)"
          >
            通过
          </button>
          <button
            class="px-4 py-2 rounded-lg border border-red-200 bg-red-50 text-red-500 text-sm font-medium hover:bg-red-100 transition-colors"
            @click="onReject(seller)"
          >
            拒绝
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { fetchSellers, approveSeller, rejectSeller, useModalStore } from '@vinyl-store/shared';

const modal = useModalStore();

const sellers = ref([]);
const loading = ref(false);
const activeTab = ref('PENDING');

const tabs = [
  { key: 'PENDING', label: '待审核' },
  { key: 'APPROVED', label: '已通过' },
  { key: 'REJECTED', label: '已拒绝' },
];

const pendingCount = computed(() => sellers.value.filter(s => s.status === 'PENDING').length);

const filteredSellers = computed(() =>
  sellers.value.filter(s => s.status === activeTab.value)
);

async function load() {
  loading.value = true;
  try {
    sellers.value = await fetchSellers();
  } finally {
    loading.value = false;
  }
}

async function onApprove(seller) {
  const ok = await modal.open({
    message: `确认通过「${seller.storeName}」的入驻申请？`,
    confirmText: '确认通过',
    cancelText: '取消',
  });
  if (!ok) return;
  await approveSeller(seller.id);
  await load();
}

async function onReject(seller) {
  const ok = await modal.open({
    message: `确认拒绝「${seller.storeName}」的入驻申请？`,
    confirmText: '确认拒绝',
    cancelText: '取消',
  });
  if (!ok) return;
  await rejectSeller(seller.id);
  await load();
}

function formatDate(d) {
  return new Date(d).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  });
}

onMounted(() => load());
</script>
