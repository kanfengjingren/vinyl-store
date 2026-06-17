<template>
  <div class="about-page">
    <div
      class="image-row"
      @mousedown="onDragStart"
      @mousemove="onDragMove"
      @mouseup="onDragEnd"
      @mouseleave="onDragEnd"
      @touchstart="onDragStart"
      @touchmove="onDragMove"
      @touchend="onDragEnd"
      @click.capture="onRowClick"
    >
      <img
        v-for="(src, i) in images"
        :key="i"
        :src="`/about-images/${src}`"
        :alt="src"
        class="square-img shadow-lg"
      />

      <!-- 分工 + 下载卡片：2 倍宽 -->
      <div class="info-card flex items-center justify-center bg-white select-none shadow-lg">
        <div class="text-center px-12 py-10">
          <h2 class="text-[28px] font-bold text-[#1d1d1f] mb-8 tracking-wide">团队成员</h2>

          <ul class="list-none p-0 m-0 mb-10 text-left space-y-4">
            <li class="text-[18px] text-[#3a3a3c] leading-relaxed">
              <span class="inline-block min-w-[52px] text-[13px] font-semibold text-[#6e6e73] bg-[#f2f2f6] rounded px-2 py-0.5 mr-3 text-center">后端</span>
              <strong>姚铭楷</strong> · NestJS + Prisma + MySQL
            </li>
            <li class="text-[18px] text-[#3a3a3c] leading-relaxed">
              <span class="inline-block min-w-[52px] text-[13px] font-semibold text-[#6e6e73] bg-[#f2f2f6] rounded px-2 py-0.5 mr-3 text-center">前端</span>
              <strong>陈天翔</strong> · Vue3 + Pinia + Router
            </li>
            <li class="text-[18px] text-[#3a3a3c] leading-relaxed">
              <span class="inline-block min-w-[52px] text-[13px] font-semibold text-[#6e6e73] bg-[#f2f2f6] rounded px-2 py-0.5 mr-3 text-center">接口</span>
              <strong>李俊睿</strong> · Axios + 抽离代码
            </li>
            <li class="text-[18px] text-[#3a3a3c] leading-relaxed">
              <span class="inline-block min-w-[52px] text-[13px] font-semibold text-[#6e6e73] bg-[#f2f2f6] rounded px-2 py-0.5 mr-3 text-center">运维</span>
              <strong>樊贤俊</strong> · 测试 + PM2 + Nginx
            </li>
          </ul>

          <div class="border-t border-[#e5e5ea] pt-6">
            <h3 class="text-[20px] font-semibold text-[#1d1d1f] mb-3">Android 客户端</h3>
            <a
              href="/downloads/app-prod-debug.apk"
              class="inline-flex items-center gap-2 text-[15px] font-medium text-white bg-[#0071e3] hover:bg-[#0077ed] rounded-full px-8 py-3 transition-colors no-underline"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
              下载 APK
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const images = [
  '盗版weezer.jpg',
  'weezer.jpg',
  'weezer专业版.jpg',
  'weezer专业版2.png',

]

const dragState = ref({ el: null, startX: 0, scrollLeft: 0, dragging: false, moved: false })

function getX(e) {
  return e.touches ? e.touches[0].pageX : e.pageX
}

function onDragStart(e) {
  const el = e.currentTarget
  dragState.value = { el, startX: getX(e), scrollLeft: el.scrollLeft, dragging: true, moved: false }
}

function onDragMove(e) {
  if (!dragState.value.dragging) return
  const dx = getX(e) - dragState.value.startX
  if (Math.abs(dx) > 3) dragState.value.moved = true
  dragState.value.el.scrollLeft = dragState.value.scrollLeft - dx
}

function onDragEnd() {
  dragState.value.dragging = false
}

function onRowClick(e) {
  if (dragState.value.moved) e.stopPropagation()
}
</script>

<style scoped>
.about-page {
  --img-size: calc(100vh - 52px - 100px);
  height: calc(100vh - 52px);
  overflow: hidden;
  background: #fff;
}

.image-row {
  display: flex;
  gap: 5px;
  height: 100%;
  padding: 50px 0;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
  cursor: grab;
  box-sizing: border-box;
}

.image-row::-webkit-scrollbar { display: none; }
.image-row:active { cursor: grabbing; }

.square-img {
  height: var(--img-size);
  width: var(--img-size);
  object-fit: cover;
  flex-shrink: 0;
  user-select: none;
  -webkit-user-drag: none;
}

.info-card {
  height: var(--img-size);
  width: calc(var(--img-size) * 2 + 5px);   /* 两张图的宽度 + 中间缝 */
  flex-shrink: 0;
}
</style>
