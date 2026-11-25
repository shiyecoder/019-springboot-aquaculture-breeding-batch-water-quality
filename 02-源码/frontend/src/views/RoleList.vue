<template>
  <div class="page">
    <div class="page-head">
      <h1>角色管理</h1>
      <span class="sub">共 {{ rows.length }} 个角色 · 角色决定菜单权限与数据范围</span>
      <div class="actions"><el-button type="primary" :icon="Plus" @click="openCreate">新增角色</el-button></div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="roleName" label="角色名称" width="140" />
        <el-table-column prop="roleCode" label="角色编码" width="130" />
        <el-table-column label="数据范围" width="140">
          <template #default="{ row }">
            <el-tag size="small" :type="row.dataScope === 'ALL' ? 'success' : row.dataScope === 'DEPT' ? 'primary' : 'info'">{{ SCOPE[row.dataScope] || row.dataScope }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="职责说明" min-width="240" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="创建时间" width="160"><template #default="{ row }">{{ formatTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openAssign(row)">分配菜单</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无角色" :image-size="70" /></template>
      </el-table>
    </div>

    <el-dialog v-model="dlg" :title="form.id ? '编辑角色' : '新增角色'" width="460px" destroy-on-close>
      <el-form :model="form" label-width="96px" label-position="left">
        <el-form-item label="角色名称" required><el-input v-model="form.roleName" maxlength="20" /></el-form-item>
        <el-form-item label="角色编码" required><el-input v-model="form.roleCode" maxlength="20" placeholder="如 FEEDER" /></el-form-item>
        <el-form-item label="数据范围" required>
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option label="全部数据" value="ALL" />
            <el-option label="本部门数据" value="DEPT" />
            <el-option label="仅本人数据" value="SELF" />
          </el-select>
        </el-form-item>
        <el-form-item label="职责说明"><el-input v-model="form.remark" type="textarea" :rows="2" maxlength="80" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDlg" :title="`为「${cur?.roleName || ''}」分配菜单`" width="480px" destroy-on-close>
      <div class="notice">菜单管理节点仅可分配给系统管理员；保存后该角色重新登录即可看到新菜单。</div>
      <el-tree
        ref="treeRef"
        :data="menuTree"
        show-checkbox
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        default-expand-all
        class="mt16"
      />
      <template #footer>
        <el-button @click="assignDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import http from '../api/http'
import { formatTime } from '../utils/format'

const SCOPE = { ALL: '全部数据', DEPT: '本部门', SELF: '仅本人' }
const rows = ref([])
const menuTree = ref([])
const loading = ref(false)
const saving = ref(false)
const dlg = ref(false)
const assignDlg = ref(false)
const cur = ref(null)
const treeRef = ref(null)
const form = reactive({ id: null, roleName: '', roleCode: '', dataScope: 'SELF', remark: '', status: 1 })

async function load() {
  loading.value = true
  try { rows.value = await http.get('/api/system/role/list') } finally { loading.value = false }
}
function openCreate() {
  Object.assign(form, { id: null, roleName: '', roleCode: '', dataScope: 'SELF', remark: '', status: 1 })
  dlg.value = true
}
function openEdit(row) { Object.assign(form, row); dlg.value = true }
async function save() {
  if (!form.roleName || !form.roleCode) { ElMessage.warning('请填写角色名称与编码'); return }
  saving.value = true
  try {
    await http.post('/api/system/role', form)
    ElMessage.success('保存成功'); dlg.value = false; load()
  } finally { saving.value = false }
}

async function openAssign(row) {
  cur.value = row
  assignDlg.value = true
  const [tree, checked] = await Promise.all([
    http.get('/api/system/menu/tree'),
    http.get(`/api/system/role/${row.id}/menus`)
  ])
  menuTree.value = tree
  await nextTick()
  // 半选父节点也要勾上，否则子菜单会因父节点未存而失去导航分组
  const ids = (checked || []).slice()
  ids.forEach((id) => {
    const node = treeRef.value?.getNode(id)
    if (node && node.parent && node.parent.data && node.parent.data.id) ids.push(node.parent.data.id)
  })
  treeRef.value?.setCheckedKeys(Array.from(new Set(ids)))
}
async function saveAssign() {
  const checked = treeRef.value.getCheckedKeys().concat(treeRef.value.getHalfCheckedKeys())
  saving.value = true
  try {
    await http.post(`/api/system/role/${cur.value.id}/menus`, { menuIds: checked })
    ElMessage.success('菜单权限已保存'); assignDlg.value = false
  } finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.mt16 { margin-top: 12px; }
</style>
