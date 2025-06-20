<template>
  <q-layout view="lHh Lpr lFf">
    <q-header elevated>
      <app-header />
    </q-header>
    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import AppHeader from 'src/components/AppHeader.vue'
</script>
