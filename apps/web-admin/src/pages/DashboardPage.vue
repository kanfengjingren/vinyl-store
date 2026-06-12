<template>
  <div class="dashboard">
    <h2 class="text-xl font-semibold tracking-[-0.02em] text-black mb-6">数据看板</h2>

    <!-- 概览卡片 -->
    <div class="grid grid-cols-4 gap-5 mb-8 max-lg:grid-cols-2 max-sm:grid-cols-1">
      <div class="bg-white border border-black/5 p-5">
        <p class="text-xs text-black/40 uppercase tracking-[.06em] mb-2">总销售额</p>
        <p class="text-[28px] font-semibold tracking-[-0.02em] text-black">&yen;{{ stats.totalRevenue?.toLocaleString() ?? 0 }}</p>
      </div>
      <div class="bg-white border border-black/5 p-5">
        <p class="text-xs text-black/40 uppercase tracking-[.06em] mb-2">今日订单</p>
        <p class="text-[28px] font-semibold tracking-[-0.02em] text-black">{{ stats.todayOrders ?? 0 }}</p>
      </div>
      <div class="bg-white border border-black/5 p-5">
        <p class="text-xs text-black/40 uppercase tracking-[.06em] mb-2">待审核卖家</p>
        <p class="text-[28px] font-semibold tracking-[-0.02em] text-black">{{ stats.pendingSellers ?? 0 }}</p>
      </div>
      <div class="bg-white border border-black/5 p-5">
        <p class="text-xs text-black/40 uppercase tracking-[.06em] mb-2">专辑总数</p>
        <p class="text-[28px] font-semibold tracking-[-0.02em] text-black">{{ stats.totalAlbums ?? 0 }}</p>
      </div>
    </div>

    <!-- 30 天销售趋势 -->
    <div class="bg-white border border-black/5 p-6 mb-8">
      <h3 class="text-sm font-semibold text-black mb-4">近 30 天销售额</h3>
      <div class="h-[320px]">
        <Bar v-if="trendReady" :data="trendData" :options="trendOptions" />
        <div v-else class="h-full flex items-center justify-center text-black/20">暂无数据</div>
      </div>
    </div>

    <!-- 分类饼图 + 热销 Top 10 -->
    <div class="grid grid-cols-2 gap-8 max-lg:grid-cols-1">
      <div class="bg-white border border-black/5 p-6">
        <h3 class="text-sm font-semibold text-black mb-4">各分类销售额</h3>
        <div class="h-[320px] flex items-center justify-center">
          <Pie v-if="categoryReady" :data="categoryData" :options="pieOptions" />
          <div v-else class="text-black/20">暂无数据</div>
        </div>
      </div>
      <div class="bg-white border border-black/5 p-6">
        <h3 class="text-sm font-semibold text-black mb-4">热销专辑 Top 10</h3>
        <div class="h-[320px]">
          <Bar v-if="topReady" :data="topData" :options="barOptions" />
          <div v-else class="h-full flex items-center justify-center text-black/20">暂无数据</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { fetchDashboardStats, fetchSalesTrend, fetchCategorySales, fetchTopAlbums } from '@vinyl-store/shared';
import { Pie, Bar } from 'vue-chartjs';
import {
  Chart as ChartJS, CategoryScale, LinearScale,
  ArcElement, BarElement, Title, Tooltip, Legend, Filler,
} from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, ArcElement, BarElement, Title, Tooltip, Legend, Filler);

const stats = ref({});
const trend = ref({ days: [], amounts: [] });
const category = ref({ categories: [], amounts: [] });
const topAlbums = ref([]);

onMounted(async () => {
  try {
    const [s, t, c, top] = await Promise.all([
      fetchDashboardStats(),
      fetchSalesTrend(),
      fetchCategorySales(),
      fetchTopAlbums(),
    ]);
    stats.value = s;
    trend.value = t;
    category.value = c;
    topAlbums.value = top;
  } catch (e) {
    console.error('加载数据看板失败:', e);
  }
});

const trendReady = computed(() => trend.value.days?.length > 0);
const categoryReady = computed(() => category.value.categories?.length > 0);
const topReady = computed(() => topAlbums.value.length > 0);

const trendData = computed(() => ({
  labels: trend.value.days,
  datasets: [{
    label: '销售额 (¥)',
    data: trend.value.amounts,
    backgroundColor: 'rgb(196,147,51)',
    borderRadius: 2,
  }],
}));

const categoryData = computed(() => ({
  labels: category.value.categories,
  datasets: [{
    data: category.value.amounts,
    backgroundColor: [
      '#e74c3c', '#f39c12', '#f1c40f', '#2ecc71', '#1abc9c',
      '#3498db', '#9b59b6', '#e67e22', '#95a5a6', '#34495e',
    ],
  }],
}));

const topData = computed(() => ({
  labels: topAlbums.value.map((a) => a.title),
  datasets: [{
    label: '销量',
    data: topAlbums.value.map((a) => a.totalSold),
    backgroundColor: 'rgb(196,147,51)',
    borderRadius: 2,
  }],
}));

const trendOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: {
    x: { grid: { display: false }, ticks: { maxTicksLimit: 8, maxRotation: 45, color: '#999', font: { size: 10 } } },
    y: { grid: { color: 'rgba(0,0,0,0.04)' }, ticks: { color: '#999' } },
  },
};

const pieOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { position: 'right', labels: { padding: 16, color: '#666', font: { size: 12 } } },
  },
};

const barOptions = {
  responsive: true,
  maintainAspectRatio: false,
  indexAxis: 'y',
  plugins: { legend: { display: false } },
  scales: {
    x: { grid: { display: false }, ticks: { color: '#999' } },
    y: { grid: { display: false }, ticks: { color: '#666', font: { size: 11 } } },
  },
};
</script>
