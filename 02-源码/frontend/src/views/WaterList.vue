<template>
  <div class="page">
    <div class="page-head">
      <h1>水质监测</h1>
      <span class="sub">共 {{ total }} 条检测记录 · 超限记录自动生成预警并纳入风险分层</span>
      <div class="actions">
        <el-button type="primary" :icon="Plus" @click="$router.push('/water/new')">录入水质</el-button>
      </div>
    </div>

    <div class="filters">
      <el-select v-model="q.pondId" placeholder="塘口" clearable filterable>
        <el-option v-for="p in ponds" :key="p.id" :label="`${p.pondNo} ${p.pondName}`" :value="p.id" />
      </el-select>
      <el-select v-model="q.batchId" placeholder="养殖批次" clearable filterable>
        <el-option v-for="b in batches" :key="b.id" :label="b.batchNo" :value="b.id" />
      </el-select>
      <el-select v-model="q.level" placeholder="水质等级" clearable>
        <el-option v-for="(v, k) in WATER_LEVEL" :key="k" :label="v.label" :value="k" />
      </el-select>
      <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 240px" />
      <div class="tail">
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column label="仪表照片" width="86">
          <template #default="{ row }">
            <el-image :src="row.image" :preview-src-list="[row.image]" fit="cover" class="thumb" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column prop="recordNo" label="记录编号" width="152" class-name="nowrap" />
        <el-table-column prop="pondName" label="塘口" width="90" />
        <el-table-column prop="batchNo" label="批次" width="128" />
        <el-table-column prop="variety" label="品种" width="106" />
        <el-table-column label="检测时间" width="150" class-name="nowrap"><template #default="{ row }">{{ formatTime(row.checkTime) }}</template></el-table-column>
        <el-table-column label="水温" width="76" align="right"><template #default="{ row }"><span class="num">{{ row.waterTemp }}</span></template></el-table-column>
        <el-table-column label="pH" width="66" align="right"><template #default="{ row }"><span class="num">{{ row.ph }}</span></template></el-table-column>
        <el-table-column label="溶氧" width="76" align="right"><template #default="{ row }"><span class="num">{{ row.dissolvedOxygen }}</span></template></el-table-column>
        <el-table-column label="氨氮" width="76" align="right"><template #default="{ row }"><span class="num">{{ row.ammonia }}</span></template></el-table-column>
        <el-table-column label="亚硝酸盐" width="96" align="right"><template #default="{ row }"><span class="num">{{ row.nitrite }}</span></template></el-table-column>
        <el-table-column label="等级" width="100">
          <template #default="{ row }"><el-tag size="small" :type="statusOf(WATER_LEVEL, row.level).type">{{ statusOf(WATER_LEVEL, row.level).label }}</el-tag></template>
        </el-table-column>
        <el-table-column label="关联预警" width="106">
          <template #default="{ row }">
            <a v-if="row.warningId" @click="$router.push(`/warnings/${row.warningId}`)">{{ statusOf(WARNING_STATUS, row.warningStatus).label }}</a>
            <span v-else class="tip">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="testerName" label="检测人" width="90" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="detail(row)">详情</el-button>
            <el-button link type="primary" @click="evaluate(row)">风险分层</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="当前筛选条件下无水质记录，清空筛选或录入一条检测" :image-size="70" /></template>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="load" @current-change="load" />
      </div>
    </div>

    <el-drawer v-model="dlg" title="水质检测详情" size="600px" destroy-on-close>
      <template v-if="cur">
        <div class="flex" style="margin-bottom: 14px">
          <b>{{ cur.record.recordNo }}</b>
          <el-tag size="small" :type="statusOf(WATER_LEVEL, cur.record.level).type">{{ statusOf(WATER_LEVEL, cur.record.level).label }}</el-tag>
          <span class="tip right">{{ formatTime(cur.record.checkTime) }} · {{ cur.record.testerName }}</span>
        </div>
        <el-image :src="cur.record.image" :preview-src-list="[cur.record.image]" fit="cover" class="big-img" preview-teleported />
        <table class="param">
          <tbody>
            <tr><th>水温</th><td class="num">{{ cur.record.waterTemp }} ℃</td><th>pH</th><td class="num">{{ cur.record.ph }}</td></tr>
            <tr><th>溶解氧</th><td class="num">{{ cur.record.dissolvedOxygen }} mg/L</td><th>氨氮</th><td class="num">{{ cur.record.ammonia }} mg/L</td></tr>
            <tr><th>亚硝酸盐</th><td class="num">{{ cur.record.nitrite }} mg/L</td><th>透明度</th><td class="num">{{ cur.record.transparency }} cm</td></tr>
            <tr><th>塘口</th><td>{{ cur.pond?.pondName }}</td><th>批次</th><td>{{ cur.batch?.batchNo }}</td></tr>
          </tbody>
        </table>
        <div class="block-t mt16">现场备注</div>
        <div class="tip">{{ cur.record.remark || '—' }}</div>
        <div class="block-t mt16">风险分层留痕（{{ (cur.scores || []).length }} 次）</div>
        <div v-for="s in cur.scores" :key="s.id" class="score-row">
          <div class="flex">
            <el-tag size="small" :type="RISK_LEVEL[s.riskLevel]?.type">{{ RISK_LEVEL[s.riskLevel]?.label }}</el-tag>
            <span class="num">{{ s.totalScore }} 分</span>
            <span class="tip">规则 v{{ s.ruleVersion }} · {{ formatTime(s.createTime) }}</span>
            <el-tag v-if="s.manualLevel" size="small" type="warning" class="right">人工修正为 {{ RISK_LEVEL[s.manualLevel]?.label }}</el-tag>
          </div>
          <div class="tip mt8">{{ s.advice }}</div>
          <div v-if="s.manualReason" class="why mt8">修正原因：{{ s.manualReason }}（{{ s.manualBy }} · {{ formatTime(s.manualTime) }}）</div>
        </div>
        <el-empty v-if="!(cur.scores || []).length" description="尚无计算记录" :image-size="60" />
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Plus, Search, RefreshLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { formatTime } from '../utils/format'
import { RISK_LEVEL, WATER_LEVEL, WARNING_STATUS, statusOf } from '../utils/status'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const ponds = ref([])
const batches = ref([])
const range = ref([])
const dlg = ref(false)
const cur = ref(null)
const q = reactive({ page: 1, size: 10, pondId: null, batchId: null, level: '' })

