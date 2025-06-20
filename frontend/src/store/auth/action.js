import api from 'src/services/api'

export default {
  async login({ commit }, credentials) {
    try {
      console.log('Login action called with:', credentials);
      commit('setToken', null);
      const response = await api.login(credentials);
      console.log('Login response, token:', response.data);
      commit('setToken', response.data);
      const userResponse = await api.getCurrentUser();
      console.log('Current user response:', userResponse.data);
      commit('setUser', userResponse.data);
      return response;
    } catch (error) {
      console.error('Login failed:', error);
      console.error('Error details:', error.response?.data || error.message);
      throw error;
    }
  },
  async register({ _commit }, userData) {
    try {
      console.log('Register action called with:', userData);
      const response = await api.register(userData);
      console.log('Register response:', response.data);
      return response.data;
    } catch (error) {
      console.error('Registration failed:', error.response?.data || error.message);
      throw error.response?.data?.error || 'Registration failed';
    }
  },
  async getCurrentUser({ commit }) {
    try {
      const response = await api.getCurrentUser();
      commit('setUser', response.data);
      return { success: true, data: response.data };
    } catch (error) {
      console.error('Ошибка получения данных пользователя:', error);
      throw new Error(`Ошибка получения данных пользователя: ${error.response?.data || error.message}`);
    }
  },
  async editUser({ commit }, userData) {
    try {
      const response = await api.editUser(userData);
      if (response.data.token) {
        commit('setToken', response.data.token);
        // Сохраняем токен для последующих запросов
        localStorage.setItem('token', response.data.token);
        console.log('New token set:', response.data.token);
      }
      const userResponse = await api.getCurrentUser();
      commit('setUser', userResponse.data);
      return { success: true, message: response.data.message };
    } catch (error) {
      console.error('Ошибка редактирования:', error.response?.data || error.message);
      throw new Error(`Ошибка редактирования профиля: ${error.response?.data?.error || error.message}`);
    }
  },
  async changePassword({ _commit }, passwordData) {
    try {
      await api.changePassword(passwordData);
      return { success: true, message: 'Пароль успешно изменен' };
    } catch (error) {
      throw new Error(`Ошибка смены пароля: ${error.response?.data || error.message}`);
    }
  },
  async deleteUser({ _commit }, login) {
    try {
      await api.deleteUser(login);
      return { success: true, message: `Пользователь ${login} удален` };
    } catch (error) {
      throw new Error(`Ошибка удаления: ${error.response?.data || error.message}`);
    }
  },
  async updateUserRoles({ _commit }, { login, roleName }) {
    try {
      console.log('Sending roleName to server:', roleName); // Логирование
      await api.updateUserRoles(login, roleName);
      return { success: true, message: `Роль пользователя ${login} обновлена` };
    } catch (error) {
      console.error('Ошибка обновления роли:', error.response?.data || error.message);
      throw new Error(`Ошибка обновления роли: ${error.response?.data || error.message}`);
    }
  },
  async getAllUsers({ _commit }) {
    try {
      const response = await api.getAllUsers();
      return { success: true, data: response.data };
    } catch (error) {
      throw new Error(`Ошибка получения пользователей: ${error.response?.data || error.message}`);
    }
  },
  async searchUsers({ _commit }, keyword) {
    try {
      const response = await api.searchUsers(keyword);
      return { success: true, data: response.data };
    } catch (error) {
      throw new Error(`Ошибка поиска пользователей: ${error.response?.data || error.message}`);
    }
  },
  async updateDatabase({ _commit }) {
    try {
      const response = await api.updateDatabase();
      return { success: true, message: response.data };
    } catch (error) {
      throw new Error(`Ошибка обновления базы данных: ${error.response?.data || error.message}`);
    }
  },
  async getAddressInfo({ _commit }, address) {
    try {
      const response = await api.getAddressInfo(address);
      return { success: true, data: response.data };
    } catch (error) {
      console.error('Get address info failed:', error.response?.data || error.message);
      throw new Error(`Не удалось получить детали адреса: ${error.response?.data || error.message}`);
    }
  },
  logout({ commit }) {
    commit('logout');
    return Promise.resolve(true);
  },
}
