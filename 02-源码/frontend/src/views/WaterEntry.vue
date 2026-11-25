<template>
  <div class="page">
    <div class="page-head">
      <h1>录入水质检测</h1>
      <span class="sub">保存后立即执行多参数风险分层；超限将自动生成待处置预警</span>
      <div class="actions"><el-button :icon="Back" @click="$router.back()">返回</el-button></div>
    </div>

    <div class="grid-2-1">
      <div class="card">
        <div class="card-h"><h3>检测数据</h3><span class="hint">带 * 为必填，数值按现场仪表读数填写</span></div>
        <div class="card-b">
          <el-form :model="form" label-width="112px" label-position="left">
            <div class="grid2">
              <el-form-item label="养殖批次" required>
                <el-select v-model="form.batchId" filterable placeholder="选择批次" style="width: 100%" @change="onBatch">
                  <el-option v-for="b in batches" :key="b.id" :label="`${b.batchNo}（${b.pondName}·${b.variety}）`" :value="b.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="检测时间" required>
                <el-date-picker v-model="form.checkTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
              </el-form-item>
              <el-form-item label="水温(℃)" required>
                <el-input-number v-model="form.waterTemp" :min="0" :max="45" :precision="1" :step="0.5" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="pH" required>
                <el-input-number v-model="form.ph" :min="0" :max="14" :precision="2" :step="0.1" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="溶解氧(mg/L)" required>
                <el-input-number v-model="form.dissolvedOxygen" :min="0" :max="20" :precision="1" :step="0.1" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="氨氮(mg/L)" required>
                <el-input-number v-model="form.ammonia" :min="0" :max="5" :precision="2" :step="0.01" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="亚硝酸盐(mg/L)" required>
                <el-input-number v-model="form.nitrite" :min="0" :max="5" :precision="2" :step="0.01" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item label="透明度(cm)">
                <el-input-number v-model="form.transparency" :min="0" :max="200" controls-position="right" style="width: 100%" />
              </el-form-item>
            </div>
            <el-form-item label="仪表照片" required>
              <ImageUpload v-model="form.image" type="gauge" />
            </el-form-item>
            <el-form-item label="现场备注">
              <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="120" show-word-limit placeholder="如：阴雨、刚换水、摄食异常等" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="submit">保存并计算风险等级</el-button>
              <el-button @click="reset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <div class="card">
        <div class="card-h"><h3>适宜区间参考</h3><span class="hint">按生长阶段</span></div>
        <div class="card-b">
          <div class="tip" style="margin-bottom: 10px">当前阶段：{{ stageLabel }}</div>
          <table class="ref">
            <thead><tr><th>指标</th><th>适宜区间</th></tr></thead>
            <tbody>
              <tr v-for="r in ranges" :key="r.k"><td>{{ r.k }}</td><td class="num">{{ r.v }}</td></tr>
            </tbody>
          </table>
          <div class="notice mt16">超出区间不会直接判定处罚，只生成待处置预警与处置建议，由技术员确认后执行。</div>
        </div>
      </div>
    </div>

    <!-- 保存后的计算结果：逐项得分 + 未满足条件 + 建议 -->
    <div class="card mt16" v-if="result">
      <div class="card-h"><h3>风险分层计算结果</h3><span class="hint">{{ result.recordNo }} · 规则 v{{ result.ruleVersion }}</span></div>
      <div class="card-b">
        <div class="flex">
          <div>
            <div class="tip">综合得分</div>
            <div class="big" :style="{ color: RISK_LEVEL[result.riskLevel]?.color }">{{ result.totalScore }} 分 · {{ result.levelText }}</div>
          </div>
          <el-tag class="right" :type="RISK_LEVEL[result.riskLevel]?.type" size="large">{{ RISK_LEVEL[result.riskLevel]?.label }}</el-tag>
        </div>
        <table class="score-tb">
          <thead><tr><th>因素</th><th>实测</th><th>适宜区间</th><th>权重</th><th>得分</th><th>贡献</th></tr></thead>
          <tbody>
            <tr v-for="it in result.items" :key="it.key">
              <td>{{ it.label }}</td><td class="num">{{ it.value }}</td><td class="num">{{ it.range }}</td>
              <td class="num">{{ it.weight }}%</td>
              <td><span class="score-bar"><i :style="{ width: Math.min(it.score, 100) + '%' }"></i></span><span class="num">{{ it.score }}</span></td>
              <td class="num">{{ it.contribution }}</td>
            </tr>
          </tbody>
        </table>
        <div class="block-t mt16">规则建议</div>
        <div class="advice">{{ result.advice }}</div>
        <div class="flex mt16">
          <el-button type="primary" plain @click="$router.push('/water')">查看水质记录</el-button>
          <el-button v-if="result.warningId" type="danger" plain @click="$router.push(`/warnings/${result.warningId}`)">前往处置预警</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import http from '../api/http'