async function load() {
  loading.value = true
  try {
    const d = await http.get('/api/water/page', {
      params: {
        ...q,
        pondId: q.pondId || undefined,
        batchId: q.batchId || undefined,
        level: q.level || undefined,
        start: range.value?.[0],
        end: range.value?.[1]
      }
    })
    rows.value = d.list || []
    total.value = d.total || 0
  } finally { loading.value = false }
}
function search() { q.page = 1; load() }
function reset() { Object.assign(q, { page: 1, pondId: null, batchId: null, level: '' }); range.value = []; load() }

async function detail(row) {
  cur.value = await http.get(`/api/water/${row.id}`)
  dlg.value = true
}
async function evaluate(row) {
  const r = await http.post(`/api/water/${row.id}/evaluate`)
  ElMessage.success(`已完成风险分层：${RISK_LEVEL[r.riskLevel]?.label}（${r.totalScore} 分）`)
  detail(row)
}

onMounted(async () => {
  load()
  ponds.value = await http.get('/api/pond/options')
  batches.value = await http.get('/api/aquaculture-water/options')
})
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; padding: 12px 18px; }
.big-img { width: 100%; height: 190px; border-radius: 6px; border: 1px solid var(--c-border); }
.param { width: 100%; border-collapse: collapse; margin-top: 14px; font-size: 12.5px; }
.param th { text-align: left; width: 90px; background: #F6F8F7; padding: 8px 10px; color: #5B7176; font-weight: 500; }
.param td { padding: 8px 10px; border-bottom: 1px solid var(--c-border-l); }
.score-row { padding: 10px 12px; border: 1px solid var(--c-border-l); border-radius: 6px; margin-bottom: 8px; }
.why { font-size: 12px; color: var(--c-text-2); background: #F6F8F7; border-left: 2px solid var(--c-border); padding: 6px 10px; }
</style>
