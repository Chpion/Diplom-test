<template>
  <q-page padding>
    <q-card class="q-pa-md" style="max-width: 400px; margin: auto">
      <q-card-section>
        <div class="text-h6">Login</div>
      </q-card-section>
      <q-card-section>
        <q-form @submit="onSubmit">
          <q-input
            v-model="form.login"
            label="Login"
            :rules="[(val) => !!val || 'Login is required']"
          />
          <q-input
            v-model="form.password"
            label="Password"
            type="password"
            :rules="[(val) => !!val || 'Password is required']"
          />
          <q-btn type="submit" color="primary" label="Login" class="q-mt-md" :loading="loading" />
          <q-btn flat label="Register" to="/register" class="q-mt-md q-ml-sm" />
        </q-form>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { ref } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { Notify } from 'quasar'

const store = useStore()
const router = useRouter()
const form = ref({ login: '', password: '' })
const loading = ref(false)

console.log('LoginPage: store exists?', !!store)

const onSubmit = async () => {
  loading.value = true
  try {
    console.log('Login attempt:', form.value)
    if (!store) {
      throw new Error('Vuex store is not initialized')
    }
    await store.dispatch('auth/login', form.value)
    console.log('Login successful, redirecting to /search')
    router.push('/search')
    Notify.create({ type: 'positive', message: 'Login successful' })
  } catch (error) {
    console.error('Login error:', error)
    const errorMessage = error.message || error || 'Login failed'
    Notify.create({ type: 'negative', message: errorMessage })
  } finally {
    loading.value = false
  }
}
</script>
