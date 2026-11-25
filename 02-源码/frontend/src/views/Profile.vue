<template>
  <div class="page">
    <div class="page-head">
      <h1>个人中心</h1>
      <span class="sub">修改资料、密码与头像，头像保存后右上角立即同步</span>
    </div>

    <div class="grid-2-1">
      <div class="card">
        <div class="card-h"><h3>基本资料</h3></div>
        <div class="card-b">
          <el-form :model="form" label-width="96px" label-position="left" style="max-width: 520px">
            <el-form-item label="登录账号"><el-input v-model="form.username" disabled /></el-form-item>
            <el-form-item label="姓名" required><el-input v-model="form.realName" maxlength="20" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="form.phone" maxlength="20" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="form.email" maxlength="50" /></el-form-item>
            <el-form-item label="所属组织"><el-input :model-value="form.orgName" disabled /></el-form-item>
            <el-form-item label="角色"><el-input :model-value="form.roleName" disabled /></el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <div class="card">
        <div class="card-h"><h3>头像</h3><span class="hint">JPG/PNG ≤5MB</span></div>
        <div class="card-b" style="text-align: center">
          <el-avatar :size="96" :src="form.avatar"><span style="font-size: 30px">{{ form.realName?.slice(0, 1) }}</span></el-avatar>
          <el-upload class="av-up" :show-file-list="false" :http-request="uploadAvatar" accept="image/*">
            <el-button size="small" :loading="uploading">更换头像</el-button>
          </el-upload>
          <div class="tip mt8">上传后自动保存，并同步到顶栏头像</div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-h"><h3>修改密码</h3><span class="hint">密码使用 BCrypt 存储，接口与日志均不返回密码</span></div>
      <div class="card-b">
        <el-form :model="pwd" label-width="96px" label-position="left" style="max-width: 520px">
          <el-form-item label="原密码" required><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
          <el-form-item label="新密码" required><el-input v-model="pwd.newPassword" type="password" show-password placeholder="不少于 6 位" /></el-form-item>
          <el-form-item label="确认新密码" required><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="savePwd">修改密码</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useUserStore } from '../store/user'

const store = useUserStore()
const saving = ref(false)
const uploading = ref(false)
const form = reactive({ username: '', realName: '', phone: '', email: '', orgName: '', roleName: '', avatar: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

async function load() {
  const d = await http.get('/api/auth/profile')
  Object.assign(form, d)
}

async function saveProfile() {
  if (!form.realName) { ElMessage.warning('姓名不能为空'); return }
  saving.value = true
  try {
    await http.put('/api/auth/profile', { realName: form.realName, phone: form.phone, email: form.email })
    store.setUser({ realName: form.realName, phone: form.phone, email: form.email })
    ElMessage.success('资料已保存')
  } finally { saving.value = false }
}

/** 头像走 /api/auth/avatar，成功后立即写回会话，顶栏同步刷新 */
async function uploadAvatar({ file }) {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await http.post('/api/auth/avatar', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    form.avatar = res.avatar || res.path
    store.setUser({ avatar: form.avatar })
    ElMessage.success('头像已更新')
  } finally { uploading.value = false }
}

async function savePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) { ElMessage.warning('请填写原密码与新密码'); return }
  if (pwd.newPassword.length < 6) { ElMessage.warning('新密码不能少于 6 位'); return }
  if (pwd.newPassword !== pwd.confirm) { ElMessage.warning('两次输入的新密码不一致'); return }
  saving.value = true
  try {
    await http.put('/api/auth/password', { oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    ElMessage.success('密码已修改，请重新登录')
    setTimeout(() => { store.logout(); location.href = '/login' }, 800)
  } finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.av-up { margin-top: 14px; }
</style>
