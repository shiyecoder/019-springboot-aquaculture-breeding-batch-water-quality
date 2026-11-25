<template>
  <div class="page">
    <div class="page-head">
      <h1>风险预警中心</h1>
      <span class="sub">共 {{ total }} 条预警 · 待处置 → 处置中 → 待复测 → 已关闭</span>
    </div>

    <div class="filters">
      <el-select v-model="q.status" placeholder="预警状态" clearable>
        <el-option v-for="(v, k) in WARNING_STATUS" :key="k" :label="v.label" :value="k" />
      </el-select>
      <el-select v-model="q.level" placeholder="风险等级" clearable>
        <el-option v-for="(v, k) in RISK_LEVEL" :key="k" :label="v.label" :value="k" />
      </el-select>
      <el-select v-model="q.warnType" placeholder="预警类型" clearable>
        <el-option v-for="(v, k) in WARN_TYPE" :key="k" :label="v" :value="k" />
      </el-select>
      <el-select v-model="q.pondId" placeholder="塘口" clearable filterable>
        <el-option v-for="p in ponds" :key="p.id" :label="p.pondName" :value="p.id" />
      </el-select>
      <div class="tail">
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
      </div>
    </div>

    <div class="quick">
      <span class="lb">快捷筛选</span>
      <el-check-tag :checked="q.status === ''" @change="quick('')">全部</el-check-tag>
      <el-check-tag :checked="q.status === 'PENDING'" @change="quick('PENDING')">待处置 {{ counts.PENDING || 0 }}</el-check-tag>
      <el-check-tag :checked="q.status === 'PROCESSING'" @change="quick('PROCESSING')">处置中 {{ counts.PROCESSING || 0 }}</el-check-tag>
      <el-check-tag :checked="q.status === 'RETEST'" @change="quick('RETEST')">待复测 {{ counts.RETEST || 0 }}</el-check-tag>
      <el-check-tag :checked="q.status === 'CLOSED'" @change="quick('CLOSED')">已关闭 {{ counts.CLOSED || 0 }}</el-check-tag>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="warningNo" label="预警编号" width="152" class-name="nowrap">
          <template #default="{ row }"><a @click="go(row)">{{ row.warningNo }}</a></template>
        </el-table-column>
        <el-table-column prop="pondName" label="塘口" width="90" />
        <el-table-column prop="batchNo" label="批次" width="130" />
        <el-table-column label="类型" width="94"><template #default="{ row }">{{ WARN_TYPE[row.warnType] || row.warnType }}</template></el-table-column>
        <el-table-column label="等级" width="92">
          <template #default="{ row }"><el-tag size="small" :type="RISK_LEVEL[row.level]?.type">{{ RISK_LEVEL[row.level]?.label }}</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }"><el-tag size="small" :type="statusOf(WARNING_STATUS, row.status).type">{{ statusOf(WARNING_STATUS, row.status).label }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="content" label="预警内容" min-width="320" show-overflow-tooltip />
        <el-table-column label="检测时间" width="150" class-name="nowrap"><template #default="{ row }">{{ formatTime(row.checkTime) }}</template></el-table-column>
        <el-table-column label="生成时间" width="150" class-name="nowrap"><template #default="{ row }">{{ formatTime(row.createTime) }}</template></el-table-column>
        <el-table-column prop="handlerName" label="处置人" width="92"><template #default="{ row }">{{ row.handlerName || '—' }}</template></el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="go(row)">{{ row.status === 'CLOSED' ? '查看' : '处置' }}</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="当前筛选条件下无预警，说明近期水质稳定" :image-size="70" /></template>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="load" @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import http from '../api/http'
import { formatTime } from '../utils/format'
import { RISK_LEVEL, WARNING_STATUS, WARN_TYPE, statusOf } from '../utils/status'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const counts = ref({})
const ponds = ref([])
const loading = ref(false)
const q = reactive({ page: 1, size: 10, status: '', level: '', warnType: '', pondId: null })

async function load() {
  loading.value = true
  try {
    const d = await http.get('/api/water/warning/page', {
      params: { ...q, status: q.status || undefined, level: q.level || undefined, warnType: q.warnType || undefined, pondId: q.pondId || undefined }
    })
    rows.value = d.list || []
    total.value = d.total || 0
  } finally { loading.value = false }
  // 各状态数量：分别取 total，用于快捷标签
  const keys = ['PENDING', 'PROCESSING', 'RETEST', 'CLOSED']
  const res = await Promise.all(keys.map((s) => http.get('/api/water/warning/page', { params: { page: 1, size: 1, status: s } })))
  counts.value = keys.reduce((acc, k, i) => ({ ...acc, [k]: res[i].total }), {})
}
function search() { q.page = 1; load() }
function reset() { Object.assign(q, { page: 1, status: '', level: '', warnType: '', pondId: null }); load() }
function quick(s) { q.status = s; search() }
function go(row) { router.push(`/warnings/${row.id}`) }

onMounted(async () => {
  load()
  ponds.value = await http.get('/api/pond/options')
})
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; padding: 12px 18px; }
.quick { display: flex; align-items: center; gap: 8px; margin: -4px 0 12px; }
.quick .lb { font-size: 12px; color: var(--c-text-2); margin-right: 4px; }
</style>
