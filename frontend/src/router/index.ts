import { createRouter, createWebHistory } from 'vue-router'
import SearchView from '@/views/SearchView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/search',
      name: 'search',
      component: SearchView,
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/search',
    },
  ],
})

export default router
