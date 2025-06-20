<template>
  <q-page padding>
    <q-card class="q-pa-md" style="max-width: 400px; margin: auto">
      <q-card-section>
        <div class="text-h6">Register</div>
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
          <q-input
            v-model="form.email"
            label="Email"
            :rules="[
              (val) => !!val || 'Email is required',
              (val) => /.+@.+\..+/.test(val) || 'Invalid email',
            ]"
          />
          <q-input
            v-model="form.lastname"
            label="Last Name"
            :rules="[(val) => !!val || 'Last Name is required']"
          />
          <q-input
            v-model="form.firstname"
            label="First Name"
            :rules="[(val) => !!val || 'First Name is required']"
          />
          <q-input v-model="form.middlename" label="Middle Name" />
          <q-input
            v-model="form.dateOfBirth"
            label="Date of Birth (YYYY.MM.DD)"
            :rules="[
              (val) => !!val || 'Date is required',
              (val) => /^\d{4}\.\d{2}\.\d{2}$/.test(val) || 'Format: YYYY.MM.DD',
            ]"
          />
          <q-btn
            type="submit"
            color="primary"
            label="Register"
            class="q-mt-md"
            :loading="loading"
          />
          <q-btn flat label="Login" to="/login" class="q-mt-md q-ml-sm" />
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
const form = ref({
  login: '',
  password: '',
  email: '',
  lastname: '',
  firstname: '',
  middlename: '',
  dateOfBirth: '',
  role: 'USER',
})
const loading = ref(false)

console.log('RegisterPage: store exists?', !!store)

const onSubmit = async () => {
  loading.value = true
  try {
    console.log('Register attempt:', form.value)
    if (!store) {
      throw new Error('Vuex store is not initialized')
    }
    await store.dispatch('auth/register', form.value)
    console.log('Registration successful, redirecting to /login')
    router.push('/login')
    Notify.create({ type: 'positive', message: 'Registration successful' })
  } catch (error) {
    console.error('Registration error:', error)
    const errorMessage = error.message || error || 'Registration failed'
    Notify.create({ type: 'negative', message: errorMessage })
  } finally {
    loading.value = false
  }
}
</script>
