<template>
  <div class="page">
    <div class="page-head">
      <h1>分塘与起捕</h1>
      <span class="sub">分塘数量与起捕估算存量不得超过批次可用存量；休药期内禁止创建起捕任务</span>
      <div class="actions">
        <el-button type="primary" :icon="Plus" @click="openHarvest">新建起捕任务</el-button>
        <el-button :icon="Sort" @click="openTransfer">登记分塘</el-button>
      </div>
    </div>

    <div class="card">
      <el-tabs v-model="tab" class="tabs">
        <el-tab-pane :label="`起捕任务 (${total})`" name="harvest">
          <div class="filters inner">
            <el-select v-model="q.batchId" placeholder="养殖批次" clearable filterable>
              <el-option v-for="b in batches" :key="b.id" :label="b.batchNo" :value="b.id" />
            </el-select>
            <el-select v-model="q.status" placeholder="任务状态" clearable>
              <el-option v-for="(v, k) in HARVEST_STATUS" :key="k" :label="v.label" :value="k" />
            </el-select>
            <el-select v-model="q.grade" placeholder="等级" clearable>
              <el-option v-for="(v, k) in GRADE" :key="k" :label="v.label" :value="k" />
            </el-select>
            <div class="tail">
              <el-button type="primary" :icon="Search" @click="search">查询</el-button>
              <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
            </div>
          </div>
          <el-table :data="rows" v-loading="loading">
            <el-table-column label="起捕照片" width="86">
              <template #default="{ row }"><el-image :src="row.image" :preview-src-list="[row.image]" fit="cover" class="thumb" preview-teleported /></template>
            </el-table-column>
            <el-table-column prop="harvestNo" label="起捕编号" width="148" class-name="nowrap" />
            <el-table-column prop="batchNo" label="批次" width="128" />
            <el-table-column prop="variety" label="品种" width="104" />
            <el-table-column prop="pondName" label="塘口" width="90" />
            <el-table-column prop="planDate" label="计划日期" width="108" class-name="nowrap" />
            <el-table-column prop="actualDate" label="实际日期" width="108" class-name="nowrap"><template #default="{ row }">{{ row.actualDate || '—' }}</template></el-table-column>
            <el-table-column label="重量(kg)" width="104" align="right"><template #default="{ row }"><span class="num">{{ Number(row.weight || 0).toFixed(0) }}</span></template></el-table-column>
            <el-table-column label="估算存量(kg)" width="120" align="right"><template #default="{ row }"><span class="num">{{ Number(row.estStock || 0).toFixed(0) }}</span></template></el-table-column>
            <el-table-column label="等级" width="82"><template #default="{ row }">{{ GRADE[row.grade]?.label || row.grade }}</template></el-table-column>
            <el-table-column prop="destination" label="去向" min-width="110" />
            <el-table-column label="损耗率" width="86" align="right"><template #default="{ row }"><span class="num">{{ row.lossRate }}%</span></template></el-table-column>
            <el-table-column label="状态" width="94">
              <template #default="{ row }"><el-tag size="small" :type="statusOf(HARVEST_STATUS, row.status).type">{{ statusOf(HARVEST_STATUS, row.status).label }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="approver" label="审批人" width="90" />
            <el-table-column label="操作" width="136" fixed="right" class-name="nowrap">
              <template #default="{ row }">
                <el-button v-if="row.status !== 'DONE' && row.status !== 'VOID'" link type="primary" @click="openFinish(row)">完成起捕</el-button>
                <el-button v-else link type="primary" @click="openView(row)">查看</el-button>
              </template>
            </el-table-column>
            <template #empty><el-empty description="当前筛选条件下无起捕任务" :image-size="64" /></template>
          </el-table>
          <div class="pager">
            <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="load" @current-change="load" />
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`分塘记录 (${tTotal})`" name="transfer">
          <el-table :data="transfers" size="default">
            <el-table-column prop="transferNo" label="分塘编号" width="152" />
            <el-table-column prop="batchNo" label="批次" width="132" />
            <el-table-column label="转出 → 转入" min-width="200">
              <template #default="{ row }">{{ row.fromPondName }} → {{ row.toPondName }}</template>
            </el-table-column>
            <el-table-column label="数量(kg)" width="110" align="right"><template #default="{ row }"><span class="num">{{ row.qty }}</span></template></el-table-column>
            <el-table-column prop="transferDate" label="分塘日期" width="112" />
            <el-table-column prop="operator" label="操作人" width="96" />
            <el-table-column label="状态" width="92">
              <template #default="{ row }"><el-tag size="small" type="success">{{ row.status === 'DONE' ? '已完成' : row.status }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
            <template #empty><el-empty description="暂无分塘记录" :image-size="64" /></template>
          </el-table>
          <div class="pager">
            <el-pagination v-model:current-page="tq.page" v-model:page-size="tq.size" :total="tTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="loadTransfer" @current-change="loadTransfer" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="harvestDlg" title="新建起捕任务" width="520px" destroy-on-close>
      <div class="notice">休药期内的批次不允许创建起捕任务，需场长先作废用药记录。</div>
      <el-form :model="form" label-width="110px" label-position="left" class="mt16">
        <el-form-item label="养殖批次" required>
          <el-select v-model="form.batchId" style="width: 260px" filterable placeholder="选择批次" @change="onBatch">
            <el-option v-for="b in batches" :key="b.id" :label="`${b.batchNo}（存量 ${Number(b.currentStock || 0).toFixed(0)} kg）`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划日期" required>
          <el-date-picker v-model="form.planDate" type="date" value-format="YYYY-MM-DD" style="width: 260px" />
        </el-form-item>
        <el-form-item label="估算存量(kg)">
          <el-input-number v-model="form.estStock" :min="0" :precision="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="计划重量(kg)">
          <el-input-number v-model="form.weight" :min="0" :precision="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="预计等级">
          <el-select v-model="form.grade" style="width: 150px">
            <el-option v-for="(v, k) in GRADE" :key="k" :label="v.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="去向">
          <el-select v-model="form.destination" style="width: 200px" allow-create filterable>
            <el-option v-for="d in ['批发市场', '商超直供', '餐饮直配', '活鱼运输商']" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="起捕照片" required><ImageUpload v-model="form.image" type="harvest" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" maxlength="120" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="harvestDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doHarvest">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="finishDlg" title="完成起捕" width="480px" destroy-on-close>
      <div class="notice" v-if="cur">{{ cur.harvestNo }} · {{ cur.batchNo }} · 可用存量需 ≥ 实际起捕重量</div>
      <el-form :model="fin" label-width="110px" label-position="left" class="mt16">
        <el-form-item label="实际日期" required>
          <el-date-picker v-model="fin.actualDate" type="date" value-format="YYYY-MM-DD" style="width: 230px" />
        </el-form-item>
        <el-form-item label="实际重量(kg)" required>
          <el-input-number v-model="fin.weight" :min="1" :precision="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="fin.grade" style="width: 150px">
            <el-option v-for="(v, k) in GRADE" :key="k" :label="v.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="去向"><el-input v-model="fin.destination" maxlength="40" /></el-form-item>
        <el-form-item label="损耗率(%)"><el-input-number v-model="fin.lossRate" :min="0" :max="30" :precision="1" controls-position="right" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="finishDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doFinish">确认完成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="transferDlg" title="登记分塘" width="480px" destroy-on-close>
      <el-form :model="tf" label-width="110px" label-position="left">
        <el-form-item label="养殖批次" required>
          <el-select v-model="tf.batchId" style="width: 240px" filterable placeholder="选择批次">
            <el-option v-for="b in batches" :key="b.id" :label="`${b.batchNo}（${b.pondName}）`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标塘口" required>
          <el-select v-model="tf.toPondId" style="width: 240px" filterable placeholder="选择塘口">
            <el-option v-for="p in ponds" :key="p.id" :label="`${p.pondNo} ${p.pondName}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分塘数量(kg)" required>
          <el-input-number v-model="tf.qty" :min="1" :precision="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="分塘日期" required>
          <el-date-picker v-model="tf.transferDate" type="date" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="tf.remark" type="textarea" :rows="2" maxlength="120" placeholder="如：规格分化，分塘降低密度" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doTransfer">提交</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="viewDlg" title="起捕详情" size="440px" destroy-on-close>
      <template v-if="cur">
        <el-image :src="cur.image" :preview-src-list="[cur.image]" fit="cover" class="dimg" preview-teleported />
        <dl class="detail-dl">
          <dt>起捕编号</dt><dd>{{ cur.harvestNo }}</dd>
          <dt>批次</dt><dd>{{ cur.batchNo }} · {{ cur.variety }} · {{ cur.pondName }}</dd>
          <dt>计划/实际</dt><dd>{{ cur.planDate }} / {{ cur.actualDate || '—' }}</dd>
          <dt>重量</dt><dd class="num">{{ cur.weight }} kg</dd>
          <dt>等级/去向</dt><dd>{{ GRADE[cur.grade]?.label || cur.grade }} · {{ cur.destination }}</dd>
          <dt>损耗率</dt><dd class="num">{{ cur.lossRate }}%</dd>
          <dt>状态</dt><dd>{{ statusOf(HARVEST_STATUS, cur.status).label }} · 审批人 {{ cur.approver || '—' }}</dd>
          <dt>备注</dt><dd>{{ cur.remark || '—' }}</dd>
        </dl>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, RefreshLeft, Sort } from '@element-plus/icons-vue'
