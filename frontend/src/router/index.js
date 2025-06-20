import { route } from 'quasar/wrappers'
import { createRouter, createWebHistory } from 'vue-router'
import routes from './routes'
import store from 'src/store'

export default route(function () {
  const Router = createRouter({
    history: createWebHistory(),
    routes,
  })

  const vuexStore = store()

  Router.beforeEach((to, from, next) => {
    console.log(
      'Router beforeEach: store exists?',
      !!vuexStore,
      'to:',
      to.path,
      'isAuthenticated:',
      vuexStore.getters['auth/isAuthenticated'],
      'auth state:',
      vuexStore.state.auth,
    )
    if (to.path === '/login' || to.path === '/register') {
      next()
      return
    }
    const isAuthenticated = vuexStore.getters['auth/isAuthenticated']
    const isAdmin = vuexStore.getters['auth/isAdmin']
    if (to.meta.requiresAuth && !isAuthenticated) {
      next('/login')
    } else if (to.meta.requiresAdmin && !isAdmin) {
      next('/profile') // Перенаправление, если не админ
    } else {
      next()
    }
  })

  return Router
})
