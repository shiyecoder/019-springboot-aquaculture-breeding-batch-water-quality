<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <h1>{{ wn.warningNo || '预警详情' }}</h1>
      <el-tag :type="statusOf(WARNING_STATUS, wn.status).type">{{ statusOf(WARNING_STATUS, wn.status).label }}</el-tag>
      <el-tag :type="RISK_LEVEL[wn.level]?.type" effect="plain">{{ RISK_LEVEL[wn.level]?.label }}</el-tag>
      <span class="sub">{{ pond.pondName }} · {{ batch.batchNo }} · 负责人 {{ wn.handlerName || '待指派' }}</span>
      <div class="actions">
        <el-button :icon="Back" @click="$router.back()">返回</el-button>
        <el-button v-if="['PENDING', 'RETEST'].includes(wn.status)" type="primary" @click="handleDlg = true">登记处置措施</el-button>
        <el-button v-if="['PROCESSING', 'RETEST'].includes(wn.status)" type="primary" plain @click="retestDlg = true">复测确认</el-button>
        <el-button v-if="wn.status !== 'CLOSED'" type="danger" plain @click="closeDlg = true">直接关闭</el-button>
      </div>
    </div>

    <div class="grid-2-1">
      <div class="card">
        <div class="card-h"><h3>预警信息</h3><span class="hint">生成 {{ formatTime(wn.createTime) }}{{ wn.closeTime ? ' · 关闭 ' + formatTime(wn.closeTime) : '' }}</span></div>
        <div class="card-b">
          <div class="notice">{{ wn.content }}</div>
          <div class="grid2 mt16">
            <dl class="detail-dl">
              <dt>预警编号</dt><dd>{{ wn.warningNo }}</dd>
              <dt>预警类型</dt><dd>{{ WARN_TYPE[wn.warnType] || wn.warnType }}</dd>
              <dt>风险等级</dt><dd>{{ RISK_LEVEL[wn.level]?.label }}</dd>
              <dt>当前状态</dt><dd>{{ statusOf(WARNING_STATUS, wn.status).label }}</dd>
            </dl>
            <dl class="detail-dl">
              <dt>塘口</dt><dd>{{ pond.pondNo }} {{ pond.pondName }}（{{ pond.area }} 亩 · 增氧机 {{ pond.aeratorCount }} 台）</dd>
              <dt>批次</dt><dd>{{ batch.batchNo }} · {{ batch.variety }} · {{ statusOf(GROWTH_STAGE, batch.growthStage).label }}</dd>
              <dt>处置人</dt><dd>{{ wn.handlerName || '—' }}</dd>
              <dt>检测时间</dt><dd>{{ formatTime(water.checkTime) }}</dd>
            </dl>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-h"><h3>触发参数</h3><span class="hint">检测值 vs 适宜区间</span></div>
        <div class="card-b">
          <el-image v-if="water.image" :src="water.image" :preview-src-list="[water.image]" fit="cover" class="wimg" preview-teleported />
          <table class="pt">
            <tbody>
              <tr><th>水温</th><td class="num">{{ water.waterTemp }} ℃</td></tr>
              <tr><th>pH</th><td class="num">{{ water.ph }}</td></tr>
              <tr><th>溶解氧</th><td class="num">{{ water.dissolvedOxygen }} mg/L</td></tr>
              <tr><th>氨氮</th><td class="num">{{ water.ammonia }} mg/L</td></tr>
              <tr><th>亚硝酸盐</th><td class="num">{{ water.nitrite }} mg/L</td></tr>
              <tr><th>透明度</th><td class="num">{{ water.transparency }} cm</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-h"><h3>多参数水质风险分层</h3><span class="hint">输入快照、逐项得分与规则建议，结果可追溯</span></div>
      <div class="card-b">
        <template v-if="score">
          <div class="flex" style="margin-bottom: 12px">
            <div>
              <div class="tip">综合得分（规则 v{{ score.ruleVersion }}）</div>
              <div class="big" :style="{ color: RISK_LEVEL[score.riskLevel]?.color }">{{ score.totalScore }} 分 · {{ RISK_LEVEL[score.riskLevel]?.label }}</div>
            </div>
            <el-tag class="right" v-if="score.manualLevel" type="warning">人工修正为 {{ RISK_LEVEL[score.manualLevel]?.label }}</el-tag>
          </div>
          <table class="st">
            <thead><tr><th>因素</th><th>实测</th><th>适宜区间</th><th>权重</th><th>得分</th><th>贡献</th></tr></thead>
            <tbody>
              <tr v-for="it in parse(score.scoreJson)" :key="it.key">
                <td>{{ it.label }}</td>
                <td class="num">{{ it.value || '—' }}</td>
                <td class="num">{{ it.range || '—' }}</td>
                <td class="num">{{ it.weight }}%</td>
                <td><span class="score-bar"><i :style="{ width: Math.min(it.score, 100) + '%' }"></i></span><span class="num">{{ it.score }}</span></td>
                <td class="num">{{ it.contribution ?? ((it.score * it.weight) / 100).toFixed(1) }}</td>
              </tr>
            </tbody>
          </table>
          <div class="block-t mt16">规则建议</div>
          <div class="advice">{{ score.advice }}</div>
          <div v-if="score.manualReason" class="why mt8">人工修正：{{ score.manualReason }}（{{ score.manualBy }} · {{ formatTime(score.manualTime) }}）</div>
        </template>
        <el-empty v-else description="该预警暂未关联风险分层计算记录" :image-size="64" />
      </div>
    </div>

    <div class="card">
      <div class="card-h"><h3>处置与复测时间轴</h3></div>
      <div class="card-b">
        <div class="grid2">
          <div>
            <div class="block-t">应急处置记录</div>
            <div v-for="e in emergencies" :key="e.id" class="em">
              <div class="flex"><b>{{ e.measure === 'RETEST' ? '复测' : MEASURE[e.measure] || e.measure }}</b><span class="tip right">{{ formatTime(e.actionTime) }} · {{ e.operator }}</span></div>
              <div class="tip mt8">{{ e.detail || '—' }}</div>
              <div v-if="e.retestResult" class="retest">复测结果：{{ e.retestResult }}</div>
            </div>
            <el-empty v-if="!emergencies.length" description="尚未登记处置措施" :image-size="60" />
          </div>
          <div>
            <div class="block-t">状态流转留痕</div>
            <div class="tl">
              <div v-for="(e, i) in reverse(timeline)" :key="e.id" class="ev" :class="{ cur: i === 0 }">
                <div class="t">{{ e.action }}</div>
                <div class="m">{{ formatTime(e.createTime) }} · {{ e.operatorName }}</div>
                <div v-if="e.reason" class="why">{{ e.reason }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="handleDlg" title="登记处置措施" width="480px" destroy-on-close>
      <el-form :model="handleForm" label-width="94px" label-position="left">
        <el-form-item label="处置措施" required>
          <el-select v-model="handleForm.measure" style="width: 220px" placeholder="选择措施">
            <el-option v-for="(v, k) in MEASURE" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行详情">
          <el-input v-model="handleForm.detail" type="textarea" :rows="3" maxlength="160" placeholder="如：开启 4 台增氧机 2 小时，换水 1/3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doHandle">提交（转处置中）</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="retestDlg" title="复测确认" width="500px" destroy-on-close>
      <div class="notice">复测需选择同批次的新水质记录，确认后预警关闭，并计入处置时长统计。</div>
      <el-form label-width="104px" label-position="left" class="mt16">
        <el-form-item label="复测记录" required>
          <el-select v-model="retestForm.waterId" style="width: 260px" placeholder="选择本批次水质记录" filterable>
            <el-option v-for="w in batchWaters" :key="w.id" :label="`${w.recordNo} · ${formatTime(w.checkTime)}`" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="复测结论">
          <el-input v-model="retestForm.result" type="textarea" :rows="2" placeholder="如：溶氧回升至 6.2 mg/L，指标恢复正常" />
        </el-form-item>
      </el-form>
      <div class="tip">若尚未录入复测数据，请先到「水质监测 → 录入水质」新增一条记录。</div>
      <template #footer>
        <el-button @click="retestDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doRetest">确认关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="closeDlg" title="关闭预警" width="440px" destroy-on-close>
      <el-form label-width="80px" label-position="left">
        <el-form-item label="关闭原因" required>
          <el-input v-model="closeReason" type="textarea" :rows="3" maxlength="120" placeholder="不少于 5 个字，说明核实结果" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDlg = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="doClose">确认关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import http from '../api/http'
import { formatTime } from '../utils/format'
import { GROWTH_STAGE, MEASURE, RISK_LEVEL, WARNING_STATUS, WARN_TYPE, statusOf } from '../utils/status'

const route = useRoute()
const loading = ref(false)
const saving = ref(false)
const data = ref({})
const batchWaters = ref([])
const handleDlg = ref(false)
const retestDlg = ref(false)
const closeDlg = ref(false)
const closeReason = ref('')
const handleForm = reactive({ measure: 'AERATION', detail: '' })
const retestForm = reactive({ waterId: null, result: '' })

const wn = computed(() => data.value.warning || {})
const water = computed(() => data.value.water || {})
const pond = computed(() => data.value.pond || {})
const batch = computed(() => data.value.batch || {})
const emergencies = computed(() => data.value.emergencies || [])
const timeline = computed(() => data.value.timeline || [])
const score = computed(() => (data.value.scores || [])[0] || null)

function parse(json) {
  try { return JSON.parse(json || '[]') } catch (e) { return [] }
}
function reverse(list) { return [...list].reverse() }

async function load() {
  loading.value = true
  try {
    data.value = await http.get(`/api/water/warning/${route.params.id}`)
    const w = await http.get('/api/water/page', { params: { page: 1, size: 20, batchId: batch.value.id } })
    batchWaters.value = w.list || []
  } finally { loading.value = false }
}

async function doHandle() {
  if (!handleForm.measure) { ElMessage.warning('请选择处置措施'); return }
  saving.value = true
  try {
    await http.post(`/api/water/warning/${wn.value.id}/handle`, handleForm)
    ElMessage.success('已登记处置措施，状态转为处置中')
    handleDlg.value = false
    load()
  } finally { saving.value = false }
}
async function doRetest() {
  if (!retestForm.waterId) { ElMessage.warning('请选择复测水质记录'); return }
  saving.value = true
  try {
    await http.post(`/api/water/warning/${wn.value.id}/retest`, retestForm)
    ElMessage.success('复测确认，预警已关闭')
    retestDlg.value = false
    load()
  } finally { saving.value = false }
}
async function doClose() {
  if (!closeReason.value || closeReason.value.trim().length < 5) { ElMessage.warning('请填写不少于 5 个字的关闭原因'); return }
  saving.value = true
  try {
    await http.post(`/api/water/warning/${wn.value.id}/close`, { reason: closeReason.value.trim() })
    ElMessage.success('预警已关闭')
    closeDlg.value = false
    load()
  } finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.big { font-size: 22px; font-weight: 600; margin-top: 3px; }
.wimg { width: 100%; height: 128px; border-radius: 6px; margin-bottom: 10px; border: 1px solid var(--c-border); }
.pt { width: 100%; border-collapse: collapse; font-size: 12.5px; }
.pt th { text-align: left; width: 96px; padding: 7px 10px; background: #F6F8F7; color: #5B7176; font-weight: 500; }
.pt td { padding: 7px 10px; border-bottom: 1px solid var(--c-border-l); }
.st { width: 100%; border-collapse: collapse; font-size: 12.5px; }
.st th { background: #F6F8F7; text-align: left; padding: 8px 10px; color: #3F5C62; font-weight: 600; }
.st td { padding: 8px 10px; border-bottom: 1px solid var(--c-border-l); }
.advice { background: #F6F8F7; border-left: 3px solid var(--c-secondary); padding: 10px 14px; font-size: 12.5px; line-height: 22px; border-radius: 0 4px 4px 0; }
.em { padding: 10px 12px; border: 1px solid var(--c-border-l); border-radius: 6px; margin-bottom: 8px; }
.retest { margin-top: 8px; font-size: 12px; color: var(--c-success); background: #F1F8F4; padding: 6px 10px; border-radius: 4px; }
.why { font-size: 12px; color: var(--c-text-2); background: #F6F8F7; border-left: 2px solid var(--c-border); padding: 6px 10px; }
</style>
