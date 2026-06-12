<template>
  <div class="not-found-page relative min-h-screen flex items-center justify-center">
    <!-- 背景 -->
    <div class="fixed inset-0 z-0 bg-[#1a1a2e]"></div>

    <div class="relative z-10 text-center px-6">
      <p class="text-[clamp(100px,16vw,180px)] font-bold leading-none text-white/8 mb-2 tracking-[-0.04em] select-none">404</p>
      <h1 class="text-[clamp(22px,3vw,32px)] font-semibold text-white mb-3 tracking-[-0.02em]">页面不存在</h1>
      <p class="text-white/40 text-[15px] mb-10 max-w-[360px] mx-auto leading-relaxed">
        你访问的页面可能已被删除、移动或从未存在过。
      </p>
      <router-link to="/"
        class="inline-flex items-center gap-2 bg-white text-black text-[15px] font-semibold px-8 py-3.5 rounded-full hover:bg-white/90 hover:scale-105 transition-all no-underline shadow-lg">
        返回首页
      </router-link>
    </div>
  </div>
</template>
