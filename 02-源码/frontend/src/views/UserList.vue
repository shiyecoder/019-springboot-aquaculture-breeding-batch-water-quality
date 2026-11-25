<template>
  <div class="page">
    <div class="page-head">
      <h1>用户管理</h1>
      <span class="sub">共 {{ total }} 个账号 · 按角色分配菜单，按数据范围控制可见数据</span>
      <div class="actions"><el-button type="primary" :icon="Plus" @click="openCreate">新增用户</el-button></div>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="账号 / 姓名" clearable @keyup.enter="search" />
      <el-select v-model="q.orgId" placeholder="所属组织" clearable>
        <el-option v-for="o in orgs" :key="o.id" :label="o.orgName" :value="o.id" />
      </el-select>
      <el-select v-model="q.roleId" placeholder="角色" clearable>
        <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
      </el-select>
      <div class="tail">
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column label="头像" width="72">
          <template #default="{ row }"><el-avatar :size="32" :src="row.avatar">{{ row.realName?.slice(0, 1) }}</el-avatar></template>
        </el-table-column>
        <el-table-column prop="username" label="登录账号" width="130" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="orgName" label="所属组织" min-width="140" />
        <el-table-column prop="roleName" label="角色" width="110" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="200" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="最近登录" width="160" class-name="nowrap"><template #default="{ row }">{{ formatTime(row.lastLogin) }}</template></el-table-column>
        <el-table-column label="操作" width="180" fixed="right" class-name="nowrap">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="reset2(row)">重置密码</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="当前筛选条件下无用户" :image-size="70" /></template>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="load" @current-change="load" />
      </div>
    </div>

    <el-drawer v-model="dlg" :title="form.id ? '编辑用户' : '新增用户'" size="480px" destroy-on-close>
      <el-form :model="form" label-width="96px" label-position="left">
        <el-form-item label="登录账号" required :disabled="!!form.id">
          <el-input v-model="form.username" :disabled="!!form.id" maxlength="30" />
        </el-form-item>
        <el-form-item label="姓名" required><el-input v-model="form.realName" maxlength="20" /></el-form-item>
        <el-form-item label="初始密码" v-if="!form.id"><el-input v-model="form.password" maxlength="30" placeholder="默认 123456" /></el-form-item>
        <el-form-item label="所属组织" required>
          <el-select v-model="form.orgId" style="width: 100%">
            <el-option v-for="o in orgs" :key="o.id" :label="o.orgName" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="form.roleId" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="`${r.roleName}（${r.roleCode}）`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" maxlength="20" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" maxlength="50" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshLeft } from '@element-plus/icons-vue'
import http from '../api/http'
import { formatTime } from '../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dlg = ref(false)
const orgs = ref([])
const roles = ref([])
const q = reactive({ page: 1, size: 10, keyword: '', orgId: null, roleId: null })
const form = reactive({ id: null, username: '', realName: '', password: '', orgId: null, roleId: null, phone: '', email: '', status: 1 })

async function load() {
  loading.value = true
  try {
    const d = await http.get('/api/system/user/page', { params: { ...q, keyword: q.keyword || undefined, orgId: q.orgId || undefined, roleId: q.roleId || undefined } })
    rows.value = d.list || []
    total.value = d.total || 0
  } finally { loading.value = false }
}
function search() { q.page = 1; load() }
function reset() { Object.assign(q, { page: 1, keyword: '', orgId: null, roleId: null }); load() }

function openCreate() {
  Object.assign(form, { id: null, username: '', realName: '', password: '123456', orgId: orgs.value[0]?.id || null, roleId: roles.value[0]?.id || null, phone: '', email: '', status: 1 })
  dlg.value = true
}
function openEdit(row) {
  Object.assign(form, { ...row, password: '' })
  dlg.value = true
}
async function save() {
  if (!form.realName) { ElMessage.warning('请填写姓名'); return }
  if (!form.id && !form.username) { ElMessage.warning('请填写登录账号'); return }
  saving.value = true
  try {
    if (form.id) await http.put(`/api/system/user/${form.id}`, form)
    else await http.post('/api/system/user', form)
    ElMessage.success('保存成功'); dlg.value = false; load()
  } finally { saving.value = false }
}
async function reset2(row) {
  await ElMessageBox.confirm(`确认将 ${row.realName} 的密码重置为 123456？`, '重置密码', { type: 'warning' })
  await http.post(`/api/system/user/${row.id}/reset-password`)
  ElMessage.success('密码已重置为 123456')
}

onMounted(async () => {
  load()
  orgs.value = await http.get('/api/system/org/list')
  roles.value = await http.get('/api/system/role/list')
})
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; padding: 12px 18px; }
</style>
