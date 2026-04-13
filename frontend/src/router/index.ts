import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/',
      component: () => import('@/layout/AppLayout.vue'),
      children: [
        { path: '', redirect: '/literatures' },
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue') },
        { path: 'workflow', name: 'workflow', component: () => import('@/views/workflow/WorkflowView.vue') },
        { path: 'literatures', name: 'literatures', component: () => import('@/views/literature/LiteratureView.vue') },
        { path: 'extraction/indicators', name: 'extraction-indicators', component: () => import('@/views/extraction/IndicatorsView.vue') },
        { path: 'extraction/evidence', name: 'extraction-evidence', component: () => import('@/views/extraction/EvidenceView.vue') },
        { path: 'analysis', name: 'analysis', component: () => import('@/views/analysis/AnalysisView.vue') },
        { path: 'topology', name: 'topology', component: () => import('@/views/topology/TopologyView.vue') },
        { path: 'graph', name: 'graph', component: () => import('@/views/graph/GraphView.vue') },
        { path: 'tasks', name: 'tasks', component: () => import('@/views/tasks/TasksView.vue') },
        { path: 'logs', name: 'logs', component: () => import('@/views/logs/LogsView.vue'), meta: { requiresAdmin: true } },
        { path: 'users', name: 'users', component: () => import('@/views/users/UsersView.vue'), meta: { requiresAdmin: true } }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (!userStore.isAuthenticated && to.path !== '/login') {
    return '/login'
  }
  if (userStore.isAuthenticated && to.meta.guestOnly) {
    return '/literatures'
  }
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    return '/literatures'
  }
  return true
})

export default router
