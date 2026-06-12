<template>
  <div class="min-h-screen flex items-center justify-center bg-apple-bg">
    <div class="text-center px-6">
      <p class="text-[clamp(80px,12vw,140px)] font-bold leading-none text-black/6 mb-2 tracking-[-0.04em] select-none">404</p>
      <h1 class="text-xl font-semibold tracking-[-0.02em] mb-2">页面不存在</h1>
      <p class="text-apple-secondary text-sm mb-8">你访问的页面不存在或已被移除。</p>
      <router-link to="/sellers"
        class="inline-flex items-center gap-2 bg-[rgb(196,147,51)] text-white text-sm font-medium px-6 py-2.5 rounded-full hover:bg-[rgb(176,127,31)] transition-colors no-underline">
        返回审核管理
      </router-link>
    </div>
  </div>
</template>
