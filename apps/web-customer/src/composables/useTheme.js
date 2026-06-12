import { ref } from 'vue';

const KEY = 'theme';
const isDark = ref(localStorage.getItem(KEY) === 'dark');

// 页面加载时立即应用
document.documentElement.classList.toggle('dark', isDark.value);

export function useTheme() {
  function toggle() {
    isDark.value = !isDark.value;
    localStorage.setItem(KEY, isDark.value ? 'dark' : 'light');
    document.documentElement.classList.toggle('dark', isDark.value);
  }
  return { isDark, toggle };
}
