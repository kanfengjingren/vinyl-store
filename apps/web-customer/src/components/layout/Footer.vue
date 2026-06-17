<template>
  <footer class="bg-white border-t border-apple-border pt-10 pb-6 px-6 mt-20">
    <div class="max-w-[1200px] mx-auto flex justify-between flex-wrap gap-10 max-sm:flex-col max-sm:gap-6">
      <div v-for="col in columns" :key="col.title">
        <h4 class="text-[13px] font-semibold tracking-[.02em] mb-3 text-apple-text">{{ col.title }}</h4>
        <ul class="list-none">
          <li v-for="link in col.links" :key="link.label" class="mb-2">
            <router-link v-if="link.to" :to="link.to" class="text-[13px] text-apple-secondary no-underline hover:text-apple-text transition-colors">{{ link.label }}</router-link>
            <a v-else href="#" class="text-[13px] text-apple-secondary no-underline hover:text-apple-text transition-colors">{{ link.label }}</a>
          </li>
        </ul>
      </div>
    </div>
    <div class="max-w-[1200px] mx-auto mt-8 pt-5 border-t border-apple-border flex justify-between flex-wrap gap-3 text-xs text-apple-tertiary">
      <span>&copy; 2024 黑胶档案馆。保留所有权利。</span>
      <span><a href="#" class="text-apple-tertiary no-underline">隐私政策</a> &middot; <a href="#" class="text-apple-tertiary no-underline">使用条款</a></span>
    </div>
  </footer>
</template>

<script setup>
const columns = [
  { title: '浏览', links: [
    { label: '全部唱片', to: '/catalog' },
    { label: '新品上架', to: '/new-arrivals' },
    { label: '即将到货' },
    { label: '稀有珍藏' },
  ]},
  { title: '服务', links: [
    { label: '品质保证' },
    { label: '退换政策' },
    { label: '全球配送' },
    { label: '联系我们' },
  ]},
  { title: '关于', links: [
    { label: '我们的故事', to: '/about' },
    { label: '策展哲学', to: '/about' },
    { label: '门店地址', to: '/about' },
    { label: '加入我们', to: '/about' },
  ]},
];
</script>
