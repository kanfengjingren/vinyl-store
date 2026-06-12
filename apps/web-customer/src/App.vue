<template>
  <!-- <div class="h-full"> -->
    <NavBar class="shrink-0" />

    <router-view v-slot="{ Component, route: r }">
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
    <ToastNotification />
    <AppModal />
  <!-- </div> -->

</template>

<script setup>
import NavBar from './components/layout/NavBar.vue';
import CartSidebar from './components/layout/CartSidebar.vue';
import AudioPlayer from './components/player/AudioPlayer.vue';
import { ToastNotification, AppModal } from '@vinyl-store/shared/ui';
</script>
