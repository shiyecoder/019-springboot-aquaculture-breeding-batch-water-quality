<template>
  <div class="login-wrap">
    <div class="brand-side">
      <div class="brand-top">
        <span class="mark">渔</span>
        <div>
          <div class="nm">水产养殖批次与水质监测管理系统</div>
          <div class="en">AQUACULTURE BATCH &amp; WATER QUALITY MONITORING</div>
        </div>
      </div>
      <div class="slogan">
        <h1>一塘一档，一测一档</h1>
        <p>塘口、批次、投喂、水质、预警、病害与起捕全流程留痕。<br />多参数风险分层给出可解释的处置建议，不替养殖户做不可逆决定。</p>
      </div>
      <div class="stats">
        <div><b>19</b><span>业务数据表</span></div>
        <div><b>4</b><span>业务角色</span></div>
        <div><b>6</b><span>水质参数分层</span></div>
      </div>
      <div class="foot">东区养殖场 · 西区养殖场 · 技术质检部</div>
    </div>

    <div class="form-side">
      <div class="form-box">
        <h2>登录</h2>
        <p class="sub">使用养殖场分配的账号进入系统</p>
        <el-form :model="form" @submit.prevent="submit">
          <el-form-item>
            <el-input v-model="form.username" size="large" placeholder="账号" :prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码" :prefix-icon="Lock" @keyup.enter="submit" />
          </el-form-item>
          <el-button type="primary" size="large" class="btn" :loading="loading" @click="submit">登 录</el-button>
        </el-form>
        <div class="hint">
          <span>演示账号</span>
          <el-tag size="small" @click="fill('admin', 'admin123')">admin / admin123</el-tag>
          <el-tag size="small" type="info" @click="fill('manager01', '123456')">场长</el-tag>
          <el-tag size="small" type="info" @click="fill('tech01', '123456')">技术员</el-tag>
          <el-tag size="small" type="info" @click="fill('feeder01', '123456')">养殖员</el-tag>
        </div>
      </div>
      <div class="copy">© 2026 水产养殖数字化管理 · 毕业设计演示系统</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import http from '../api/http'
import { useUserStore } from '../store/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })

function fill(u, p) {
  form.username = u
  form.password = p
}

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号与密码')
    return
  }
  loading.value = true
  try {
    const data = await http.post('/api/auth/login', { username: form.username.trim(), password: form.password })
    store.setSession(data.token, data.user, data.menus)
    ElMessage.success(`欢迎回来，${data.user.realName}`)
    const target = route.query.redirect || (data.user.roleCode === 'FEEDER' ? '/portal' : '/dashboard')
    router.push(target)
  } catch (e) {
    /* 错误提示由拦截器统一处理 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap { display: flex; min-height: 100vh; background: #F6F8F7; }
.brand-side {
  width: 58%; min-width: 620px; padding: 48px 56px; color: #fff; position: relative; display: flex; flex-direction: column;
  background: linear-gradient(150deg, #1E3A4D 0%, #284B63 46%, #3C6E71 100%);
}
.brand-side:after {
  content: ""; position: absolute; right: -1px; bottom: 0; left: 0; height: 320px; opacity: .42;
  background: url("/login-bg.jpg") center/cover no-repeat;
  -webkit-mask-image: linear-gradient(to top, #000 30%, transparent); mask-image: linear-gradient(to top, #000 30%, transparent);
}
.brand-top { display: flex; align-items: center; gap: 12px; position: relative; z-index: 1; }
.brand-top .mark { width: 40px; height: 40px; border-radius: 9px; background: rgba(255,255,255,.14); border: 1px solid rgba(255,255,255,.3); display: grid; place-items: center; font-weight: 700; }
.brand-top .nm { font-size: 15px; font-weight: 600; letter-spacing: .4px; }
.brand-top .en { font-size: 10.5px; color: #B9D2DA; letter-spacing: 1.4px; margin-top: 3px; }
.slogan { margin: auto 0; position: relative; z-index: 1; }
.slogan h1 { font-size: 38px; line-height: 1.2; margin: 0 0 16px; font-weight: 600; letter-spacing: 2px; }
.slogan p { font-size: 13.5px; line-height: 26px; color: #CFE0E4; margin: 0; max-width: 460px; }
.stats { display: flex; gap: 46px; margin-top: 34px; position: relative; z-index: 1; }
.stats div { display: flex; flex-direction: column; gap: 4px; }
.stats b { font-size: 26px; font-weight: 600; font-variant-numeric: tabular-nums; }
.stats span { font-size: 11.5px; color: #B9D2DA; letter-spacing: .5px; }
.foot { margin-top: 40px; font-size: 11.5px; color: #9FBAC6; position: relative; z-index: 1; }
.form-side { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px; }
.form-box { width: 360px; }
.form-box h2 { font-size: 24px; margin: 0 0 6px; font-weight: 600; color: var(--c-text); }
.form-box .sub { font-size: 12.5px; color: var(--c-text-2); margin: 0 0 26px; }
.btn { width: 100%; margin-top: 6px; letter-spacing: 4px; }
.hint { margin-top: 22px; font-size: 12px; color: var(--c-text-2); display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.hint .el-tag { cursor: pointer; }
.copy { position: absolute; bottom: 22px; font-size: 11.5px; color: #8FA3A8; }
@media (max-width: 1100px) { .brand-side { display: none; } }
</style>
