<template>
  <div class="page">
    <div class="page-head">
      <h1>投喂计划</h1>
      <span class="sub">按日期、时段生成任务，实际用量不得超过领料量，超标需技术员复核</span>
      <div class="actions">
        <el-button :icon="Plus" @click="openPlan">新建计划</el-button>
      </div>
    </div>

    <div class="filters">
      <el-select v-model="q.batchId" placeholder="养殖批次" clearable filterable>
        <el-option v-for="b in batches" :key="b.id" :label="`${b.batchNo}（${b.variety}）`" :value="b.id" />
      </el-select>
      <el-select v-model="q.status" placeholder="任务状态" clearable>
        <el-option v-for="(v, k) in FEEDING_STATUS" :key="k" :label="v.label" :value="k" />
      </el-select>
      <el-date-picker v-model="q.planDate" type="date" value-format="YYYY-MM-DD" placeholder="计划日期" class="w140" />
      <div class="tail">
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
      </div>
    </div>

    <div class="card">
      <el-tabs v-model="tab" class="tabs">
        <el-tab-pane :label="`投喂任务 (${total})`" name="plan">
          <el-table :data="rows" v-loading="loading" size="default">
            <el-table-column prop="planNo" label="计划编号" width="146" class-name="nowrap" />
            <el-table-column prop="batchNo" label="批次" width="130" />
            <el-table-column prop="pondName" label="塘口" width="92" />
            <el-table-column prop="feedName" label="饲料" min-width="150" />
            <el-table-column label="计划量(kg)" width="106" align="right"><template #default="{ row }"><span class="num">{{ row.feedQty }}</span></template></el-table-column>
            <el-table-column label="实际量(kg)" width="106" align="right"><template #default="{ row }"><span class="num">{{ row.actualQty ?? '—' }}</span></template></el-table-column>
            <el-table-column prop="planDate" label="计划日期" width="108" class-name="nowrap" />
            <el-table-column label="时段" width="80"><template #default="{ row }">{{ PERIOD[row.period] || row.period }}</template></el-table-column>
            <el-table-column prop="executorName" label="执行人" width="92" />
            <el-table-column label="状态" width="92">
              <template #default="{ row }"><el-tag size="small" :type="statusOf(FEEDING_STATUS, row.status).type">{{ statusOf(FEEDING_STATUS, row.status).label }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status !== 'DONE'" link type="primary" @click="openExec(row)">执行投喂</el-button>
                <span v-else class="tip">已完成</span>
              </template>
            </el-table-column>
            <template #empty><el-empty description="当前筛选条件下无投喂任务，调整日期或新建计划" :image-size="64" /></template>
          </el-table>
          <div class="pager">
            <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="load" @current-change="load" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="投喂记录" name="record">
          <el-table :data="records" size="small" max-height="440">
            <el-table-column prop="planId" label="计划ID" width="86" />
            <el-table-column prop="actualQty" label="实际用量(kg)" width="120" align="right" />
            <el-table-column prop="executorName" label="执行人" width="110" />
            <el-table-column label="执行时间" width="170"><template #default="{ row }">{{ formatTime(row.executeTime || row.createTime) }}</template></el-table-column>
            <el-table-column prop="remark" label="现场情况" min-width="220" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="planDlg" title="新建投喂计划" width="480px" destroy-on-close>
      <el-form :model="plan" label-width="104px" label-position="left">
        <el-form-item label="养殖批次" required>
          <el-select v-model="plan.batchId" style="width: 230px" filterable placeholder="选择批次">
            <el-option v-for="b in batches" :key="b.id" :label="`${b.batchNo}（${b.variety}）`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划日期" required>
          <el-date-picker v-model="plan.planDate" type="date" value-format="YYYY-MM-DD" style="width: 230px" />
        </el-form-item>
        <el-form-item label="投喂时段" required>
          <el-select v-model="plan.period" style="width: 230px">
            <el-option v-for="(v, k) in PERIOD" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="饲料名称" required><el-input v-model="plan.feedName" maxlength="40" /></el-form-item>
        <el-form-item label="计划量(kg)" required><el-input-number v-model="plan.feedQty" :min="0.5" :precision="1" controls-position="right" /></el-form-item>
        <el-form-item label="执行人">
          <el-select v-model="plan.executorId" style="width: 230px" clearable placeholder="选择养殖员">
            <el-option v-for="u in feeders" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="plan.remark" type="textarea" :rows="2" maxlength="120" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePlan">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="execDlg" title="执行投喂" width="440px" destroy-on-close>
      <div class="notice" v-if="cur">{{ cur.planNo }} · {{ cur.pondName }} · 计划 {{ cur.feedQty }} kg（不得超过领料量）</div>
      <el-form label-width="100px" label-position="left" class="mt16">
        <el-form-item label="实际用量(kg)" required>
          <el-input-number v-model="exec.actualQty" :min="0" :precision="1" :step="0.5" controls-position="right" />
        </el-form-item>
        <el-form-item label="现场情况"><el-input v-model="exec.remark" type="textarea" :rows="2" placeholder="如：摄食正常 / 阴雨减半" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="execDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doExec">确认执行</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, RefreshLeft } from '@element-plus/icons-vue'