import http from '../api/http'
import ImageUpload from '../components/ImageUpload.vue'
import { GRADE, HARVEST_STATUS, statusOf } from '../utils/status'

const tab = ref('harvest')
const rows = ref([])
const transfers = ref([])
const batches = ref([])
const ponds = ref([])
const total = ref(0)
const tTotal = ref(0)
const loading = ref(false)
const saving = ref(false)
const harvestDlg = ref(false)
const finishDlg = ref(false)
const transferDlg = ref(false)
const viewDlg = ref(false)
const cur = ref(null)
const q = reactive({ page: 1, size: 10, batchId: null, status: '', grade: '' })
const tq = reactive({ page: 1, size: 10, batchId: null })
const form = reactive({ batchId: null, planDate: '', estStock: 0, weight: 0, grade: 'A', destination: '批发市场', image: '', remark: '' })
const fin = reactive({ actualDate: '', weight: 0, grade: 'A', destination: '', lossRate: 3 })
const tf = reactive({ batchId: null, toPondId: null, qty: 100, transferDate: '', remark: '' })

async function load() {
  loading.value = true
  try {
    const d = await http.get('/api/harvest/page', { params: { ...q, batchId: q.batchId || undefined, status: q.status || undefined, grade: q.grade || undefined } })
    rows.value = d.list || []
    total.value = d.total || 0
  } finally { loading.value = false }
  loadTransfer()
}
async function loadTransfer() {
  const d = await http.get('/api/harvest/transfer/page', { params: { ...tq, batchId: tq.batchId || undefined } })
  transfers.value = d.list || []
  tTotal.value = d.total || 0
}
function search() { q.page = 1; load() }
function reset() { Object.assign(q, { page: 1, batchId: null, status: '', grade: '' }); load() }

