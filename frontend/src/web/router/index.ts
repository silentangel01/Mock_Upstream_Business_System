import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@shared/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@web/views/Login.vue'),
      meta: { guest: true },
    },
    {
      path: '/',
      component: () => import('@web/layouts/DefaultLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'Dashboard', component: () => import('@web/views/Dashboard.vue') },
        { path: 'tickets', name: 'TicketList', component: () => import('@web/views/TicketList.vue') },
        { path: 'tickets/:id', name: 'TicketDetail', component: () => import('@web/views/TicketDetail.vue') },
        {
          path: 'dispatch-rules',
          name: 'DispatchRules',
          component: () => import('@web/views/DispatchRules.vue'),
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'users',
          name: 'UserManager',
          component: () => import('@web/views/UserManager.vue'),
          meta: { roles: ['ADMIN'] },
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.guest) return auth.isLoggedIn ? '/dashboard' : true
  if (!auth.isLoggedIn) return '/login'
  if (to.meta.roles && !(to.meta.roles as string[]).includes(auth.role)) return '/dashboard'
  return true
})

export default router
