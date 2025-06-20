import { boot } from 'quasar/wrappers'
import store from '../store'

export default boot(({ app }) => {
  const vuexStore = store()
  console.log('Registering Vuex store instance:', vuexStore)
  app.use(vuexStore)
})
