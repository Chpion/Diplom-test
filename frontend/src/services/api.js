import axios from 'axios'
import store from 'src/store'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
})

api.interceptors.request.use((config) => {
  let token = store().state.auth.token || localStorage.getItem('token');
  console.log('Interceptor: Config before processing:', config);
  console.log('Interceptor: Token for request', config.url, ':', token);
  if (token && !config.url.includes('/auth/login') && !config.url.includes('/search/')) {
    config.headers.Authorization = `Bearer ${token}`;
    console.log('Interceptor: Added Authorization header:', config.headers.Authorization);
  } else {
    console.log('Interceptor: No token or login request, skipping Authorization');
  }
  console.log('Interceptor: Config after processing:', config);
  return config;
}, (error) => {
  console.error('Interceptor error:', error);
  return Promise.reject(error);
});

export default {
  searchES(keyword) {
    return api.get('/search/bySearchES', { params: { keyword } })
  },
  getAddressInfo(keyword) {
    return api.get('/search/getAddressInfo', { params: { keyword } })
  },
  advancedSearch({ region, city, street, house }) {
    return api.get('/search/advancedSearch', {
      params: { region, city, street, house },
    });
  },
  login(credentials) {
    return api.post('/auth/login', {
      login: credentials.login,
      password: credentials.password
    })
  },
  register(userData) {
    const formData = new FormData()
    Object.keys(userData).forEach((key) => {
      formData.append(key, userData[key])
    })
    return api.post('/auth/register', formData)
  },
  deleteUser(login) {
    return api.delete(`/auth/users/${login}`)
  },
  updateUserRoles(login, roleName) {
    console.log('Sending roleName to server:', roleName); // Логирование для диагностики
    return api.put(`/auth/users/${login}/roles`, roleName, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
  },
  getAllUsers() {
    return api.get('/auth/users')
  },
  searchUsers(keyword) {
    return api.get('/auth/users/search', { params: { keyword } })
  },
  editUser(userData) {
    console.log('Sending editUser request with data:', userData);
    return api.put('/auth/edit', userData)
  },
  changePassword(passwordData) {
    return api.put('/auth/change-password', passwordData)
  },
  getCurrentUser() {
    console.log('Calling getCurrentUser');
    return api.get('/auth/me');
  },
  updateDatabase() {
    return api.post('/import/update-from-fias')
  },
}
