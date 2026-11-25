<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <h1>{{ d.batchNo || '批次详情' }}</h1>
      <el-tag :type="statusOf(BATCH_STATUS, d.status).type">{{ statusOf(BATCH_STATUS, d.status).label }}</el-tag>
      <span class="sub">{{ d.pondNo }} {{ d.pondName }} · {{ d.variety }} · 责任人 {{ d.managerName || '—' }}</span>
      <div class="actions">
        <el-button :icon="Back" @click="$router.back()">返回</el-button>
        <el-button type="primary" plain @click="goWater">录入水质</el-button>
      </div>
    </div>

    <div class="grid3">
      <div class="kpi"><div class="lb">当前存量</div><div class="v">{{ n(d.currentStock) }}<small>kg</small></div><div class="df">目标产量 {{ n(d.targetOutput) }} kg</div></div>
      <div class="kpi"><div class="lb">累计投喂</div><div class="v">{{ n(d.feedTotal) }}<small>kg</small></div><div class="df">饲料系数 {{ feedCoefficient }}</div></div>
      <div class="kpi"><div class="lb">风险分层（最新）</div><div class="v" :style="{ color: RISK_LEVEL[d.latestScore?.riskLevel]?.color }">{{ d.latestScore ? d.latestScore.totalScore : '—' }}<small>分</small></div><div class="df">{{ RISK_LEVEL[d.latestScore?.riskLevel]?.label || '暂无计算记录' }}</div></div>
    </div>

    <div class="card mt16">
      <div class="card-h"><h3>基础信息</h3><span class="hint">创建 {{ formatTime(d.createTime) }} · 最近更新 {{ formatTime(d.updateTime) }}</span></div>
      <div class="card-b">
        <div class="grid2">
          <dl class="detail-dl">
            <dt>批次编号</dt><dd>{{ d.batchNo }}</dd>
            <dt>养殖池</dt><dd>{{ d.pondNo }} {{ d.pondName }}</dd>
            <dt>养殖品种</dt><dd>{{ d.variety }}</dd>
            <dt>苗种来源</dt><dd>{{ d.seedSource || '—' }}</dd>
            <dt>投苗日期</dt><dd>{{ d.stockDate || '—' }}</dd>
            <dt>投苗数量</dt><dd><span class="num">{{ d.stockQty }} 万尾</span></dd>
          </dl>
          <dl class="detail-dl">
            <dt>平均规格</dt><dd><span class="num">{{ d.avgWeight || '—' }} g</span></dd>
            <dt>预计起捕日</dt><dd>{{ d.expectHarvestDate || '—' }}</dd>
            <dt>生长阶段</dt><dd>{{ statusOf(GROWTH_STAGE, d.growthStage).label }}</dd>
            <dt>可流转至</dt><dd>{{ (d.nextStatus || []).map((s) => statusOf(BATCH_STATUS, s).label).join('、') || '已完成全部阶段' }}</dd>
            <dt>备注</dt><dd>{{ d.remark || '—' }}</dd>
          </dl>
        </div>
      </div>
    </div>

    <div class="card">
      <el-tabs v-model="tab" class="tabs">
        <el-tab-pane label="水质记录" name="water">
          <el-table :data="d.waters || []" size="small" max-height="360">
            <el-table-column prop="recordNo" label="记录编号" width="150" class-name="nowrap" />
            <el-table-column label="检测时间" width="150" class-name="nowrap"><template #default="{ row }">{{ formatTime(row.checkTime) }}</template></el-table-column>
            <el-table-column prop="waterTemp" label="水温" width="76" align="right" />
            <el-table-column prop="ph" label="pH" width="66" align="right" />
            <el-table-column prop="dissolvedOxygen" label="溶氧" width="76" align="right" />
            <el-table-column prop="ammonia" label="氨氮" width="76" align="right" />
            <el-table-column prop="nitrite" label="亚硝酸盐" width="92" align="right" />
            <el-table-column prop="transparency" label="透明度" width="86" align="right" />
            <el-table-column label="等级" width="96">
              <template #default="{ row }"><el-tag size="small" :type="statusOf(WATER_LEVEL, row.level).type">{{ statusOf(WATER_LEVEL, row.level).label }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="testerName" label="检测人" width="90" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }"><el-button link type="primary" @click="evaluate(row)">风险分层</el-button></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`投喂计划 (${(d.plans || []).length})`" name="plan">
          <el-table :data="d.plans || []" size="small" max-height="360">
            <el-table-column prop="planNo" label="计划编号" width="150" />
            <el-table-column prop="planDate" label="日期" width="110" />
            <el-table-column label="时段" width="86"><template #default="{ row }">{{ PERIOD[row.period] || row.period }}</template></el-table-column>
            <el-table-column prop="feedName" label="饲料" min-width="150" />
            <el-table-column prop="feedQty" label="计划量(kg)" width="106" align="right" />
            <el-table-column prop="actualQty" label="实际量(kg)" width="106" align="right" />
            <el-table-column label="状态" width="96"><template #default="{ row }"><el-tag size="small" :type="statusOf(FEEDING_STATUS, row.status).type">{{ statusOf(FEEDING_STATUS, row.status).label }}</el-tag></template></el-table-column>
            <el-table-column prop="executorName" label="执行人" width="96" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`预警 (${(d.warnings || []).length})`" name="warn">
          <el-table :data="d.warnings || []" size="small" max-height="360">
            <el-table-column prop="warningNo" label="预警编号" width="150" />
            <el-table-column label="类型" width="96"><template #default="{ row }">{{ WARN_TYPE[row.warnType] || row.warnType }}</template></el-table-column>
            <el-table-column label="等级" width="86"><template #default="{ row }"><el-tag size="small" :type="RISK_LEVEL[row.level]?.type">{{ RISK_LEVEL[row.level]?.label }}</el-tag></template></el-table-column>
            <el-table-column label="状态" width="96"><template #default="{ row }"><el-tag size="small" :type="statusOf(WARNING_STATUS, row.status).type">{{ statusOf(WARNING_STATUS, row.status).label }}</el-tag></template></el-table-column>
            <el-table-column prop="content" label="预警内容" min-width="280" show-overflow-tooltip />
            <el-table-column label="生成时间" width="150"><template #default="{ row }">{{ formatTime(row.createTime) }}</template></el-table-column>
            <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="$router.push(`/warnings/${row.id}`)">处置</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`起捕与分塘 (${(d.harvests || []).length})`" name="harvest">
          <el-table :data="d.harvests || []" size="small" max-height="360">
            <el-table-column prop="harvestNo" label="起捕编号" width="150" />
            <el-table-column prop="planDate" label="计划日期" width="110" />
            <el-table-column prop="weight" label="重量(kg)" width="100" align="right" />
            <el-table-column label="等级" width="86"><template #default="{ row }">{{ GRADE[row.grade]?.label || row.grade }}</template></el-table-column>
            <el-table-column prop="destination" label="去向" min-width="120" />
            <el-table-column label="状态" width="96"><template #default="{ row }"><el-tag size="small" :type="statusOf(HARVEST_STATUS, row.status).type">{{ statusOf(HARVEST_STATUS, row.status).label }}</el-tag></template></el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="操作时间轴" name="timeline">
          <div class="tl" style="padding: 6px 0 0 18px">
            <div v-for="(e, i) in reverse(d.timeline)" :key="e.id" class="ev" :class="{ cur: i === 0 }">
              <div class="t">{{ e.action }}</div>
              <div class="m">{{ formatTime(e.createTime) }} · {{ e.operatorName }} · {{ e.fromStatus || '—' }} → {{ e.toStatus || '—' }}</div>
              <div v-if="e.reason" class="why">{{ e.reason }}</div>
            </div>
            <el-empty v-if="!(d.timeline || []).length" description="暂无流转记录" :image-size="60" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 风险分层结果：展示输入依据、逐项得分、建议与人工修正 -->
    <el-drawer v-model="scoreDlg" title="多参数水质风险分层计算" size="620px" destroy-on-close>
      <div v-if="score">
        <div class="score-head">
          <div>
            <div class="tip">最终结果</div>
            <div class="big" :style="{ color: RISK_LEVEL[score.riskLevel]?.color }">{{ score.levelText }} · {{ score.totalScore }} 分</div>
            <div class="tip">规则版本 v{{ score.ruleVersion }} · 生长阶段 {{ GROWTH_STAGE[score.growthStage]?.label || score.growthStage }} · {{ formatTime(score.createTime) }}</div>
          </div>
          <el-tag :type="RISK_LEVEL[score.finalLevel]?.type" size="large">{{ RISK_LEVEL[score.finalLevel]?.label }}</el-tag>
        </div>
        <table class="score-tb">
          <thead><tr><th>因素</th><th>实测</th><th>适宜区间</th><th>偏离</th><th>权重</th><th>得分</th><th>贡献</th></tr></thead>
          <tbody>
            <tr v-for="it in score.items" :key="it.key">
              <td>{{ it.label }}</td>
              <td class="num">{{ it.value }}</td>
              <td class="num">{{ it.range }}</td>
              <td class="num">{{ it.deviation === undefined ? '—' : it.deviation + '%' }}</td>
              <td class="num">{{ it.weight }}%</td>
              <td><span class="score-bar"><i :style="{ width: Math.min(it.score, 100) + '%' }"></i></span><span class="num">{{ it.score }}</span></td>
              <td class="num">{{ it.contribution }}</td>
            </tr>
          </tbody>
        </table>
        <div class="block-t mt16">未满足条件</div>
        <div class="unmet">
          <el-tag v-for="u in score.unmet" :key="u.label" type="danger" effect="plain" size="small">{{ u.label }} {{ u.value }}（应为 {{ u.range }}）</el-tag>
          <span v-if="!(score.unmet || []).length" class="tip">全部指标均在适宜区间</span>
        </div>
        <div class="block-t mt16">规则建议</div>
        <div class="advice">{{ score.advice }}</div>
        <div class="block-t mt16">人工修正</div>
        <div class="flex">
          <el-select v-model="manual.level" placeholder="修正等级" style="width: 150px">
            <el-option v-for="(v, k) in RISK_LEVEL" :key="k" :label="v.label" :value="k" />
          </el-select>
          <el-input v-model="manual.reason" placeholder="修正原因（不少于 5 个字）" style="flex: 1" />
          <el-button type="primary" :loading="saving" @click="doManual">提交修正</el-button>
        </div>
        <div class="tip mt8">原始计算结果不会被覆盖，修正记录单独留痕，可在预警详情中查看。</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import http from '../api/http'