import ImageUpload from '../components/ImageUpload.vue'
import { GROWTH_STAGE, RISK_LEVEL } from '../utils/status'

const route = useRoute()
const router = useRouter()
const batches = ref([])
const saving = ref(false)
const result = ref(null)
const stage = ref('ADULT')

const form = reactive({
  batchId: route.query.batchId ? Number(route.query.batchId) : null,
  checkTime: nowStr(),
  waterTemp: 26,
  ph: 7.4,
  dissolvedOxygen: 6,
  ammonia: 0.1,
  nitrite: 0.02,
  transparency: 55,
  image: '',
  remark: ''
})

function nowStr() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:00`
}

/** 与后端 risk_rule.threshold_json 保持一致的参考区间 */
const THRESHOLD = {
  FRY: { do: [5, 12], ph: [6.8, 8.2], temp: [24, 30], ammonia: [0, 0.15], nitrite: [0, 0.08] },
  JUVENILE: { do: [4.5, 12], ph: [6.6, 8.4], temp: [22, 31], ammonia: [0, 0.2], nitrite: [0, 0.1] },
  ADULT: { do: [4, 14], ph: [6.5, 8.5], temp: [18, 32], ammonia: [0, 0.3], nitrite: [0, 0.15] }
}
const stageLabel = computed(() => GROWTH_STAGE[stage.value]?.label || '成鱼')
const ranges = computed(() => {
  const t = THRESHOLD[stage.value] || THRESHOLD.ADULT
  return [
    { k: '水温', v: `${t.temp[0]}~${t.temp[1]} ℃` },
    { k: 'pH', v: `${t.ph[0]}~${t.ph[1]}` },
    { k: '溶解氧', v: `${t.do[0]}~${t.do[1]} mg/L` },
    { k: '氨氮', v: `${t.ammonia[0]}~${t.ammonia[1]} mg/L` },
    { k: '亚硝酸盐', v: `${t.nitrite[0]}~${t.nitrite[1]} mg/L` }
  ]
})

function onBatch(id) {
  const b = batches.value.find((x) => x.id === id)
  if (b) stage.value = b.growthStage
}
function reset() {
  Object.assign(form, { checkTime: nowStr(), waterTemp: 26, ph: 7.4, dissolvedOxygen: 6, ammonia: 0.1, nitrite: 0.02, transparency: 55, image: '', remark: '' })
  result.value = null
}

async function submit() {
  if (!form.batchId) { ElMessage.warning('请选择养殖批次'); return }
  if (!form.image) { ElMessage.warning('请上传水质仪表照片'); return }
  saving.value = true
  try {
    const r = await http.post('/api/water', form)
    result.value = { ...r.risk, recordNo: r.recordNo, warningId: r.warningId }
    ElMessage.success(`已保存 ${r.recordNo}，风险等级：${r.risk.levelText}`)
  } finally { saving.value = false }
}

onMounted(async () => {
  batches.value = await http.get('/api/aquaculture-water/options')
  if (form.batchId) onBatch(form.batchId)
})
</script>

<style scoped>
.big { font-size: 24px; font-weight: 600; margin-top: 4px; }
.ref { width: 100%; border-collapse: collapse; font-size: 12.5px; }
.ref th { text-align: left; background: #F6F8F7; padding: 8px 10px; color: #5B7176; font-weight: 500; }
.ref td { padding: 8px 10px; border-bottom: 1px solid var(--c-border-l); }
.score-tb { width: 100%; border-collapse: collapse; margin-top: 14px; font-size: 12.5px; }
.score-tb th { background: #F6F8F7; text-align: left; padding: 8px 10px; color: #3F5C62; font-weight: 600; }
.score-tb td { padding: 8px 10px; border-bottom: 1px solid var(--c-border-l); }
.advice { background: #F6F8F7; border-left: 3px solid var(--c-secondary); padding: 10px 14px; font-size: 12.5px; line-height: 22px; border-radius: 0 4px 4px 0; }
</style>
