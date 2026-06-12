<template>
  <transition
    mode="out-in"
    enter-active-class="animate__animated animate__fadeIn animate__faster"
    leave-active-class="animate__animated animate__fadeOut animate__faster"
  >
    <router-view />
  </transition>
  <AppModal />
</template>

<script setup>
import { AppModal } from '@vinyl-store/shared';
</script>