import { formatTime } from '../utils/format'
import { BATCH_STATUS, FEEDING_STATUS, GRADE, GROWTH_STAGE, HARVEST_STATUS, PERIOD, RISK_LEVEL, WARN_TYPE, WARNING_STATUS, WATER_LEVEL, statusOf } from '../utils/status'

const route = useRoute()
const router = useRouter()
const d = ref({})
const loading = ref(false)
const tab = ref('water')
const scoreDlg = ref(false)
const score = ref(null)
const saving = ref(false)
const manual = reactive({ level: '', reason: '' })

const feedCoefficient = computed(() => {
  const t = Number(d.value.feedTotal || 0)
  const grow = Number(d.value.currentStock || 0) - Number(d.value.stockQty || 0) * 1000
  if (!t || grow <= 0) return '—'
  return (t / grow).toFixed(2)
})

function n(v) { return v === null || v === undefined ? '—' : Number(v).toFixed(0) }
function reverse(list) { return [...(list || [])].reverse() }

async function load() {
  loading.value = true
  try { d.value = await http.get(`/api/aquaculture-water/${route.params.id}`) } finally { loading.value = false }
}
async function evaluate(row) {
  score.value = await http.post(`/api/aquaculture-water/evaluate?waterId=${row.id}`)
  manual.level = score.value.finalLevel
  manual.reason = ''
  scoreDlg.value = true
}
async function doManual() {
  if (!manual.reason || manual.reason.trim().length < 5) { ElMessage.warning('请填写不少于 5 个字的修正原因'); return }
  saving.value = true
  try {
    await http.post(`/api/rule/score/${score.value.scoreId}/manual`, { manualLevel: manual.level, manualReason: manual.reason.trim() })
    ElMessage.success('已记录人工修正')
    scoreDlg.value = false
    load()
  } finally { saving.value = false }
}
function goWater() { router.push({ path: '/water/new', query: { batchId: d.value.id } }) }

onMounted(load)
</script>

<style scoped>
.tabs :deep(.el-tabs__header) { padding: 0 18px; margin-bottom: 0; }
.tabs :deep(.el-tabs__nav-wrap:after) { height: 1px; }
.tabs :deep(.el-tabs__content) { padding: 4px 18px 16px; }
.score-head { display: flex; align-items: center; gap: 16px; padding: 14px 16px; background: #F6F8F7; border-radius: 8px; }
.score-head .big { font-size: 22px; font-weight: 600; margin: 4px 0; }
.score-tb { width: 100%; border-collapse: collapse; margin-top: 14px; font-size: 12.5px; }
.score-tb th { background: #F6F8F7; text-align: left; padding: 8px 10px; font-weight: 600; color: #3F5C62; }
.score-tb td { padding: 8px 10px; border-bottom: 1px solid var(--c-border-l); }
.unmet { display: flex; gap: 8px; flex-wrap: wrap; }
.advice { background: #F6F8F7; border-left: 3px solid var(--c-secondary); padding: 10px 14px; font-size: 12.5px; line-height: 22px; border-radius: 0 4px 4px 0; }
</style>
