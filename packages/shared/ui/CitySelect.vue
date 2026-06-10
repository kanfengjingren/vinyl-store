<template>
  <div>
    <div class="flex gap-3 mb-3">
      <select
        :value="province"
        @change="onProvinceChange"
        class="flex-1 px-3 py-2.5 rounded-xl border border-black/15 text-[14px] text-black/80 bg-white outline-none focus:border-[rgb(196,147,51)] focus:ring-2 focus:ring-[rgb(196,147,51)]/15 transition-all cursor-pointer appearance-none"
        style="background-image: url('data:image/svg+xml;utf8,<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%2212%22 height=%2212%22 viewBox=%220 0 24 24%22 fill=%22none%22 stroke=%22%23999%22 stroke-width=%222%22><path d=%22m6 9 6 6 6-6%22/></svg>'); background-repeat: no-repeat; background-position: right 10px center; padding-right: 30px;"
      >
        <option value="">选择省/直辖市</option>
        <option v-for="p in regions" :key="p.name" :value="p.name">{{ p.name }}</option>
      </select>
      <select
        :value="city"
        @change="onCityChange"
        :disabled="!province"
        class="flex-1 px-3 py-2.5 rounded-xl border border-black/15 text-[14px] text-black/80 bg-white outline-none focus:border-[rgb(196,147,51)] focus:ring-2 focus:ring-[rgb(196,147,51)]/15 transition-all cursor-pointer appearance-none disabled:opacity-40 disabled:cursor-not-allowed"
        style="background-image: url('data:image/svg+xml;utf8,<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%2212%22 height=%2212%22 viewBox=%220 0 24 24%22 fill=%22none%22 stroke=%22%23999%22 stroke-width=%222%22><path d=%22m6 9 6 6 6-6%22/></svg>'); background-repeat: no-repeat; background-position: right 10px center; padding-right: 30px;"
      >
        <option value="">选择市/区</option>
        <option v-for="c in cities" :key="c" :value="c">{{ c }}</option>
      </select>
    </div>
    <textarea
      :value="detail"
      @input="onDetailInput"
      rows="2"
      placeholder="详细地址：街道、小区、门牌号等"
      class="w-full px-4 py-3 rounded-xl border border-black/15 text-[15px] text-black/80 outline-none focus:border-[rgb(196,147,51)] focus:ring-2 focus:ring-[rgb(196,147,51)]/15 transition-all resize-none placeholder:text-black/25"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { regions } from '../data/regions.js';

const props = defineProps({
  modelValue: { type: String, default: '' },   // 完整地址
  detail: { type: String, default: '' },        // 详细地址（v-model:detail）
});

const emit = defineEmits(['update:modelValue', 'update:detail']);

// 从完整地址中解析省/市/详情
const province = ref('');
const city = ref('');

// 初始化：尝试从 modelValue 解析已有地址
function parseAddress(addr) {
  if (!addr) return;
  for (const p of regions) {
    if (addr.startsWith(p.name)) {
      province.value = p.name;
      const rest = addr.slice(p.name.length).trim();
      for (const c of p.cities) {
        if (rest.startsWith(c)) {
          city.value = c;
          // 剩余部分给 detail（如果 detail 为空）
          if (!props.detail) {
            emit('update:detail', rest.slice(c.length).trim());
          }
          return;
        }
      }
      return;
    }
  }
}

watch(() => props.modelValue, parseAddress, { immediate: true });

const cities = computed(() => {
  if (!province.value) return [];
  const p = regions.find(r => r.name === province.value);
  return p ? p.cities : [];
});

function emitFull(detailOverride) {
  const parts = [];
  if (province.value) parts.push(province.value);
  if (city.value) parts.push(city.value);
  const d = (detailOverride ?? props.detail)?.trim();
  if (d) parts.push(d);
  emit('update:modelValue', parts.join(' '));
}

function onDetailInput(e) {
  const val = e.target.value;
  emit('update:detail', val);
  emitFull(val); // 直接用新值，不等 prop 更新
}

function onProvinceChange(e) {
  province.value = e.target.value;
  city.value = '';
  emitFull();
}

function onCityChange(e) {
  city.value = e.target.value;
  emitFull();
}
</script>
