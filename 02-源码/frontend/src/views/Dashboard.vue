<template>
  <div class="dash">
    <div class="page-head" style="margin-bottom: 0">
      <h1>养殖批次看板</h1>
      <span class="sub">近 {{ days }} 天 · 数据来自塘口日常监测与处置记录</span>
      <div class="actions">
        <el-radio-group v-model="days" size="small" @change="load">
          <el-radio-button :value="7">7 天</el-radio-button>
          <el-radio-button :value="30">30 天</el-radio-button>
          <el-radio-button :value="90">本季度</el-radio-button>
        </el-radio-group>
        <el-button size="small" :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="kpi-grid">
      <div v-for="c in cards" :key="c.name" class="kpi">
        <div class="lb">{{ c.name }}</div>
        <div class="v">{{ fmt(c.value) }}<small>{{ c.unit }}</small></div>
        <div class="df">{{ c.tip }}</div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="card"><div class="card-h"><h3>水质合格率与平均溶氧趋势</h3><span class="hint">点击查看水质记录</span></div><div ref="c1" class="ch" @click="go('/water')"></div></div>
      <div class="card"><div class="card-h"><h3>预警类型分布</h3></div><div ref="c2" class="ch" @click="go('/warnings')"></div></div>
      <div class="card"><div class="card-h"><h3>塘口存塘量（kg）</h3><span class="hint">点击进入塘口总览</span></div><div ref="c3" class="ch" @click="go('/ponds')"></div></div>
      <div class="card"><div class="card-h"><h3>风险分层结果分布</h3><span class="hint">创新点计算结果</span></div><div ref="c4" class="ch" @click="go('/rules')"></div></div>
      <div class="card"><div class="card-h"><h3>处置人处理量</h3></div><div ref="c5" class="ch"></div></div>
      <div class="card"><div class="card-h"><h3>各类预警平均处置时长（小时）</h3></div><div ref="c6" class="ch"></div></div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import http from '../api/http'

const router = useRouter()
const days = ref(30)
const cards = ref([])
const c1 = ref(null); const c2 = ref(null); const c3 = ref(null)
const c4 = ref(null); const c5 = ref(null); const c6 = ref(null)
let charts = []

const COLORS = ['#284B63', '#3C6E71', '#355A6B', '#7FA3A9', '#A9761F', '#AD2E3A']
const base = {
  grid: { left: 40, right: 16, top: 26, bottom: 24 },
  tooltip: { trigger: 'axis' },
  textStyle: { fontSize: 11, color: '#5B7176' }
}

function fmt(v) {
  if (v === null || v === undefined) return '-'
  return Number.isInteger(v) ? v : Number(v).toFixed(1)
}
function go(p) { router.push(p) }

function init(el, option) {
  const c = echarts.init(el)
  c.setOption(option)
  charts.push(c)
}

async function load() {
  const d = await http.get('/api/dashboard', { params: { days: days.value } })
  cards.value = d.cards || []
  await nextTick()
  paint(d)
  // 布局（flex 收缩）稳定后再校准一次，避免图表按初始尺寸绘制导致底部被裁
  setTimeout(() => charts.forEach((c) => c.resize()), 160)
}

