<template>
  <router-view />
</template>

<script setup>
import { useStore } from 'vuex'

const store = useStore()
console.log('App.vue: store exists?', !!store)
</script>
