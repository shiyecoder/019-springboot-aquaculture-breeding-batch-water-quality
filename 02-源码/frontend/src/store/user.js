import { reactive } from 'vue'

/**
 * 极简用户状态容器（项目未引入 Pinia，保持依赖轻量）。
 * 令牌与用户信息持久化到 localStorage，键前缀 aq_（水产 Aquaculture）。
 */
const TOKEN_KEY = 'aq_token'
const USER_KEY = 'aq_user'

export const state = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: JSON.parse(localStorage.getItem(USER_KEY) || 'null'),
  menus: []
})

/** 登录成功后写入会话 */
export function setSession(token, user, menus) {
  state.token = token
  state.user = user
  state.menus = menus || []
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  localStorage.setItem('aq_menus', JSON.stringify(state.menus))
}

/** 刷新页面后恢复会话（菜单随会话一起缓存，避免重复请求） */
export function restore() {
  state.token = localStorage.getItem(TOKEN_KEY) || ''
  state.user = JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  state.menus = JSON.parse(localStorage.getItem('aq_menus') || '[]')
}

export function setUser(user) {
  state.user = { ...state.user, ...user }
  localStorage.setItem(USER_KEY, JSON.stringify(state.user))
}

export function setMenus(menus) {
  state.menus = menus || []
  localStorage.setItem('aq_menus', JSON.stringify(state.menus))
}

export function logout() {
  state.token = ''
  state.user = null
  state.menus = []
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem('aq_menus')
}

/** 是否拥有某个权限点（ADMIN 拥有全部权限） */
export function hasPerm(perm) {
  if (!perm) return true
  if (state.user?.roleCode === 'ADMIN') return true
  return state.menus.some((m) => m.perm === perm || (m.children || []).some((c) => c.perm === perm))
}

export function useUserStore() {
  return { state, setSession, restore, setUser, setMenus, logout, hasPerm }
}
