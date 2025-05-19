import './assets/main.scss'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { useAnalyzerStore } from "@/stores/analyzer"

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

useAnalyzerStore(pinia).init()
  .finally(() => {
    app.mount('#app')
  })