function paint(d) {
  charts.forEach((c) => c.dispose())
  charts = []
  const t = d.trend || { labels: [], qualified: [], dissolvedOxygen: [] }
  init(c1.value, {
    ...base,
    legend: { right: 0, top: 0, itemWidth: 10, itemHeight: 8, textStyle: { fontSize: 11 } },
    xAxis: { type: 'category', data: t.labels, axisLine: { lineStyle: { color: '#E2E8E6' } }, axisLabel: { fontSize: 11 } },
    yAxis: [
      { type: 'value', max: 100, name: '%', nameTextStyle: { fontSize: 10 }, splitLine: { lineStyle: { color: '#F0F4F2' } } },
      { type: 'value', name: 'mg/L', nameTextStyle: { fontSize: 10 }, splitLine: { show: false } }
    ],
    series: [
      { name: '合格率', type: 'line', smooth: true, data: t.qualified, itemStyle: { color: '#284B63' }, areaStyle: { color: 'rgba(40,75,99,.10)' }, symbolSize: 5 },
      { name: '平均溶氧', type: 'line', yAxisIndex: 1, smooth: true, data: t.dissolvedOxygen, itemStyle: { color: '#3C6E71' }, symbolSize: 5 }
    ]
  })
  init(c2.value, {
    ...base, tooltip: { trigger: 'item' },
    legend: { type: 'scroll', orient: 'vertical', right: 0, top: 'middle', itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 11 } },
    series: [{ type: 'pie', radius: ['46%', '70%'], center: ['38%', '50%'], data: d.warnType || [], label: { show: false }, itemStyle: { borderColor: '#fff', borderWidth: 2 }, color: COLORS }]
  })
  init(c3.value, {
    ...base,
    xAxis: { type: 'category', data: (d.stock || []).map((x) => x.name), axisLabel: { fontSize: 10, interval: 0 }, axisLine: { lineStyle: { color: '#E2E8E6' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#F0F4F2' } } },
    series: [{ type: 'bar', barWidth: 16, data: (d.stock || []).map((x) => x.value), itemStyle: { color: '#3C6E71', borderRadius: [3, 3, 0, 0] } }]
  })
  init(c4.value, {
    ...base, tooltip: { trigger: 'item' },
    legend: { bottom: 0, itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['40%', '64%'], center: ['50%', '44%'], label: { formatter: '{b}\n{c}', fontSize: 11 },
      data: (d.riskDist || []).map((x, i) => ({ ...x, itemStyle: { color: ['#2B7A5A', '#A9761F', '#AD2E3A'][i] || '#7FA3A9' } }))
    }]
  })
  init(c5.value, {
    ...base,
    grid: { left: 60, right: 24, top: 16, bottom: 20 },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#F0F4F2' } } },
    yAxis: { type: 'category', data: (d.handler || []).map((x) => x.name), axisLine: { lineStyle: { color: '#E2E8E6' } } },
    series: [{ type: 'bar', barWidth: 12, data: (d.handler || []).map((x) => x.value), itemStyle: { color: '#355A6B', borderRadius: [0, 3, 3, 0] }, label: { show: true, position: 'right', fontSize: 11 } }]
  })
  init(c6.value, {
    ...base,
    xAxis: { type: 'category', data: (d.efficiency || []).map((x) => x.name), axisLabel: { fontSize: 10, interval: 0 }, axisLine: { lineStyle: { color: '#E2E8E6' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#F0F4F2' } } },
    series: [{ type: 'bar', barWidth: 14, data: (d.efficiency || []).map((x) => x.value), itemStyle: { color: '#7FA3A9', borderRadius: [3, 3, 0, 0] } }]
  })
  charts.forEach((c) => c.resize())
}

function onResize() { charts.forEach((c) => c.resize()) }
onMounted(() => {
  load()
  window.addEventListener('resize', onResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  charts.forEach((c) => c.dispose())
  charts = []
})
</script>

<style scoped>
/* 1440x900 内一屏：6 指标 + 6 图表，不出现纵向滚动 */
.dash { height: calc(100vh - 56px); box-sizing: border-box; padding: 10px 20px 12px; display: flex; flex-direction: column; gap: 10px; overflow: hidden; }
.dash .kpi-grid { min-height: 0; }
.dash .kpi { padding: 8px 12px; min-width: 0; }
.dash .kpi .lb { font-size: 12px; white-space: nowrap; }
.dash .kpi .v { font-size: 20px; line-height: 26px; margin-top: 2px; }
.dash .kpi .df { font-size: 11px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.chart-grid { flex: 1; min-height: 0; display: grid; grid-template-columns: repeat(3, 1fr); grid-template-rows: minmax(0, 1fr) minmax(0, 1fr); gap: 10px; }
.chart-grid .card { display: flex; flex-direction: column; min-height: 0; min-width: 0; overflow: hidden; }
.chart-grid .card + .card { margin-top: 0; }
.chart-grid .card-h { padding: 7px 12px; }
.chart-grid .card-h h3 { font-size: 13px; }
.chart-grid .card-h h3:before { top: 2px; bottom: 2px; }
.chart-grid .card-h .hint { font-size: 11px; }
.ch { flex: 1; min-height: 0; cursor: pointer; }
</style>
