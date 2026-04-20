import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '@shared/stores/auth'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/login',
      name: 'H5Login',
      component: () => import('@h5/views/Login.vue'),
      meta: { guest: true },
    },
    {
      path: '/',
      component: () => import('@h5/layouts/TabLayout.vue'),
      children: [
        { path: '', redirect: '/tasks' },
        { path: 'tasks', name: 'TaskList', component: () => import('@h5/views/TaskList.vue') },
        { path: 'history', name: 'History', component: () => import('@h5/views/History.vue') },
      ],
    },
    {
      path: '/tasks/:id',
      name: 'TaskDetail',
      component: () => import('@h5/views/TaskDetail.vue'),
    },
    {
      path: '/tasks/:id/handle',
      name: 'HandleTask',
      component: () => import('@h5/views/HandleTask.vue'),
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.guest) return auth.isLoggedIn ? '/tasks' : true
  if (!auth.isLoggedIn) return '/login'
  return true
})

export default router
