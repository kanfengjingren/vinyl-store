import { createApp } from 'vue';
import { createPinia } from 'pinia';
import router from './router';
import App from './App.vue';
import 'animate.css';
import './style.css';

// 刷新前记住滚动位置
window.addEventListener('beforeunload', () => {
  sessionStorage.setItem('__scrollY', window.scrollY);
});

const app = createApp(App);


app.use(createPinia());
app.use(router);



app.mount('#app');
