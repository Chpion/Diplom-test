<template>
  <q-toolbar>
    <q-toolbar-title>Поиск адресов</q-toolbar-title>
    <q-btn v-if="isAuthenticated" flat label="Поиск" to="/search" />
    <q-btn v-if="isAuthenticated" flat label="Профиль" to="/profile" />
    <q-btn v-if="!isAuthenticated" flat label="Войти" to="/login" />
    <q-btn v-if="!isAuthenticated" flat label="Регистрация" to="/register" />
    <q-btn v-if="isAuthenticated" flat label="Выход" @click="logout" />
  </q-toolbar>
</template>

<script setup>
import { computed } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'

const store = useStore()
const router = useRouter()

console.log('AppHeader: store exists?', !!store)

const isAuthenticated = computed(() => {
  return store ? store.getters['auth/isAuthenticated'] : false
})

const logout = () => {
  if (store) {
    store.dispatch('auth/logout')
    router.push('/login')
  } else {
    console.error('Vuex store is not initialized')
    router.push('/login')
  }
}
</script>