import http from '../api/http'
import { formatTime } from '../utils/format'
import { FEEDING_STATUS, PERIOD, statusOf } from '../utils/status'

const tab = ref('plan')
const rows = ref([])
const records = ref([])
const batches = ref([])
const feeders = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const planDlg = ref(false)
const execDlg = ref(false)
const cur = ref(null)
const q = reactive({ page: 1, size: 10, batchId: null, status: '', planDate: '' })
const plan = reactive({ batchId: null, planDate: '', period: 'AM', feedName: '淡水鱼配合饲料', feedQty: 30, executorId: null, remark: '' })
const exec = reactive({ actualQty: 0, remark: '' })

async function load() {
  loading.value = true
  try {
    const d = await http.get('/api/feeding/plan/page', { params: { ...q, batchId: q.batchId || undefined, status: q.status || undefined, planDate: q.planDate || undefined } })
    rows.value = d.list || []
    total.value = d.total || 0
  } finally { loading.value = false }
  const r = await http.get('/api/feeding/record/page', { params: { page: 1, size: 20 } })
  records.value = r.list || []
}
function search() { q.page = 1; load() }
function reset() { Object.assign(q, { page: 1, batchId: null, status: '', planDate: '' }); load() }

function openPlan() {
  Object.assign(plan, { batchId: q.batchId, planDate: new Date().toISOString().slice(0, 10), period: 'AM', feedName: '淡水鱼配合饲料', feedQty: 30, executorId: null, remark: '' })
  planDlg.value = true
}
async function savePlan() {
  if (!plan.batchId || !plan.planDate) { ElMessage.warning('请选择批次与计划日期'); return }
  saving.value = true
  try {
    await http.post('/api/feeding/plan', plan)
    ElMessage.success('计划已生成'); planDlg.value = false; load()
  } finally { saving.value = false }
}
function openExec(row) {
  cur.value = row
  exec.actualQty = Number(row.feedQty)
  exec.remark = ''
  execDlg.value = true
}
async function doExec() {
  if (exec.actualQty > Number(cur.value.feedQty)) { ElMessage.warning('实际用量不得超过计划领料量，请核实'); return }
  saving.value = true
  try {
    await http.post(`/api/feeding/plan/${cur.value.id}/execute`, exec)
    ElMessage.success('投喂已登记'); execDlg.value = false; load()
  } finally { saving.value = false }
}

onMounted(async () => {
  load()
  batches.value = await http.get('/api/aquaculture-water/options')
  const u = await http.get('/api/system/user/page', { params: { page: 1, size: 50, roleId: 4 } })
  feeders.value = u.list || []
})
</script>

<style scoped>
.tabs :deep(.el-tabs__header) { padding: 0 18px; margin-bottom: 0; }
.tabs :deep(.el-tabs__content) { padding: 4px 18px 0; }
.pager { display: flex; justify-content: flex-end; padding: 12px 0 14px; }
</style>
