<template>
  <div class="page">
    <div class="page-head">
      <h1>菜单管理</h1>
      <span class="sub">目录（M）→ 菜单（C）→ 按钮（B）；菜单变更后角色重新登录生效</span>
      <div class="actions"><el-button type="primary" :icon="Plus" @click="openCreate(null)">新增菜单</el-button></div>
    </div>

    <div class="card">
      <el-table :data="tree" row-key="id" :tree-props="{ children: 'children' }" default-expand-all v-loading="loading">
        <el-table-column prop="name" label="菜单名称" min-width="220" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 'M' ? 'primary' : row.type === 'C' ? 'success' : 'info'">{{ TYPE[row.type] || row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由 / 路径" min-width="190"><template #default="{ row }">{{ row.path || '—' }}</template></el-table-column>
        <el-table-column prop="perm" label="权限标识" min-width="180"><template #default="{ row }">{{ row.perm || '—' }}</template></el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="可见" width="90">
          <template #default="{ row }"><el-tag size="small" :type="row.visible === 1 ? 'success' : 'info'">{{ row.visible === 1 ? '显示' : '隐藏' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.type !== 'B'" link type="primary" @click="openCreate(row)">加子项</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dlg" :title="form.id ? '编辑菜单' : '新增菜单'" width="480px" destroy-on-close>
      <el-form :model="form" label-width="96px" label-position="left">
        <el-form-item label="上级菜单">
          <el-select v-model="form.parentId" style="width: 100%" clearable placeholder="顶级菜单">
            <el-option v-for="m in flat" :key="m.id" :label="`${'　'.repeat(m.level)}${m.name}`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单名称" required><el-input v-model="form.menuName" maxlength="20" /></el-form-item>
        <el-form-item label="菜单类型" required>
          <el-radio-group v-model="form.menuType">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="B">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路由路径"><el-input v-model="form.path" maxlength="60" placeholder="如 /ponds" /></el-form-item>
        <el-form-item label="权限标识"><el-input v-model="form.perm" maxlength="60" placeholder="如 pond:list" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="1" :max="999" controls-position="right" /></el-form-item>
        <el-form-item label="是否显示"><el-switch v-model="form.visible" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import http from '../api/http'

const TYPE = { M: '目录', C: '菜单', B: '按钮' }
const tree = ref([])
const loading = ref(false)
const saving = ref(false)
const dlg = ref(false)
const form = reactive({ id: null, parentId: null, menuName: '', menuType: 'C', path: '', perm: '', sort: 10, visible: 1 })

/** 生成带层级的扁平列表，供上级菜单下拉使用 */
const flat = computed(() => {
  const out = []
  const walk = (items, level) => items.forEach((m) => {
    out.push({ id: m.id, name: m.name, level })
    if (m.children?.length) walk(m.children, level + 1)
  })
  walk(tree.value, 0)
  return out
})

async function load() {
  loading.value = true
  try { tree.value = await http.get('/api/system/menu/tree') } finally { loading.value = false }
}
function openCreate(parent) {
  Object.assign(form, { id: null, parentId: parent?.id || null, menuName: '', menuType: parent ? 'C' : 'M', path: '', perm: '', sort: 10, visible: 1 })
  dlg.value = true
}
function openEdit(row) {
  Object.assign(form, { id: row.id, parentId: row.parentId || null, menuName: row.name, menuType: row.type, path: row.path || '', perm: row.perm || '', sort: row.sort || 10, visible: row.visible ?? 1 })
  dlg.value = true
}
async function save() {
  if (!form.menuName) { ElMessage.warning('请填写菜单名称'); return }
  saving.value = true
  try {
    await http.post('/api/system/menu', form)
    ElMessage.success('保存成功'); dlg.value = false; load()
  } finally { saving.value = false }
}

onMounted(load)
</script>
