<template>
  <q-page padding>
    <h2>Смена пароля</h2>
    <q-form @submit="changePassword">
      <q-input v-model="passwordForm.currentPassword" label="Текущий пароль" type="password" :rules="[val => !!val || 'Текущий пароль обязателен']" />
      <q-input v-model="passwordForm.newPassword" label="Новый пароль" type="password" :rules="[val => !!val || 'Новый пароль обязателен']" />
      <q-btn type="submit" color="primary" label="Сменить пароль" />
      <q-btn color="secondary" label="Назад" to="/profile" class="q-ml-md" />
    </q-form>
  </q-page>
</template>

<script>
export default {
  name: 'ChangePassword',
  data() {
    return {
      passwordForm: {
        currentPassword: '',
        newPassword: ''
      }
    }
  },
  methods: {
    async changePassword() {
      try {
        const response = await this.$store.dispatch('auth/changePassword', this.passwordForm);
        this.$q.notify({ type: 'positive', message: response.message });
        this.passwordForm.currentPassword = '';
        this.passwordForm.newPassword = '';
        this.$router.push('/profile');
      } catch (error) {
        this.$q.notify({ type: 'negative', message: error.message });
      }
    }
  }
}
</script>
