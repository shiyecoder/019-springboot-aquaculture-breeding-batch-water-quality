<template>
  <div class="page">
    <div class="page-head">
      <h1>水质风险规则</h1>
      <span class="sub">多参数风险分层：权重合计必须为 100%，修改后生成新版本，仅影响新计算</span>
      <div class="actions"><el-button type="primary" :loading="saving" @click="save">保存为新版本</el-button></div>
    </div>

    <div class="grid-2-1">
      <div class="card">
        <div class="card-h"><h3>权重配置</h3><span class="hint">当前版本 v{{ active.version || '—' }} · 生效 {{ formatTime(active.effectiveTime) }}</span></div>
        <div class="card-b">
          <div class="wrow" v-for="w in weightItems" :key="w.key">
            <div class="lb">{{ w.label }}</div>
            <el-slider v-model="form[w.key]" :min="0" :max="60" :step="5" style="flex: 1; margin: 0 16px" />
            <el-input-number v-model="form[w.key]" :min="0" :max="100" :step="5" size="small" controls-position="right" style="width: 92px" />
            <span class="pct">%</span>
          </div>
          <div class="sum" :class="{ bad: totalWeight !== 100 }">
            权重合计 <b>{{ totalWeight }}%</b>
            <span v-if="totalWeight !== 100">，需调整为 100% 才能保存</span>
          </div>
          <el-form label-width="98px" label-position="left" class="mt16">
            <el-form-item label="规则名称"><el-input v-model="form.ruleName" maxlength="40" /></el-form-item>
          </el-form>
          <div class="notice">权重调整会改变评分口径，历史计算快照保留原版本，不会被覆盖。</div>
        </div>
      </div>

      <div class="card">
        <div class="card-h"><h3>版本历史</h3></div>
        <div class="card-b">
          <div v-for="r in rules" :key="r.id" class="ver">
            <div class="flex">
              <b>v{{ r.version }}</b>
              <el-tag v-if="r.enabled === 1" size="small" type="success">现行</el-tag>
              <span class="tip right">{{ formatTime(r.effectiveTime) }}</span>
            </div>
            <div class="tip mt8">{{ r.ruleName }} · 创建人 {{ r.createBy }}</div>
            <div class="tip">水温 {{ r.weightTemp }} / pH {{ r.weightPh }} / 溶氧 {{ r.weightDo }} / 氨氮 {{ r.weightAmmonia }} / 亚硝酸盐 {{ r.weightNitrite }} / 连续异常 {{ r.weightTrend }}</div>
          </div>
          <el-empty v-if="!rules.length" description="暂无规则版本" :image-size="60" />
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-h"><h3>适宜区间（按生长阶段）</h3><span class="hint">偏离区间越大得分越高，权重加权后得到总分</span></div>
      <div class="card-b">
        <table class="th">
          <thead>
            <tr><th>阶段</th><th>水温(℃)</th><th>pH</th><th>溶解氧(mg/L)</th><th>氨氮(mg/L)</th><th>亚硝酸盐(mg/L)</th></tr>
          </thead>
          <tbody>
            <tr v-for="(t, k) in thresholds" :key="k">
              <td>{{ GROWTH_STAGE[k]?.label || k }}</td>
              <td><el-input v-model="t.temp" size="small" placeholder="24,30" /></td>
              <td><el-input v-model="t.ph" size="small" placeholder="6.8,8.2" /></td>
              <td><el-input v-model="t.do" size="small" placeholder="5,12" /></td>
              <td><el-input v-model="t.ammonia" size="small" placeholder="0,0.15" /></td>
              <td><el-input v-model="t.nitrite" size="small" placeholder="0,0.08" /></td>
            </tr>
          </tbody>
        </table>
        <div class="tip mt8">填写格式为「下限,上限」，保存时会转为规则的 thresholdJson 一并提交。</div>
      </div>
    </div>

    <div class="card">
      <div class="card-h"><h3>计算试算</h3><span class="hint">选择一条水质记录，按当前规则计算并展示逐项依据</span></div>
      <div class="card-b">
        <div class="flex">
          <el-select v-model="waterId" filterable placeholder="选择水质记录" style="width: 320px">
            <el-option v-for="w in waters" :key="w.id" :label="`${w.recordNo} · ${w.pondName} · ${formatTime(w.checkTime)}`" :value="w.id" />
          </el-select>
          <el-button type="primary" plain :loading="calc" @click="doCalc">执行计算</el-button>
        </div>
        <div v-if="result" class="mt16">
          <div class="flex" style="margin-bottom: 10px">
            <b :style="{ color: RISK_LEVEL[result.riskLevel]?.color, fontSize: '20px' }">{{ result.totalScore }} 分 · {{ result.levelText }}</b>
            <span class="tip">规则 v{{ result.ruleVersion }} · 可复现 {{ result.reproducible ? '是' : '否' }}</span>
          </div>
          <table class="st">
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
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { formatTime } from '../utils/format'
import { GROWTH_STAGE, RISK_LEVEL } from '../utils/status'

