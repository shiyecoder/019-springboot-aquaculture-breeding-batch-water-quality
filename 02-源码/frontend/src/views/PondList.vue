<template>
  <div class="page">
    <div class="page-head">
      <h1>塘口总览</h1>
      <span class="sub">共 {{ total }} 个塘口 · 存塘与在养批次实时汇总</span>
      <div class="actions">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增塘口</el-button>
      </div>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="塘口编号 / 名称" clearable @keyup.enter="search" />
      <el-select v-model="q.status" placeholder="塘口状态" clearable>
        <el-option v-for="(v, k) in POND_STATUS" :key="k" :label="v.label" :value="k" />
      </el-select>
      <el-select v-model="q.waterSource" placeholder="水源类型" clearable>
        <el-option v-for="(v, k) in WATER_SOURCE" :key="k" :label="v" :value="k" />
      </el-select>
      <div class="tail">
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" row-key="id">
        <el-table-column label="塘口照片" width="86">
          <template #default="{ row }">
            <el-image :src="row.image" :preview-src-list="[row.image]" fit="cover" class="thumb" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column prop="pondNo" label="塘口编号" width="100" class-name="nowrap" />
        <el-table-column prop="pondName" label="塘口名称" min-width="110" />
        <el-table-column prop="area" label="面积(亩)" width="92" align="right">
          <template #default="{ row }"><span class="num">{{ row.area }}</span></template>
        </el-table-column>
        <el-table-column prop="depth" label="水深(m)" width="92" align="right">
          <template #default="{ row }"><span class="num">{{ row.depth }}</span></template>
        </el-table-column>
        <el-table-column label="水源" width="96">
          <template #default="{ row }">{{ WATER_SOURCE[row.waterSource] || row.waterSource }}</template>
        </el-table-column>
        <el-table-column prop="aeratorCount" label="增氧机" width="86" align="right" />
        <el-table-column prop="managerName" label="负责人" width="96" />
        <el-table-column label="在养品种" min-width="130">
          <template #default="{ row }">{{ row.variety || '—' }}</template>
        </el-table-column>
        <el-table-column label="批次/存塘" min-width="140">
          <template #default="{ row }">
            <span class="num">{{ row.batchCount }} 批 · {{ Number(row.stock || 0).toFixed(0) }} kg</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="96" class-name="nowrap">
          <template #default="{ row }">
            <el-tag size="small" :type="statusOf(POND_STATUS, row.status).type">{{ statusOf(POND_STATUS, row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" class-name="nowrap">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="goBatches(row)">批次</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="当前筛选条件下无塘口，清空筛选或新增塘口" :image-size="70" />
        </template>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="q.page"
          v-model:page-size="q.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <el-drawer v-model="drawer" :title="form.id ? '编辑塘口' : '新增塘口'" size="520px" destroy-on-close>
      <el-form :model="form" label-width="104px" label-position="left">
        <el-form-item label="塘口编号" required>
          <el-input v-model="form.pondNo" maxlength="20" placeholder="如 TK-09" />
        </el-form-item>
        <el-form-item label="塘口名称" required>
          <el-input v-model="form.pondName" maxlength="30" placeholder="如 九号塘" />
        </el-form-item>
        <el-form-item label="面积(亩)" required>
          <el-input-number v-model="form.area" :min="0.1" :precision="1" :step="0.5" controls-position="right" />
        </el-form-item>
        <el-form-item label="水深(m)" required>
          <el-input-number v-model="form.depth" :min="0.5" :precision="1" :step="0.1" controls-position="right" />
        </el-form-item>
        <el-form-item label="水源类型" required>
          <el-select v-model="form.waterSource" style="width: 210px">
            <el-option v-for="(v, k) in WATER_SOURCE" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="增氧机数量">
          <el-input-number v-model="form.aeratorCount" :min="0" :max="20" controls-position="right" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.managerId" style="width: 210px" placeholder="选择负责人" clearable>
            <el-option v-for="u in managers" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="塘口状态">
          <el-select v-model="form.status" style="width: 210px">
            <el-option v-for="(v, k) in POND_STATUS" :key="k" :label="v.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="塘口照片">
          <ImageUpload v-model="form.image" type="pond" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshLeft } from '@element-plus/icons-vue'
import http from '../api/http'
import ImageUpload from '../components/ImageUpload.vue'
import { POND_STATUS, WATER_SOURCE, statusOf } from '../utils/status'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const drawer = ref(false)
const managers = ref([])
const q = reactive({ page: 1, size: 10, keyword: '', status: '', waterSource: '' })
const form = reactive({ id: null, pondNo: '', pondName: '', area: 8, depth: 2, waterSource: 'WELL', aeratorCount: 2, managerId: null, status: 'NORMAL', image: '', remark: '' })

async function load() {
  loading.value = true
  try {
    const d = await http.get('/api/pond/page', { params: { ...q, keyword: q.keyword || undefined, status: q.status || undefined, waterSource: q.waterSource || undefined } })
    rows.value = d.list || []
    total.value = d.total || 0
  } finally {
    loading.value = false
  }
}
function search() { q.page = 1; load() }
function reset() { Object.assign(q, { page: 1, keyword: '', status: '', waterSource: '' }); load() }

function openCreate() {
  Object.assign(form, { id: null, pondNo: '', pondName: '', area: 8, depth: 2, waterSource: 'WELL', aeratorCount: 2, managerId: null, status: 'NORMAL', image: '', remark: '' })
  drawer.value = true
}
function openEdit(row) {
  Object.assign(form, row)
  drawer.value = true
}
async function save() {
  if (!form.pondNo || !form.pondName) { ElMessage.warning('请填写塘口编号与名称'); return }
  saving.value = true
  try {
    if (form.id) await http.put(`/api/pond/${form.id}`, form)
    else await http.post('/api/pond', form)
    ElMessage.success('保存成功')
    drawer.value = false
    load()
  } finally { saving.value = false }
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除塘口「${row.pondName}」？已关联批次的塘口不可删除。`, '删除确认', { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' })
  await http.delete(`/api/pond/${row.id}`)
  ElMessage.success('已删除')
  load()
}
function goBatches(row) { router.push({ path: '/batches', query: { pondId: row.id } }) }

onMounted(async () => {
  load()
  const u = await http.get('/api/system/user/page', { params: { page: 1, size: 50 } })
  managers.value = u.list || []
})
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; padding: 12px 18px; }
</style>
