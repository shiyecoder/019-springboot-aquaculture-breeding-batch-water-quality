<template>
  <div class="page">
    <div class="page-head">
      <h1>病害用药</h1>
      <span class="sub">上报症状与死亡数 → 技术员诊断并审核用药 → 记录休药期，休药期内禁止起捕</span>
      <div class="actions"><el-button type="primary" :icon="Plus" @click="openReport">上报病害</el-button></div>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="病害编号 / 症状" clearable @keyup.enter="search" />
      <el-select v-model="q.batchId" placeholder="养殖批次" clearable filterable>
        <el-option v-for="b in batches" :key="b.id" :label="b.batchNo" :value="b.id" />
      </el-select>
      <el-select v-model="q.status" placeholder="审核状态" clearable>
        <el-option v-for="(v, k) in REVIEW_STATUS" :key="k" :label="v.label" :value="k" />
      </el-select>
      <div class="tail">
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column label="病害图片" width="86">
          <template #default="{ row }">
            <el-image :src="row.image" :preview-src-list="[row.image]" fit="cover" class="thumb" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column prop="diseaseNo" label="病害编号" width="146" class-name="nowrap" />
        <el-table-column prop="batchNo" label="批次" width="126" />
        <el-table-column prop="variety" label="品种" width="104" />
        <el-table-column prop="symptom" label="症状" min-width="180" show-overflow-tooltip />
        <el-table-column label="死亡数(尾)" width="104" align="right"><template #default="{ row }"><span class="num">{{ row.deathCount }}</span></template></el-table-column>
        <el-table-column label="上报时间" width="150" class-name="nowrap"><template #default="{ row }">{{ formatTime(row.reportTime) }}</template></el-table-column>
        <el-table-column prop="reporter" label="上报人" width="88" />
        <el-table-column prop="diagnosis" label="诊断" min-width="130" show-overflow-tooltip />
        <el-table-column label="用药" min-width="150">
          <template #default="{ row }">
            <span v-if="row.drugName">{{ row.drugName }}<span class="tip"> · {{ row.dose }}</span></span>
            <span v-else class="tip">待审核</span>
          </template>
        </el-table-column>
        <el-table-column label="休药期" width="150">
          <template #default="{ row }">
            <span v-if="row.withdrawalDays !== null && row.withdrawalDays !== undefined">
              <span class="num">{{ row.withdrawalDays }} 天</span>
              <div class="tip">至 {{ row.withdrawalEnd }}</div>
            </span>
            <span v-else class="tip">—</span>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="96">
          <template #default="{ row }"><el-tag size="small" :type="statusOf(REVIEW_STATUS, row.reviewStatus).type">{{ statusOf(REVIEW_STATUS, row.reviewStatus).label }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="reviewer" label="审核人" width="88" />
        <el-table-column label="操作" width="150" fixed="right" class-name="nowrap">
          <template #default="{ row }">
            <el-button v-if="row.reviewStatus === 'PENDING'" link type="primary" @click="openReview(row)">审核用药</el-button>
            <el-button v-if="row.reviewStatus === 'APPROVED' && isAdmin" link type="danger" @click="openVoid(row)">作废</el-button>
            <el-button v-if="row.reviewStatus !== 'PENDING'" link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="当前筛选条件下无病害记录，说明近期无异常上报" :image-size="70" /></template>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="load" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="reportDlg" title="上报病害" width="520px" destroy-on-close>
      <el-form :model="form" label-width="100px" label-position="left">
        <el-form-item label="养殖批次" required>
          <el-select v-model="form.batchId" style="width: 260px" filterable placeholder="选择批次">
            <el-option v-for="b in batches" :key="b.id" :label="`${b.batchNo}（${b.pondName}·${b.variety}）`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="症状描述" required>
          <el-input v-model="form.symptom" type="textarea" :rows="2" maxlength="120" placeholder="如：肝脏发白、游动缓慢、摄食下降" />
        </el-form-item>
        <el-form-item label="死亡数(尾)" required>
          <el-input-number v-model="form.deathCount" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="现场图片" required>
          <ImageUpload v-model="form.image" type="disease" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doReport">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewDlg" title="用药审核" width="520px" destroy-on-close>
      <div class="notice" v-if="cur">{{ cur.diseaseNo }} · {{ cur.batchNo }} · 死亡 {{ cur.deathCount }} 尾 · {{ cur.symptom }}</div>
      <el-form :model="rv" label-width="100px" label-position="left" class="mt16">
        <el-form-item label="诊断结论" required><el-input v-model="rv.diagnosis" maxlength="60" placeholder="如：细菌性败血症" /></el-form-item>
        <el-form-item label="审核结论" required>
          <el-radio-group v-model="rv.status">
            <el-radio value="APPROVED">通过</el-radio>
            <el-radio value="REJECTED">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="rv.status === 'APPROVED'">
          <el-form-item label="用药品名" required><el-input v-model="rv.drugName" maxlength="40" /></el-form-item>
          <el-form-item label="剂量"><el-input v-model="rv.dose" maxlength="40" placeholder="如 0.15g/kg 饲料" /></el-form-item>
          <el-form-item label="休药期(天)" required><el-input-number v-model="rv.withdrawalDays" :min="0" :max="60" controls-position="right" /></el-form-item>
        </template>
        <el-form-item label="审核意见">
          <el-input v-model="rv.note" type="textarea" :rows="2" maxlength="120" placeholder="驳回时必须填写不少于 5 个字的原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doReview">提交审核</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="voidDlg" title="作废用药记录" width="440px" destroy-on-close>
      <div class="notice">作废后该休药期限制解除，需场长填写原因，操作全程留痕。</div>
      <el-form label-width="80px" label-position="left" class="mt16">
        <el-form-item label="作废原因" required><el-input v-model="voidReason" type="textarea" :rows="3" maxlength="120" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="voidDlg = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="doVoid">确认作废</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailDlg" title="病害记录详情" size="480px" destroy-on-close>
      <template v-if="cur">
        <el-image :src="cur.image" :preview-src-list="[cur.image]" fit="cover" class="dimg" preview-teleported />
        <dl class="detail-dl">
          <dt>病害编号</dt><dd>{{ cur.diseaseNo }}</dd>
          <dt>批次</dt><dd>{{ cur.batchNo }} · {{ cur.variety }} · {{ cur.pondName }}</dd>
          <dt>症状</dt><dd>{{ cur.symptom }}</dd>
          <dt>死亡数</dt><dd class="num">{{ cur.deathCount }} 尾</dd>
          <dt>上报</dt><dd>{{ cur.reporter }} · {{ formatTime(cur.reportTime) }}</dd>
          <dt>诊断</dt><dd>{{ cur.diagnosis || '—' }}</dd>
          <dt>用药</dt><dd>{{ cur.drugName || '—' }} {{ cur.dose || '' }}</dd>
          <dt>休药期</dt><dd>{{ cur.withdrawalDays ?? '—' }} 天（至 {{ cur.withdrawalEnd || '—' }}）</dd>
          <dt>审核</dt><dd>{{ statusOf(REVIEW_STATUS, cur.reviewStatus).label }} · {{ cur.reviewer || '—' }} · {{ formatTime(cur.reviewTime) }}</dd>
          <dt>审核意见</dt><dd>{{ cur.reviewNote || '—' }}</dd>
        </dl>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, RefreshLeft } from '@element-plus/icons-vue'
