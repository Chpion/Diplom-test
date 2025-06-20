<template>
  <q-page padding>
    <h2>Редактировать профиль</h2>
    <q-form class="q-mb-md" @submit="updateProfile">
      <q-input v-model="form.login" label="Логин" :rules="[val => !!val || 'Логин обязателен']" />
      <q-input v-model="form.email" label="Email" type="email" :rules="[val => !!val || 'Email обязателен']" />
      <q-input v-model="form.lastName" label="Фамилия" :rules="[val => !!val || 'Фамилия обязательна']" />
      <q-input v-model="form.firstName" label="Имя" :rules="[val => !!val || 'Имя обязательно']" />
      <q-input v-model="form.middleName" label="Отчество" />
      <q-input v-model="form.dateOfBirth" label="Дата рождения (гггг.мм.дд)" :rules="[val => !!val || 'Дата рождения обязательна']" />
      <q-btn type="submit" color="primary" label="Сохранить изменения" />
      <q-btn color="secondary" label="Назад" to="/profile" class="q-ml-md" />
    </q-form>
  </q-page>
</template>

<script>
export default {
  name: 'EditProfile',
  data() {
    return {
      form: {
        login: '',
        email: '',
        lastName: '',
        firstName: '',
        middleName: '',
        dateOfBirth: ''
      }
    }
  },
  methods: {
    async loadUserData() {
      try {
        const response = await this.$store.dispatch('auth/getCurrentUser');
        const dateOfBirth = response.data.dateOfBirth
          ? new Date(response.data.dateOfBirth).toISOString().split('T')[0].replace(/-/g, '.')
          : '';
        this.form = {
          login: response.data.login,
          email: response.data.email,
          lastName: response.data.lastName || '',
          firstName: response.data.firstName,
          middleName: response.data.middleName || '',
          dateOfBirth
        };
      } catch (error) {
        this.$q.notify({ type: 'negative', message: error.message });
      }
    },
    async updateProfile() {
      try {
        const response = await this.$store.dispatch('auth/editUser', this.form);
        this.$q.notify({ type: 'positive', message: response.message });
        this.$router.push('/profile');
      } catch (error) {
        console.error('Ошибка редактирования:', error);
        this.$q.notify({ type: 'negative', message: error.message || 'Ошибка редактирования профиля' });
      }
    }
  },
  mounted() {
    this.loadUserData();
  }
}
</script>
