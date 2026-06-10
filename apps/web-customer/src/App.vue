<template>
  <!-- <div class="h-full"> -->
    <NavBar class="shrink-0" />

    <router-view :key="route.fullPath" v-slot="{ Component }">
      <transition name="page" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>

    <Footer v-if="!auth.isAdmin" />
    <CartSidebar />
    <AudioPlayer />
    <ToastNotification />
    <AppModal />
  <!-- </div> -->

</template>

<script setup>
import NavBar from './components/layout/NavBar.vue';
import Footer from './components/layout/Footer.vue';
import CartSidebar from './components/layout/CartSidebar.vue';
import AudioPlayer from './components/player/AudioPlayer.vue';
import { ToastNotification, AppModal } from '@vinyl-store/shared/ui';
import { useRoute } from 'vue-router';
import { useAuthStore } from './stores/auth.js';
const route = useRoute()
const auth = useAuthStore()
</script>
