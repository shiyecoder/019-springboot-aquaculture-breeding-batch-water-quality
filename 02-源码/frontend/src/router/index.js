import { createRouter, createWebHistory } from 'vue-router'

/** 路由表：菜单 path 与后端 sys_menu.path 保持一致（下列 path 即菜单 path） */
const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true, title: '登录' } },
  {
    path: '/',
    component: () => import('../layout/AppLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '养殖批次看板' } },
      { path: 'ponds', component: () => import('../views/PondList.vue'), meta: { title: '塘口总览' } },
      { path: 'batches', component: () => import('../views/BatchList.vue'), meta: { title: '养殖批次' } },
      { path: 'batches/:id', component: () => import('../views/BatchDetail.vue'), meta: { title: '批次详情' } },
      { path: 'feeding', component: () => import('../views/FeedingPlan.vue'), meta: { title: '投喂计划' } },
      { path: 'water', component: () => import('../views/WaterList.vue'), meta: { title: '水质监测' } },
      { path: 'water/new', component: () => import('../views/WaterEntry.vue'), meta: { title: '录入水质' } },
      { path: 'warnings', component: () => import('../views/WarningList.vue'), meta: { title: '风险预警中心' } },
      { path: 'warnings/:id', component: () => import('../views/WarningDetail.vue'), meta: { title: '预警详情' } },
      { path: 'disease', component: () => import('../views/DiseaseList.vue'), meta: { title: '病害用药' } },
      { path: 'harvest', component: () => import('../views/HarvestList.vue'), meta: { title: '分塘与起捕' } },
      { path: 'rules', component: () => import('../views/RuleConfig.vue'), meta: { title: '水质风险规则' } },
      { path: 'messages', component: () => import('../views/MessageList.vue'), meta: { title: '消息中心' } },
      { path: 'profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } },
      { path: 'users', component: () => import('../views/UserList.vue'), meta: { title: '用户管理' } },
      { path: 'roles', component: () => import('../views/RoleList.vue'), meta: { title: '角色管理' } },
      { path: 'menus', component: () => import('../views/MenuList.vue'), meta: { title: '菜单管理' } },
      { path: 'logs', component: () => import('../views/LogList.vue'), meta: { title: '操作日志' } },
      { path: 'portal', component: () => import('../views/PortalHome.vue'), meta: { title: '我的今日任务' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({ history: createWebHistory(), routes })

/** 未登录统一跳登录页；养殖员默认进入门户首页 */
router.beforeEach((to) => {
  const token = localStorage.getItem('aq_token')
  if (to.meta.public) return true
  if (!token) return { path: '/login', query: { redirect: to.fullPath } }
  const user = JSON.parse(localStorage.getItem('aq_user') || 'null')
  if (user?.roleCode === 'FEEDER' && ['/dashboard', '/'].includes(to.path)) {
    return { path: '/portal' }
  }
  return true
})

export default router