import http from '../api/http'
import ImageUpload from '../components/ImageUpload.vue'
import { formatTime } from '../utils/format'
import { REVIEW_STATUS, statusOf } from '../utils/status'

const rows = ref([])
const batches = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const reportDlg = ref(false)
const reviewDlg = ref(false)
const voidDlg = ref(false)
const detailDlg = ref(false)
const cur = ref(null)
const voidReason = ref('')
const q = reactive({ page: 1, size: 10, keyword: '', batchId: null, status: '' })
const form = reactive({ batchId: null, symptom: '', deathCount: 0, image: '' })
const rv = reactive({ status: 'APPROVED', diagnosis: '', drugName: '', dose: '', withdrawalDays: 7, note: '' })

const isAdmin = computed(() => (JSON.parse(localStorage.getItem('aq_user') || '{}').roleCode || '') === 'ADMIN')

async function load() {
  loading.value = true
  try {
    const d = await http.get('/api/disease/page', { params: { ...q, keyword: q.keyword || undefined, batchId: q.batchId || undefined, status: q.status || undefined } })
    rows.value = d.list || []
    total.value = d.total || 0
  } finally { loading.value = false }
}
function search() { q.page = 1; load() }
function reset() { Object.assign(q, { page: 1, keyword: '', batchId: null, status: '' }); load() }

function openReport() {
  Object.assign(form, { batchId: null, symptom: '', deathCount: 0, image: '' })
  reportDlg.value = true
}
async function doReport() {
  if (!form.batchId || !form.symptom) { ElMessage.warning('请选择批次并填写症状'); return }
  if (!form.image) { ElMessage.warning('请上传现场图片'); return }
  saving.value = true
  try {
    await http.post('/api/disease', form)
    ElMessage.success('已上报，等待技术员审核用药')
    reportDlg.value = false; load()
  } finally { saving.value = false }
}
function openReview(row) {
  cur.value = row
  Object.assign(rv, { status: 'APPROVED', diagnosis: row.diagnosis || '', drugName: row.drugName || '', dose: row.dose || '', withdrawalDays: row.withdrawalDays ?? 7, note: '' })
  reviewDlg.value = true
}
async function doReview() {
  if (!rv.diagnosis) { ElMessage.warning('请填写诊断结论'); return }
  if (rv.status === 'APPROVED' && (!rv.drugName || rv.withdrawalDays === null)) { ElMessage.warning('通过审核需填写药品与休药期'); return }
  if (rv.status === 'REJECTED' && (!rv.note || rv.note.trim().length < 5)) { ElMessage.warning('驳回需填写不少于 5 个字的原因'); return }
  saving.value = true
  try {
    await http.post(`/api/disease/${cur.value.id}/review`, rv)
    ElMessage.success('审核已提交'); reviewDlg.value = false; load()
  } finally { saving.value = false }
}
function openVoid(row) { cur.value = row; voidReason.value = ''; voidDlg.value = true }
async function doVoid() {
  if (!voidReason.value || voidReason.value.trim().length < 5) { ElMessage.warning('请填写不少于 5 个字的作废原因'); return }
  saving.value = true
  try {
    await http.post(`/api/disease/${cur.value.id}/void`, { reason: voidReason.value.trim() })
    ElMessage.success('已作废'); voidDlg.value = false; load()
  } finally { saving.value = false }
}
function openDetail(row) { cur.value = row; detailDlg.value = true }

onMounted(async () => {
  load()
  batches.value = await http.get('/api/aquaculture-water/options')
})
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; padding: 12px 18px; }
.dimg { width: 100%; height: 190px; border-radius: 6px; margin-bottom: 14px; border: 1px solid var(--c-border); }
</style>
