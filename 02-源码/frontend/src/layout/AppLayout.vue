<template>
  <!-- 养殖员使用门户风格；场长/技术员/仓管员/管理员使用后台管理风格 -->
  <div v-if="isPortal" class="portal-wrap">
    <header class="portal-top">
      <div class="logo"><i>渔</i>塘口作业台</div>
      <nav>
        <a :class="{ on: route.path === '/portal' }" @click="go('/portal')">今日任务</a>
        <a :class="{ on: route.path === '/water/new' }" @click="go('/water/new')">水质录入</a>
        <a :class="{ on: route.path === '/warnings' }" @click="go('/warnings')">预警处置</a>
        <a :class="{ on: route.path === '/messages' }" @click="go('/messages')">消息</a>
        <a :class="{ on: route.path === '/profile' }" @click="go('/profile')">个人中心</a>
      </nav>
      <div class="right flex">
        <span class="tip">{{ user.orgName }} · {{ user.realName }}</span>
        <el-avatar :size="30" :src="avatar"><span>{{ user.realName?.slice(0, 1) }}</span></el-avatar>
        <el-button link type="primary" @click="logout">退出</el-button>
      </div>
    </header>
    <router-view />
  </div>

  <div v-else class="app">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="brand">
        <span class="logo">渔</span>
        <b v-show="!collapsed">水产养殖管理平台</b>
      </div>
      <nav class="menu">
        <template v-for="node in menuNodes" :key="node.key">
          <div v-if="node.children" class="grp" v-show="!collapsed">{{ node.name }}</div>
          <template v-if="node.children">
            <a v-for="c in node.children" :key="c.key" :class="{ on: isActive(c.to) }" @click="go(c.to)" :title="c.name">
              <el-icon class="ic"><component :is="c.icon" /></el-icon>
              <span v-show="!collapsed">{{ c.name }}</span>
            </a>
          </template>
          <a v-else :class="{ on: isActive(node.to) }" @click="go(node.to)" :title="node.name">
            <el-icon class="ic"><component :is="node.icon" /></el-icon>
            <span v-show="!collapsed">{{ node.name }}</span>
          </a>
        </template>
      </nav>
      <div class="side-foot" v-show="!collapsed">lane1 · v1.0.0</div>
    </aside>

    <div class="main">
      <header class="topbar">
        <el-icon class="fold" @click="collapsed = !collapsed"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
        <span class="crumb">{{ crumb }}</span>
        <span class="spacer"></span>
        <el-popover placement="bottom-end" :width="330" trigger="click" @show="loadLatest">
          <template #reference>
            <span class="icon-btn">
              <el-badge :value="unread" :hidden="!unread" :max="99"><el-icon><Bell /></el-icon></el-badge>
            </span>
          </template>
          <div class="msg-pop">
            <div class="flex" style="padding: 2px 2px 10px; border-bottom: 1px solid var(--c-border-l)">
              <b>站内消息</b>
              <el-button link type="primary" class="right" @click="readAll">全部已读</el-button>
            </div>
            <div v-for="m in latest" :key="m.id" class="msg-item" @click="openMsg(m)">
              <div class="t">{{ m.title }}</div>
              <div class="m">{{ formatTime(m.createTime) }}</div>
            </div>
            <el-empty v-if="!latest.length" description="暂无消息" :image-size="60" />
            <div style="text-align: center; padding-top: 8px">
              <el-button link type="primary" @click="go('/messages')">进入消息中心</el-button>
            </div>
          </div>
        </el-popover>
        <span class="icon-btn" @click="go('/messages')" title="消息中心"><el-icon><ChatDotSquare /></el-icon></span>
        <!-- 用 mouseenter 触发，自动化脚本可直接派发事件打开菜单 -->
        <el-dropdown trigger="click" @command="onCommand">
          <div class="who">
            <el-avatar :size="30" :src="avatar"><span>{{ user.realName?.slice(0, 1) }}</span></el-avatar>
            <div>
              <div class="n">{{ user.realName }}</div>
              <div class="r">{{ user.roleName }}</div>
            </div>
            <el-icon style="margin-left: 4px; color: #5B7176"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="messages">消息中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>
      <div class="content"><router-view /></div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import http from '../api/http'
import { formatTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const user = computed(() => store.state.user || {})
const isPortal = computed(() => user.value.roleCode === 'FEEDER')
const avatar = computed(() => user.value.avatar || '')
const collapsed = ref(false)
const unread = ref(0)
const latest = ref([])
let timer = null

/** 后端菜单 path 与前端路由对齐（/dashboard/index 是工作台子菜单） */
const PATH_MAP = {
  '/dashboard/index': '/dashboard',
  '/dashboard': '/dashboard',
  '/ponds': '/ponds',
  '/batches': '/batches',
  '/feeding': '/feeding',
  '/water': '/water',
  '/warnings': '/warnings',
  '/disease': '/disease',
  '/harvest': '/harvest',
  '/rules': '/rules',
  '/messages': '/messages',
  '/profile': '/profile',
  '/users': '/users',
  '/roles': '/roles',
  '/menus': '/menus',
  '/logs': '/logs'
}

const ICONS = {
  '/dashboard': 'DataBoard',
  '/ponds': 'Ship',
  '/batches': 'Files',
  '/feeding': 'Bowl',
  '/water': 'Odometer',
  '/warnings': 'Warning',
  '/disease': 'FirstAidKit',
  '/harvest': 'Box',
  '/rules': 'SetUp',
  '/messages': 'ChatDotSquare',
  '/profile': 'UserFilled',
  '/users': 'User',
  '/roles': 'Avatar',
  '/menus': 'Menu',
  '/logs': 'Tickets'
}

const menuNodes = computed(() => {
  const list = []
  const walk = (items) => {
    items.forEach((m, idx) => {
      const to = PATH_MAP[m.path]
      if (m.children && m.children.length) {
        const children = m.children
          .map((c) => ({ key: `${m.id}-${c.id}`, name: c.name, to: PATH_MAP[c.path], icon: ICONS[PATH_MAP[c.path]] || 'Document' }))
          .filter((c) => c.to)
        // 目录自身若是页面（如工作台）也保留入口
        if (to) list.push({ key: `${m.id}-self`, name: m.name, to, icon: ICONS[to] || 'Document' })
        if (children.length) list.push({ key: `${m.id}`, name: m.name, children })
      } else if (to) {
        list.push({ key: `${m.id}-${idx}`, name: m.name, to, icon: ICONS[to] || 'Document' })
      }
    })
  }
  walk(store.state.menus || [])
  return list
})

const crumb = computed(() => {
  const title = route.meta.title || ''
  return title ? `水产养殖管理 / ${title}` : '水产养殖管理'
})

function isActive(path) {
  return path && (route.path === path || route.path.startsWith(path + '/'))
}
function go(path) {
  if (path && route.path !== path) router.push(path)
}
function logout() {
  store.logout()
  router.push('/login')
}
function onCommand(cmd) {
  if (cmd === 'logout') logout()
  else go('/' + cmd)
}
async function loadUnread() {
  try {
    const r = await http.get('/api/message/unread')
    unread.value = r?.count || 0
  } catch (e) {
    /* 未登录或网络异常时静默 */
  }
}
async function loadLatest() {
  try {
    latest.value = await http.get('/api/message/latest', { params: { limit: 5 } })
  } catch (e) {
    latest.value = []
  }
}
async function readAll() {
  await http.post('/api/message/read-all')
  unread.value = 0
  ElMessage.success('已全部标记为已读')
  loadLatest()
}
/** 消息跳转：按业务类型回到对应详情，保证消息与单据闭环 */
function openMsg(m) {
  http.post(`/api/message/${m.id}/read`).then(() => loadUnread())
  if (m.bizType === 'WARNING') go(`/warnings/${m.bizId}`)
  else if (m.bizType === 'DISEASE') go('/disease')
  else if (m.bizType === 'HARVEST') go('/harvest')
  else if (m.bizType === 'FEEDING') go('/feeding')
  else if (m.bizType === 'WATER') go('/water')
}

onMounted(() => {
  store.restore()
  loadUnread()
  timer = setInterval(loadUnread, 60000)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.app { display: flex; height: 100vh; overflow: hidden; }
.sidebar { width: 220px; height: 100vh; background: var(--c-primary); color: #DCE7EB; display: flex; flex-direction: column; flex: 0 0 220px; transition: width .18s; }
.sidebar.collapsed { width: 64px; flex: 0 0 64px; }
.brand { height: 56px; display: flex; align-items: center; gap: 10px; padding: 0 18px; border-bottom: 1px solid rgba(255,255,255,.12); }
.brand .logo { width: 26px; height: 26px; border-radius: 6px; background: #fff; color: var(--c-primary); display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px; }
.brand b { font-size: 13.5px; color: #fff; font-weight: 600; white-space: nowrap; letter-spacing: .3px; }
.menu { padding: 8px 0; flex: 1; overflow: auto; }
.menu .grp { padding: 14px 18px 6px; font-size: 11px; color: #9FBAC6; letter-spacing: 1px; }
.menu a { display: flex; align-items: center; gap: 10px; height: 38px; padding: 0 18px; color: #CFDDE4; font-size: 13px; border-left: 3px solid transparent; cursor: pointer; white-space: nowrap; }
.menu a:hover { background: rgba(255,255,255,.08); color: #fff; }
.menu a.on { background: rgba(255,255,255,.12); color: #fff; border-left-color: #fff; font-weight: 600; padding-left: 15px; }
.menu a .ic { font-size: 16px; }
.side-foot { padding: 12px 18px; border-top: 1px solid rgba(255,255,255,.12); font-size: 11px; color: #9FBAC6; }
.main { flex: 1; min-width: 0; min-height: 0; height: 100vh; display: flex; flex-direction: column; }
.topbar { height: 56px; background: #fff; border-bottom: 1px solid var(--c-border); display: flex; align-items: center; padding: 0 24px; gap: 14px; position: sticky; top: 0; z-index: 20; }
.fold { cursor: pointer; color: var(--c-text-2); font-size: 18px; }
.crumb { color: var(--c-text-2); font-size: 13px; }
.spacer { flex: 1; }
.icon-btn { width: 32px; height: 32px; border-radius: 6px; display: flex; align-items: center; justify-content: center; color: var(--c-text-2); cursor: pointer; }
.icon-btn:hover { background: var(--c-primary-l); }
.who { display: flex; align-items: center; gap: 9px; padding-left: 14px; border-left: 1px solid var(--c-border); cursor: pointer; }
.who .n { font-size: 13px; font-weight: 600; line-height: 16px; }
.who .r { font-size: 11px; color: var(--c-text-2); line-height: 14px; }
.content { flex: 1; min-height: 0; overflow: auto; }
.msg-item { padding: 8px 2px; border-bottom: 1px solid #F3F6F5; cursor: pointer; }
.msg-item:hover { background: #F8FBFA; }
.msg-item .t { font-size: 12px; line-height: 18px; }
.msg-item .m { font-size: 11px; color: var(--c-text-2); }
.portal-wrap { min-height: 100vh; background: var(--c-bg); }
</style>