const rules = ref([])
const waters = ref([])
const active = ref({})
const saving = ref(false)
const calc = ref(false)
const waterId = ref(null)
const result = ref(null)
const form = reactive({ ruleName: '多参数水质风险分层 v3', weightTemp: 20, weightPh: 15, weightDo: 25, weightAmmonia: 20, weightNitrite: 15, weightTrend: 5 })
const thresholds = reactive({
  FRY: { temp: '24,30', ph: '6.8,8.2', do: '5,12', ammonia: '0,0.15', nitrite: '0,0.08' },
  JUVENILE: { temp: '22,31', ph: '6.6,8.4', do: '4.5,12', ammonia: '0,0.2', nitrite: '0,0.1' },
  ADULT: { temp: '18,32', ph: '6.5,8.5', do: '4,14', ammonia: '0,0.3', nitrite: '0,0.15' }
})

const weightItems = [
  { key: 'weightTemp', label: '水温偏差' },
  { key: 'weightPh', label: 'pH 偏差' },
  { key: 'weightDo', label: '溶解氧' },
  { key: 'weightAmmonia', label: '氨氮' },
  { key: 'weightNitrite', label: '亚硝酸盐' },
  { key: 'weightTrend', label: '连续异常次数' }
]
const totalWeight = computed(() => weightItems.reduce((s, i) => s + Number(form[i.key] || 0), 0))

function toJson() {
  const out = {}
  Object.keys(thresholds).forEach((k) => {
    const t = thresholds[k]
    out[k] = {
      temp: t.temp.split(',').map(Number),
      ph: t.ph.split(',').map(Number),
      do: t.do.split(',').map(Number),
      ammonia: t.ammonia.split(',').map(Number),
      nitrite: t.nitrite.split(',').map(Number)
    }
  })
  return JSON.stringify(out)
}

async function load() {
  rules.value = await http.get('/api/rule/list')
  active.value = rules.value.find((r) => r.enabled === 1) || rules.value[0] || {}
  if (active.value.id) {
    Object.assign(form, {
      ruleName: `多参数水质风险分层 v${(active.value.version || 1) + 1}`,
      weightTemp: Number(active.value.weightTemp),
      weightPh: Number(active.value.weightPh),
      weightDo: Number(active.value.weightDo),
      weightAmmonia: Number(active.value.weightAmmonia),
      weightNitrite: Number(active.value.weightNitrite),
      weightTrend: Number(active.value.weightTrend)
    })
    try {
      const t = JSON.parse(active.value.thresholdJson || '{}')
      Object.keys(thresholds).forEach((k) => {
        if (!t[k]) return
        thresholds[k] = {
          temp: t[k].temp.join(','), ph: t[k].ph.join(','), do: t[k].do.join(','),
          ammonia: t[k].ammonia.join(','), nitrite: t[k].nitrite.join(',')
        }
      })
    } catch (e) { /* 规则 JSON 异常时保留默认阈值 */ }
  }
  const w = await http.get('/api/water/page', { params: { page: 1, size: 20 } })
  waters.value = w.list || []
}

async function save() {
  if (totalWeight.value !== 100) { ElMessage.warning('权重合计必须等于 100%'); return }
  saving.value = true
  try {
    await http.post('/api/rule', { ...form, thresholdJson: toJson() })
    ElMessage.success('新版本规则已生效，仅影响新计算')
    load()
  } finally { saving.value = false }
}

async function doCalc() {
  if (!waterId.value) { ElMessage.warning('请选择水质记录'); return }
  calc.value = true
  try { result.value = await http.post(`/api/water/${waterId.value}/evaluate`) } finally { calc.value = false }
}

onMounted(load)
</script>

<style scoped>
.wrow { display: flex; align-items: center; margin-bottom: 10px; }
.wrow .lb { width: 110px; font-size: 13px; color: var(--c-text-2); }
.wrow .pct { margin-left: 6px; color: var(--c-text-2); font-size: 12px; }
.sum { margin-top: 6px; padding: 10px 14px; background: #F1F8F4; border: 1px solid #CFE5D9; border-radius: 6px; font-size: 12.5px; color: #2B7A5A; }
.sum.bad { background: #FBF2DF; border-color: #EBD9AE; color: #8A5E17; }
.ver { padding: 10px 0; border-bottom: 1px dashed var(--c-border); }
.th { width: 100%; border-collapse: collapse; font-size: 12.5px; }
.th th { background: #F6F8F7; text-align: left; padding: 8px 10px; color: #3F5C62; font-weight: 600; }
.th td { padding: 7px 10px; border-bottom: 1px solid var(--c-border-l); }
.st { width: 100%; border-collapse: collapse; font-size: 12.5px; }
.st th { background: #F6F8F7; text-align: left; padding: 8px 10px; color: #3F5C62; font-weight: 600; }
.st td { padding: 8px 10px; border-bottom: 1px solid var(--c-border-l); }
.advice { background: #F6F8F7; border-left: 3px solid var(--c-secondary); padding: 10px 14px; font-size: 12.5px; line-height: 22px; border-radius: 0 4px 4px 0; }
</style>
