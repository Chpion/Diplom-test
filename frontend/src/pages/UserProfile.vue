<template>
  <q-page padding>
    <h2>Профиль пользователя</h2>

    <div v-if="$store.getters['auth/isAdmin']" class="q-mb-md">
      <q-btn color="primary" label="Админ-панель" to="/admin-panel" />
    </div>

    <h3>Ваши данные</h3>
    <q-card class="q-mb-md">
      <q-card-section>
        <div><strong>Логин:</strong> {{ user?.login }}</div>
        <div><strong>Email:</strong> {{ user?.email }}</div>
        <div><strong>Фамилия:</strong> {{ user?.lastName }}</div>
        <div><strong>Имя:</strong> {{ user?.firstName }}</div>
        <div><strong>Отчество:</strong> {{ user?.middleName || '-' }}</div>
        <div><strong>Дата рождения:</strong> {{ user?.dateOfBirth || '-' }}</div>
        <div><strong>Роль:</strong> {{ user?.roles.join(', ') }}</div>
      </q-card-section>
    </q-card>

    <div class="q-mb-md">
      <q-btn color="primary" label="Редактировать профиль" to="/edit-profile" class="q-mr-md" />
      <q-btn color="primary" label="Сменить пароль" to="/change-password" />
    </div>
  </q-page>
</template>

<script>
export default {
  name: 'UserProfile',
  computed: {
    user() {
      return this.$store.getters['auth/user']
    }
  },
  methods: {
    async loadUserData() {
      try {
        await this.$store.dispatch('auth/getCurrentUser');
      } catch (error) {
        this.$q.notify({ type: 'negative', message: error.message });
      }
    }
  },
  mounted() {
    this.loadUserData();
  }
}
</script>
