<template>
  <div class="page">
    <div class="page-head">
      <h1>养殖批次</h1>
      <span class="sub">共 {{ total }} 个批次 · 核心主数据，状态机：准备中→养殖中→起捕中→已结束</span>
      <div class="actions">
        <el-button type="primary" :icon="Plus" @click="openCreate">新建批次</el-button>
      </div>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="批次编号 / 品种" clearable @keyup.enter="search" />
      <el-select v-model="q.pondId" placeholder="养殖池" clearable filterable>
        <el-option v-for="p in ponds" :key="p.id" :label="`${p.pondNo} ${p.pondName}`" :value="p.id" />
      </el-select>
      <el-select v-model="q.variety" placeholder="养殖品种" clearable>
        <el-option v-for="v in varieties" :key="v" :label="v" :value="v" />
      </el-select>
      <el-select v-model="q.status" placeholder="批次状态" clearable>
        <el-option v-for="(v, k) in BATCH_STATUS" :key="k" :label="v.label" :value="k" />
      </el-select>
      <div class="tail">
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="batchNo" label="批次编号" width="136" class-name="nowrap">
          <template #default="{ row }"><a @click="detail(row)">{{ row.batchNo }}</a></template>
        </el-table-column>
        <el-table-column label="养殖池" min-width="120">
          <template #default="{ row }">{{ row.pondNo }} {{ row.pondName }}</template>
        </el-table-column>
        <el-table-column prop="variety" label="品种" width="110" />
        <el-table-column prop="stockDate" label="投苗日期" width="106" class-name="nowrap" />
        <el-table-column label="投苗量(万尾)" width="110" align="right">
          <template #default="{ row }"><span class="num">{{ row.stockQty }}</span></template>
        </el-table-column>
        <el-table-column label="当前阶段" width="96">
          <template #default="{ row }">
            <el-tag size="small" :type="statusOf(GROWTH_STAGE, row.growthStage).type" effect="plain">{{ statusOf(GROWTH_STAGE, row.growthStage).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前存量(kg)" width="116" align="right">
          <template #default="{ row }"><span class="num">{{ Number(row.currentStock || 0).toFixed(0) }}</span></template>
        </el-table-column>
        <el-table-column label="累计投喂(kg)" width="116" align="right">
          <template #default="{ row }"><span class="num">{{ Number(row.feedTotal || 0).toFixed(0) }}</span></template>
        </el-table-column>
        <el-table-column label="最近水质" width="122" class-name="nowrap">
          <template #default="{ row }">
            <el-tag size="small" :type="statusOf(WATER_LEVEL, row.waterLevel).type">{{ statusOf(WATER_LEVEL, row.waterLevel).label }}</el-tag>
            <span class="num tip" style="margin-left: 6px">{{ row.lastCheckTime ? formatDate(row.lastCheckTime) : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="未闭环预警" width="104" align="center">
          <template #default="{ row }">
            <el-badge v-if="row.openWarning" :value="row.openWarning" type="danger" />
            <span v-else class="tip">0</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="92" class-name="nowrap">
          <template #default="{ row }">
            <el-tag size="small" :type="statusOf(BATCH_STATUS, row.status).type">{{ statusOf(BATCH_STATUS, row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="176" fixed="right" class-name="nowrap">
          <template #default="{ row }">
            <el-button link type="primary" @click="detail(row)">详情</el-button>
            <el-button link type="primary" :disabled="row.status === 'CLOSED'" @click="openTransition(row)">流转</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="当前筛选条件下无批次，清空筛选或新建批次" :image-size="70" />
        </template>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="load" @current-change="load" />
      </div>
    </div>

    <el-drawer v-model="drawer" :title="form.id ? '编辑批次' : '新建批次'" size="560px" destroy-on-close>
      <el-form :model="form" label-width="112px" label-position="left">
        <el-form-item label="养殖池" required>
          <el-select v-model="form.pondId" style="width: 220px" filterable placeholder="选择塘口">
            <el-option v-for="p in ponds" :key="p.id" :label="`${p.pondNo} ${p.pondName}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="养殖品种" required>
          <el-select v-model="form.variety" style="width: 220px" filterable allow-create placeholder="选择或输入品种">
            <el-option v-for="v in varieties" :key="v" :label="v" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item label="苗种来源">
          <el-input v-model="form.seedSource" maxlength="50" placeholder="如 市水产站 / 自繁自育" />
        </el-form-item>
        <el-form-item label="投苗日期" required>
          <el-date-picker v-model="form.stockDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 220px" />
        </el-form-item>
        <el-form-item label="投苗数量(万尾)" required>
          <el-input-number v-model="form.stockQty" :min="0.1" :precision="1" :step="0.5" controls-position="right" />
        </el-form-item>
        <el-form-item label="平均规格(g)">
          <el-input-number v-model="form.avgWeight" :min="0" :precision="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="预计起捕日">
          <el-date-picker v-model="form.expectHarvestDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 220px" />
        </el-form-item>
        <el-form-item label="目标产量(kg)">
          <el-input-number v-model="form.targetOutput" :min="0" :precision="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="当前阶段">
          <el-select v-model="form.growthStage" style="width: 220px">
            <el-option v-for="(v, k) in GROWTH_STAGE" :key="k" :label="v.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任人">
          <el-select v-model="form.managerId" style="width: 220px" clearable filterable placeholder="选择责任人">
            <el-option v-for="u in users" :key="u.id" :label="`${u.realName}（${u.roleName}）`" :value="u.id" />
          </el-select>
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

    <el-dialog v-model="transDlg" title="批次状态流转" width="440px" destroy-on-close>
      <div class="notice" v-if="trans.row">当前批次 {{ trans.row.batchNo }}：{{ statusOf(BATCH_STATUS, trans.row.status).label }}</div>
      <el-form label-width="92px" label-position="left" class="mt16">
        <el-form-item label="目标状态" required>
          <el-select v-model="trans.target" style="width: 220px">
            <el-option v-for="s in nextStatuses(trans.row?.status)" :key="s" :label="statusOf(BATCH_STATUS, s).label" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作原因">
          <el-input v-model="trans.reason" type="textarea" :rows="3" maxlength="120" placeholder="说明流转原因，便于追溯（不少于 5 个字）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doTransition">确认流转</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search, RefreshLeft } from '@element-plus/icons-vue'
import http from '../api/http'
import { BATCH_STATUS, GROWTH_STAGE, WATER_LEVEL, statusOf } from '../utils/status'
import { formatDate } from '../utils/format'

const route = useRoute()
const router = useRouter()
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const drawer = ref(false)
const transDlg = ref(false)
const ponds = ref([])
const users = ref([])
const varieties = ['草鱼', '鲫鱼', '南美白对虾', '加州鲈', '黄颡鱼', '罗非鱼']
const q = reactive({ page: 1, size: 10, keyword: '', pondId: route.query.pondId ? Number(route.query.pondId) : null, variety: '', status: '' })
const form = reactive({ id: null, pondId: null, variety: '', seedSource: '', stockDate: '', stockQty: 5, avgWeight: 20, expectHarvestDate: '', targetOutput: 2000, growthStage: 'FRY', managerId: null, remark: '' })
const trans = reactive({ row: null, target: '', reason: '' })

/** 状态机允许的下一步（与后端 BatchService.transition 保持一致） */
const FLOW = { PREP: ['BREEDING'], BREEDING: ['HARVESTING'], HARVESTING: ['CLOSED'], CLOSED: [] }
function nextStatuses(cur) { return FLOW[cur] || [] }

async function load() {
  loading.value = true
  try {
    const d = await http.get('/api/aquaculture-water/page', {
      params: { ...q, keyword: q.keyword || undefined, pondId: q.pondId || undefined, variety: q.variety || undefined, status: q.status || undefined }
    })
    rows.value = d.list || []
    total.value = d.total || 0
  } finally { loading.value = false }
}
function search() { q.page = 1; load() }
function reset() { Object.assign(q, { page: 1, keyword: '', pondId: null, variety: '', status: '' }); load() }
function detail(row) { router.push(`/batches/${row.id}`) }

function openCreate() {
  Object.assign(form, { id: null, pondId: q.pondId, variety: '', seedSource: '', stockDate: new Date().toISOString().slice(0, 10), stockQty: 5, avgWeight: 20, expectHarvestDate: '', targetOutput: 2000, growthStage: 'FRY', managerId: null, remark: '' })
  drawer.value = true
}
function openEdit(row) { Object.assign(form, row); drawer.value = true }
async function save() {
  if (!form.pondId || !form.variety || !form.stockDate) { ElMessage.warning('请填写养殖池、品种与投苗日期'); return }
  saving.value = true
  try {
    if (form.id) await http.put(`/api/aquaculture-water/${form.id}`, form)
    else await http.post('/api/aquaculture-water', form)
    ElMessage.success('保存成功'); drawer.value = false; load()
  } finally { saving.value = false }
}
function openTransition(row) {
  trans.row = row
  trans.target = nextStatuses(row.status)[0] || ''
  trans.reason = ''
  transDlg.value = true
}
async function doTransition() {
  if (!trans.target) { ElMessage.warning('请选择目标状态'); return }
  if (!trans.reason || trans.reason.trim().length < 5) { ElMessage.warning('请填写不少于 5 个字的流转原因'); return }
  saving.value = true
  try {
    await http.post(`/api/aquaculture-water/${trans.row.id}/transition`, { target: trans.target, reason: trans.reason.trim() })
    ElMessage.success('状态已流转'); transDlg.value = false; load()
  } finally { saving.value = false }
}

onMounted(async () => {
  load()
  ponds.value = await http.get('/api/pond/options')
  const u = await http.get('/api/system/user/page', { params: { page: 1, size: 50 } })
  users.value = u.list || []
})
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; padding: 12px 18px; }
</style>
