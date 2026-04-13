<template>
  <div class="page-grid">
    <div class="panel">
      <div class="section-title">
        <div>
          <h2>日志筛选</h2>
        </div>
      </div>
      <div class="table-toolbar">
        <el-input v-model="operatorEmail" placeholder="输入邮箱后按查询" clearable style="width: 320px" @keyup.enter="loadLogs" />
        <el-button type="primary" @click="loadLogs">查询日志</el-button>
      </div>
    </div>

    <div class="stats-grid">
      <StatCard label="日志总数" :value="logs.length" />
      <StatCard label="登录操作" :value="actionCount('LOGIN')" />
      <StatCard label="文献处理" :value="literatureActionCount" />
      <StatCard label="用户操作" :value="actionCount('UPDATE_ROLE') + actionCount('CREATE')" />
    </div>

    <div class="panel">
      <div class="section-title">
        <div>
          <h2>操作日志列表</h2>
        </div>
      </div>
      <el-table :data="logs" stripe>
        <el-table-column prop="operatorEmail" label="操作者" min-width="180" />
        <el-table-column prop="actionType" label="动作类型" width="140" />
        <el-table-column prop="resourceType" label="资源类型" width="140" />
        <el-table-column prop="resourceId" label="资源 ID" width="110" />
        <el-table-column prop="detail" label="详情" min-width="320" show-overflow-tooltip />
        <el-table-column label="时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatCard from '@/components/StatCard.vue'
import { listLogs } from '@/api/modules/logs'
import type { OperationLogView } from '@/types/platform'
import { formatDateTime } from '@/utils/format'

const operatorEmail = ref('')
const logs = ref<OperationLogView[]>([])

const literatureActionCount = computed(
  () => logs.value.filter((item) => item.resourceType === 'LITERATURE').length
)

function actionCount(actionType: string) {
  return logs.value.filter((item) => item.actionType === actionType).length
}

async function loadLogs() {
  try {
    logs.value = await listLogs(operatorEmail.value || undefined)
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

onMounted(loadLogs)
</script>
