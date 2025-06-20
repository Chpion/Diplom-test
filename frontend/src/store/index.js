import { store } from 'quasar/wrappers'
import { createStore } from 'vuex'
import auth from './auth'

export default store(function () {
  const Store = createStore({
    modules: {
      auth,
    },
  })
  console.log('Vuex store created:', Store)
  console.log('Initial auth state:', Store.state.auth)
  return Store
})
