<template>
  <div class="up">
    <el-upload
      class="box"
      :show-file-list="false"
      :http-request="doUpload"
      :before-upload="beforeUpload"
      accept="image/*"
      drag
    >
      <img v-if="modelValue" :src="modelValue" class="pic" />
      <div v-else class="ph">
        <el-icon><Plus /></el-icon>
        <span>点击或拖拽上传</span>
      </div>
    </el-upload>
    <div class="ext">
      <el-button v-if="modelValue" link type="danger" size="small" @click="$emit('update:modelValue', '')">移除图片</el-button>
      <span class="tip">JPG/PNG，≤5MB，保存到项目 uploads 目录</span>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import http from '../api/http'

/** 通用图片上传：所有业务图片统一走此组件，禁止外链与手填 URL */
const props = defineProps({
  modelValue: { type: String, default: '' },
  type: { type: String, default: 'images' }
})
const emit = defineEmits(['update:modelValue'])

function beforeUpload(file) {
  if (!/^image\//.test(file.type)) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片不能超过 5MB')
    return false
  }
  return true
}

async function doUpload({ file }) {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('type', props.type)
  const res = await http.post('/api/file/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  emit('update:modelValue', res.path)
  ElMessage.success('上传成功')
}
</script>

<style scoped>
.up { display: flex; flex-direction: column; gap: 6px; }
.box :deep(.el-upload-dragger) { width: 148px; height: 100px; padding: 0; display: grid; place-items: center; border-radius: 6px; border-color: #E2E8E6; }
.pic { width: 146px; height: 98px; object-fit: cover; border-radius: 5px; }
.ph { display: flex; flex-direction: column; align-items: center; gap: 6px; color: #8FA3A8; font-size: 12px; }
.ph .el-icon { font-size: 20px; }
.ext { display: flex; align-items: center; gap: 10px; }
</style>
