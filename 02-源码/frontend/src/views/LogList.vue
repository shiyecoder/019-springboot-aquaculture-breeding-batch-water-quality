<template>
  <div class="page">
    <div class="page-head">
      <h1>操作日志</h1>
      <span class="sub">共 {{ total }} 条 · 状态流转、审批与作废均留痕</span>
    </div>

    <div class="filters">
      <el-input v-model="q.keyword" placeholder="操作人 / 模块 / 业务编号" clearable @keyup.enter="search" />
      <div class="tail">
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" size="small">
        <el-table-column label="时间" width="160" class-name="nowrap"><template #default="{ row }">{{ formatTime(row.createTime) }}</template></el-table-column>
        <el-table-column prop="userName" label="操作人" width="100" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="action" label="动作" min-width="180" />
        <el-table-column prop="bizNo" label="业务编号" width="150"><template #default="{ row }">{{ row.bizNo || '—' }}</template></el-table-column>
        <el-table-column prop="detail" label="详情" min-width="260" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="120" />
        <template #empty><el-empty description="暂无日志记录" :image-size="70" /></template>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="q.page" v-model:page-size="q.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="load" @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import http from '../api/http'
import { formatTime } from '../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ page: 1, size: 10, keyword: '' })

async function load() {
  loading.value = true
  try {
    const d = await http.get('/api/system/log/page', { params: { page: q.page, size: q.size, keyword: q.keyword || undefined } })
    rows.value = d.list || []
    total.value = d.total || 0
  } finally { loading.value = false }
}
function search() { q.page = 1; load() }
function reset() { q.keyword = ''; search() }
onMounted(load)
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; padding: 12px 18px; }
</style>
