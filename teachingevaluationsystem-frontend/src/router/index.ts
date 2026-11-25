import { createRouter, createWebHistory } from 'vue-router'
import { clearAuth, getSide, getToken } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/student/login' },
    { path: '/admin/login', component: () => import('@/pages/AdminLoginPage.vue') },
    { path: '/student/login', component: () => import('@/pages/StudentLoginPage.vue') },
    {
      path: '/admin',
      component: () => import('@/pages/AdminLayoutPage.vue'),
      children: [
        { path: '', redirect: '/admin/classes' },
        { path: 'classes', component: () => import('@/pages/ClassesPage.vue') },
        { path: 'students', component: () => import('@/pages/StudentsPage.vue') },
        { path: 'teachers', component: () => import('@/pages/TeachersPage.vue') },
        { path: 'admin-users', component: () => import('@/pages/AdminUsersPage.vue') },
        { path: 'courses', component: () => import('@/pages/CoursesPage.vue') },
        { path: 'indicators', component: () => import('@/pages/IndicatorsPage.vue') },
        { path: 'evaluations', component: () => import('@/pages/EvaluationsPage.vue') },
        { path: 'scores', component: () => import('@/pages/ScoresPage.vue') }
      ]
    },
    { path: '/student/tasks', component: () => import('@/pages/TasksPage.vue') },
    { path: '/student/tasks/score/:taskId', component: () => import('@/pages/StudentScorePage.vue') }
  ]
})

router.beforeEach((to) => {
  const token = getToken()
  const side = getSide()
  const isAdminRoute = to.path.startsWith('/admin')
  const isStudentRoute = to.path.startsWith('/student')
  const isAdminLogin = to.path === '/admin/login'
  const isStudentLogin = to.path === '/student/login'
  const isLoginRoute = isAdminLogin || isStudentLogin

  if (token && side !== 'admin' && side !== 'student') {
    clearAuth()
    return isAdminRoute ? '/admin/login' : '/student/login'
  }

  if (!token && !isLoginRoute) {
    return isAdminRoute ? '/admin/login' : '/student/login'
  }

  if (token && isLoginRoute) {
    if (isAdminLogin && side === 'admin') return '/admin/classes'
    if (isStudentLogin && side === 'student') return '/student/tasks'
    clearAuth()
    return true
  }

  if (token && isAdminRoute && side !== 'admin') {
    clearAuth()
    return '/admin/login'
  }
  if (token && isStudentRoute && side !== 'student') {
    clearAuth()
    return '/student/login'
  }

  return true
})

export default router
