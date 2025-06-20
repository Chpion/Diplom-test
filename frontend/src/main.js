import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import { Quasar, Dialog, Notify } from 'quasar'
import '@quasar/extras/material-icons/material-icons.css'
import 'quasar/src/css/index.sass'

const app = createApp(App)

app.use(router)
app.use(store)
app.use(Quasar, {
  plugins: { Dialog, Notify } // Подключаем Dialog и Notify
})

app.mount('#app')
