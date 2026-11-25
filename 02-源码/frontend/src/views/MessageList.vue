<template>
  <div class="page">
    <div class="page-head">
      <h1>消息中心</h1>
      <span class="sub">共 {{ total }} 条 · 未读 {{ unread }} 条 · 待审批、超时、退回、待验收等事件在此汇聚</span>
      <div class="actions">
        <el-radio-group v-model="q.unreadOnly" size="small" @change="search">
          <el-radio-button :value="false">全部</el-radio-button>
          <el-radio-button :value="true">未读</el-radio-button>
        </el-radio-group>
        <el-button size="small" :icon="Check" @click="readAll">全部已读</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" @row-click="open">
        <el-table-column width="52" align="center">
          <template #default="{ row }"><span class="dot" :class="{ un: !row.isRead }"></span></template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="280">
          <template #default="{ row }"><span :class="{ bold: !row.isRead }">{{ row.title }}</span></template>
        </el-table-column>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">{{ BIZ[row.bizType] || row.bizType || '系统' }}</template>
        </el-table-column>
        <el-table-column prop="bizNo" label="关联单据" width="160"><template #default="{ row }">{{ row.bizNo || '—' }}</template></el-table-column>
        <el-table-column prop="content" label="内容" min-width="320" show-overflow-tooltip />
        <el-table-column label="时间" width="160" class-name="nowrap"><template #default="{ row }">{{ formatTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="状态" width="88">
          <template #default="{ row }"><el-tag size="small" :type="row.isRead ? 'info' : 'danger'">{{ row.isRead ? '已读' : '未读' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.isRead" link type="primary" @click.stop="mark(row)">标为已读</el-button>
            <el-button v-if="row.bizType === 'WARNING'" link type="primary" @click.stop="$router.push(`/warnings/${row.bizId}`)">前往</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无消息" :image-size="70" /></template>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="load" @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import http from '../api/http'
import { formatTime } from '../utils/format'

const BIZ = { WARNING: '水质预警', DISEASE: '病害用药', HARVEST: '起捕审批', FEEDING: '投喂任务', WATER: '水质检测', STOCK: '库存提醒', REPORT: '统计提醒', SYSTEM: '系统通知' }
const rows = ref([])
const total = ref(0)
const unread = ref(0)
const loading = ref(false)
const q = reactive({ page: 1, size: 10, unreadOnly: false })

async function load() {
  loading.value = true
  try {
    const d = await http.get('/api/message/page', { params: { page: q.page, size: q.size, unreadOnly: q.unreadOnly || undefined } })
    rows.value = d.list || []
    total.value = d.total || 0
    const u = await http.get('/api/message/unread')
    unread.value = u.count || 0
  } finally { loading.value = false }
}
function search() { q.page = 1; load() }
async function mark(row) {
  await http.post(`/api/message/${row.id}/read`)
  load()
}
async function readAll() {
  await http.post('/api/message/read-all')
  ElMessage.success('已全部标记为已读')
  load()
}
function open(row) {
  if (!row.isRead) mark(row)
}

onMounted(load)
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; padding: 12px 18px; }
.dot { width: 7px; height: 7px; border-radius: 50%; background: #D6E0DD; display: inline-block; }
.dot.un { background: var(--c-danger); }
.bold { font-weight: 600; }
</style>
