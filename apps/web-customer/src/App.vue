<template>
  <NavBar class="shrink-0" />


  <!-- routerview -->
  <router-view v-slot="{ Component, route: r }" :key="route.name">
    <transition
      mode="out-in"
      enter-active-class="animate__animated animate__fadeIn animate__faster"
      leave-active-class="animate__animated animate__fadeOut animate__faster"
    >

      <keep-alive include="HomePage">
        <component :is="Component" :key="r.name" />
      </keep-alive>

    </transition>
  </router-view>

  <CartSidebar />
  <AudioPlayer />
  <PlayerOverlay />
  <ToastNotification />
  <AppModal />
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from './stores/auth'
import NavBar from './components/layout/NavBar.vue';
import CartSidebar from './components/layout/CartSidebar.vue';
import AudioPlayer from './components/player/AudioPlayer.vue';

const auth = useAuthStore()
onMounted(() => { auth.checkAuth() })
import PlayerOverlay from './components/player/PlayerOverlay.vue';
import { ToastNotification, AppModal } from '@vinyl-store/shared/ui';
import { useRoute } from "vue-router";
const route = useRoute();
</script>