function openHarvest() {
  Object.assign(form, { batchId: null, planDate: new Date().toISOString().slice(0, 10), estStock: 0, weight: 0, grade: 'A', destination: '批发市场', image: '', remark: '' })
  harvestDlg.value = true
}
function onBatch(id) {
  const b = batches.value.find((x) => x.id === id)
  if (b) form.estStock = Number(b.currentStock || 0)
}
async function doHarvest() {
  if (!form.batchId || !form.planDate) { ElMessage.warning('请选择批次与计划日期'); return }
  if (!form.image) { ElMessage.warning('请上传起捕现场照片'); return }
  saving.value = true
  try {
    await http.post('/api/harvest', form)
    ElMessage.success('起捕任务已创建'); harvestDlg.value = false; load()
  } finally { saving.value = false }
}
function openFinish(row) {
  cur.value = row
  Object.assign(fin, { actualDate: new Date().toISOString().slice(0, 10), weight: Number(row.weight || 0), grade: row.grade || 'A', destination: row.destination || '', lossRate: Number(row.lossRate || 3) })
  finishDlg.value = true
}
async function doFinish() {
  saving.value = true
  try {
    await http.post(`/api/harvest/${cur.value.id}/finish`, fin)
    ElMessage.success('起捕已完成，批次存量已扣减')
    finishDlg.value = false; load()
  } finally { saving.value = false }
}
function openTransfer() {
  Object.assign(tf, { batchId: null, toPondId: null, qty: 100, transferDate: new Date().toISOString().slice(0, 10), remark: '' })
  transferDlg.value = true
}
async function doTransfer() {
  if (!tf.batchId || !tf.toPondId) { ElMessage.warning('请选择批次与目标塘口'); return }
  saving.value = true
  try {
    await http.post('/api/harvest/transfer', tf)
    ElMessage.success('分塘已登记'); transferDlg.value = false; load()
  } finally { saving.value = false }
}
function openView(row) { cur.value = row; viewDlg.value = true }

onMounted(async () => {
  load()
  batches.value = await http.get('/api/aquaculture-water/options')
  ponds.value = await http.get('/api/pond/options')
})
</script>

<style scoped>
.tabs :deep(.el-tabs__header) { padding: 0 18px; margin-bottom: 0; }
.tabs :deep(.el-tabs__content) { padding: 0 18px; }
.filters.inner { border: none; padding: 12px 0 4px; margin-bottom: 6px; }
.pager { display: flex; justify-content: flex-end; padding: 12px 0 14px; }
.dimg { width: 100%; height: 180px; border-radius: 6px; margin-bottom: 14px; border: 1px solid var(--c-border); }
</style>
