<template>
  <q-page padding>
    <h2>Админ-панель</h2>

    <div class="q-mb-md">
      <q-input v-model="searchKeyword" label="Поиск пользователей" debounce="300" @input="searchUsers" />
      <q-btn color="primary" label="Обновить БД" class="q-ml-md" @click="confirmUpdateDatabase" />
    </div>

    <q-table
      :rows="users"
      :columns="columns"
      row-key="login"
      :loading="loading"
      :filter="searchKeyword"
    >
      <template v-slot:body-cell-fullName="props">
        <q-td :props="props">
          {{ props.row.lastName }} {{ props.row.firstName }} {{ props.row.middleName || '' }}
        </q-td>
      </template>
      <template v-slot:body-cell-roles="props">
        <q-td :props="props">
          {{ props.row.roles.join(', ') }}
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn color="negative" label="Удалить" size="sm" class="q-mr-sm" @click="confirmDeleteUser(props.row.login)" />
          <q-btn color="primary" label="Сменить роль" size="sm" @click="confirmUpdateRole(props.row.login)" />
        </q-td>
      </template>
    </q-table>
  </q-page>
</template>

<script>
export default {
  data() {
    return {
      searchKeyword: '',
      users: [],
      loading: false,
      columns: [
        { name: 'login', label: 'Логин', field: 'login', sortable: true },
        { name: 'email', label: 'Email', field: 'email', sortable: true },
        { name: 'fullName', label: 'ФИО', field: 'fullName', sortable: true },
        { name: 'roles', label: 'Роль', field: 'roles', sortable: true },
        { name: 'actions', label: 'Действия', align: 'center' }
      ]
    }
  },
  methods: {
    async fetchAllUsers() {
      this.loading = true;
      try {
        const response = await this.$store.dispatch('auth/getAllUsers');
        this.users = response.data;
      } catch (error) {
        this.$q.notify({ type: 'negative', message: error.message });
      } finally {
        this.loading = false;
      }
    },
    async searchUsers() {
      this.loading = true;
      try {
        if (this.searchKeyword) {
          const response = await this.$store.dispatch('auth/searchUsers', this.searchKeyword);
          this.users = response.data;
        } else {
          await this.fetchAllUsers();
        }
      } catch (error) {
        this.$q.notify({ type: 'negative', message: error.message });
      } finally {
        this.loading = false;
      }
    },
    confirmDeleteUser(login) {
      if (window.confirm(`Вы уверены, что хотите удалить пользователя ${login}?`)) {
        this.$store.dispatch('auth/deleteUser', login)
          .then((response) => {
            this.$q.notify({ type: 'positive', message: response.message });
            this.fetchAllUsers();
          })
          .catch((error) => {
            console.error('Ошибка удаления пользователя:', error);
            this.$q.notify({ type: 'negative', message: error.message || 'Ошибка удаления пользователя' });
          });
      }
    },
    confirmUpdateRole(login) {
      const role = window.prompt(`Введите новую роль для пользователя ${login} (USER или ADMIN):`);
      if (role && ['USER', 'ADMIN'].includes(role.toUpperCase())) {
        const formattedRole = `ROLE_${role.toUpperCase()}`; // Форматируем роль сразу
        console.log('Sending role:', formattedRole); // Логирование для диагностики
        this.$store.dispatch('auth/updateUserRoles', { login, roleName: formattedRole })
          .then((response) => {
            this.$q.notify({ type: 'positive', message: response.message });
            this.fetchAllUsers();
          })
          .catch((error) => {
            console.error('Ошибка смены роли:', error);
            this.$q.notify({ type: 'negative', message: error.message || 'Ошибка смены роли' });
          });
      } else {
        this.$q.notify({ type: 'negative', message: 'Недопустимая роль. Используйте USER или ADMIN.' });
      }
    },
    confirmUpdateDatabase() {
      if (window.confirm('Вы уверены, что хотите обновить базу данных? Этот процесс может занять от 30 до 60 минут.')) {
        this.$store.dispatch('auth/updateDatabase')
          .then((response) => {
            this.$q.notify({ type: 'positive', message: response.message });
          })
          .catch((error) => {
            this.$q.notify({ type: 'negative', message: error.message });
          });
      }
    }
  },
  mounted() {
    this.fetchAllUsers();
  }
}
</script>
