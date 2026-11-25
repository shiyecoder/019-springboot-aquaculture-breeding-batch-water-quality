<template>
  <div class="portal-page">
    <div class="hero">
      <div>
        <h2>{{ greeting }}，{{ user.realName }}</h2>
        <p>{{ today }} · {{ user.orgName }} · 今日待办 {{ todoCount }} 项</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" plain @click="$router.push('/water/new')">录入水质</el-button>
        <el-button plain @click="$router.push('/feeding')">投喂任务</el-button>
      </div>
    </div>

    <div class="grid3 mt16">
      <div class="kpi"><div class="lb">今日待执行投喂</div><div class="v">{{ plans.length }}<small>条</small></div><div class="df">按塘口逐个完成并回填实际用量</div></div>
      <div class="kpi"><div class="lb">待处置预警</div><div class="v" style="color: var(--c-danger)">{{ warnings.length }}<small>条</small></div><div class="df">优先处理高风险，2 小时内复测</div></div>
      <div class="kpi"><div class="lb">最近一次检测</div><div class="v" style="font-size: 18px">{{ latests[0] ? formatTime(latests[0].checkTime).slice(5, 16) : '—' }}</div><div class="df">{{ latests[0] ? latests[0].pondName + ' · ' + latests[0].recordNo : '暂无记录' }}</div></div>
    </div>

    <div class="grid2 mt16">
      <div class="card">
        <div class="card-h"><h3>今日投喂任务</h3><span class="hint">按提交顺序</span></div>
        <div class="card-b">
          <div v-for="p in plans" :key="p.id" class="task">
            <div class="flex">
              <b>{{ p.pondName }} · {{ p.batchNo }}</b>
              <el-tag size="small" type="warning" class="right">{{ PERIOD[p.period] }}</el-tag>
            </div>
            <div class="tip mt8">{{ p.feedName }} · 计划 {{ p.feedQty }} kg</div>
            <div class="flex mt8">
              <span class="tip">执行人 {{ p.executorName }}</span>
              <el-button size="small" type="primary" class="right" @click="exec(p)">填写实际用量</el-button>
            </div>
          </div>
          <el-empty v-if="!plans.length" description="今日投喂任务已全部完成" :image-size="64" />
        </div>
      </div>

      <div class="card">
        <div class="card-h"><h3>待处置预警</h3><span class="hint">点击进入处置</span></div>
        <div class="card-b">
          <div v-for="w in warnings" :key="w.id" class="task" @click="$router.push(`/warnings/${w.id}`)">
            <div class="flex">
              <b>{{ w.pondName }}</b>
              <el-tag size="small" :type="RISK_LEVEL[w.level]?.type" class="right">{{ RISK_LEVEL[w.level]?.label }}</el-tag>
            </div>
            <div class="tip mt8">{{ w.content }}</div>
          </div>
          <el-empty v-if="!warnings.length" description="暂无待处置预警，水质稳定" :image-size="64" />
        </div>
      </div>
    </div>

    <div class="card mt16">
      <div class="card-h"><h3>我的最近检测</h3><div class="r"><el-button link type="primary" @click="$router.push('/water')">全部记录</el-button></div></div>
      <el-table :data="latests" size="small">
        <el-table-column prop="recordNo" label="记录编号" width="150" />
        <el-table-column prop="pondName" label="塘口" width="90" />
        <el-table-column prop="batchNo" label="批次" width="130" />
        <el-table-column label="检测时间" width="150"><template #default="{ row }">{{ formatTime(row.checkTime) }}</template></el-table-column>
        <el-table-column prop="dissolvedOxygen" label="溶氧" width="80" align="right" />
        <el-table-column prop="ph" label="pH" width="70" align="right" />
        <el-table-column prop="ammonia" label="氨氮" width="80" align="right" />
        <el-table-column label="等级" width="100">
          <template #default="{ row }"><el-tag size="small" :type="statusOf(WATER_LEVEL, row.level).type">{{ statusOf(WATER_LEVEL, row.level).label }}</el-tag></template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dlg" title="填写实际投喂量" width="420px" destroy-on-close>
      <div class="notice" v-if="cur">{{ cur.pondName }} · 计划 {{ cur.feedQty }} kg，实际不得超过计划量</div>
      <el-form label-width="104px" label-position="left" class="mt16">
        <el-form-item label="实际用量(kg)"><el-input-number v-model="actual" :min="0" :precision="1" controls-position="right" /></el-form-item>
        <el-form-item label="现场情况"><el-input v-model="remark" type="textarea" :rows="2" placeholder="如：摄食正常" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doExec">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { formatTime } from '../utils/format'
import { PERIOD, RISK_LEVEL, WATER_LEVEL, statusOf } from '../utils/status'

const user = JSON.parse(localStorage.getItem('aq_user') || '{}')
const plans = ref([])
const warnings = ref([])
const latests = ref([])
const dlg = ref(false)
const saving = ref(false)
const cur = ref(null)
const actual = ref(0)
const remark = ref('')

const today = new Date().toISOString().slice(0, 10)
const greeting = computed(() => {
  const h = new Date().getHours()
  return h < 6 ? '凌晨好' : h < 11 ? '早上好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好'
})
const todoCount = computed(() => plans.value.length + warnings.value.length)

async function load() {
  const p = await http.get('/api/feeding/plan/page', { params: { page: 1, size: 10, status: 'PENDING', planDate: today } })
  plans.value = p.list || []
  const w = await http.get('/api/water/warning/page', { params: { page: 1, size: 6, status: 'PENDING' } })
  warnings.value = w.list || []
  const r = await http.get('/api/water/page', { params: { page: 1, size: 8 } })
  latests.value = r.list || []
}
function exec(p) { cur.value = p; actual.value = Number(p.feedQty); remark.value = ''; dlg.value = true }
async function doExec() {
  if (actual.value > Number(cur.value.feedQty)) { ElMessage.warning('实际用量不得超过计划量'); return }
  saving.value = true
  try {
    await http.post(`/api/feeding/plan/${cur.value.id}/execute`, { actualQty: actual.value, remark: remark.value })
    ElMessage.success('已回填投喂记录'); dlg.value = false; load()
  } finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.portal-page { padding: 24px 32px; max-width: 1400px; margin: 0 auto; }
.hero-actions { margin-left: auto; display: flex; gap: 10px; }
.hero :deep(.el-button) { background: rgba(255,255,255,.12); border-color: rgba(255,255,255,.5); color: #fff; }
.hero :deep(.el-button:hover) { background: rgba(255,255,255,.22); color: #fff; }
.task { padding: 12px 14px; border: 1px solid var(--c-border-l); border-radius: 7px; margin-bottom: 10px; background: #fff; cursor: pointer; }
.task:hover { border-color: #C9DBD6; }
</style>
