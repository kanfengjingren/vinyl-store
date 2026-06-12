<template>
  <div class="stats-page">
    <h2 class="text-xl font-semibold tracking-[-0.02em] text-black mb-6">数据统计</h2>

    <div class="grid grid-cols-2 gap-8 max-lg:grid-cols-1">
      <!-- 近 30 天销售额 -->
      <div class="bg-white border border-black/5 p-6">
        <h3 class="text-sm font-semibold text-black mb-4">近 30 天销售额</h3>
        <div class="h-[340px]">
          <Bar v-if="trendReady" :data="trendData" :options="trendOptions" />
          <div v-else class="h-full flex items-center justify-center text-black/20">暂无数据</div>
        </div>
      </div>

      <!-- 专辑分类分布 -->
      <div class="bg-white border border-black/5 p-6">
        <h3 class="text-sm font-semibold text-black mb-4">专辑分类分布</h3>
        <div class="h-[340px] flex items-center justify-center">
          <Pie v-if="categoryReady" :data="categoryData" :options="pieOptions" />
          <div v-else class="text-black/20">暂无数据</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { fetchSellerSalesTrend, fetchSellerCategoryDistribution } from '@vinyl-store/shared';
import { Bar, Pie } from 'vue-chartjs';
import {
  Chart as ChartJS, CategoryScale, LinearScale,
  BarElement, ArcElement, Title, Tooltip, Legend, Filler,
} from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, BarElement, ArcElement, Title, Tooltip, Legend, Filler);

const trend = ref({ days: [], amounts: [] });
const category = ref({ categories: [], counts: [] });

onMounted(async () => {
  try {
    const [t, c] = await Promise.all([
      fetchSellerSalesTrend(),
      fetchSellerCategoryDistribution(),
    ]);
    trend.value = t;
    category.value = c;
  } catch (e) {
    console.error('加载统计数据失败:', e);
  }
});

const trendReady = computed(() => trend.value.days?.length > 0);
const categoryReady = computed(() => category.value.categories?.length > 0);

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
    data: category.value.counts,
    backgroundColor: [
      '#e74c3c', '#f39c12', '#f1c40f', '#2ecc71', '#1abc9c',
      '#3498db', '#9b59b6', '#e67e22', '#95a5a6', '#34495e',
    ],
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
</script>
