<template>
  <div class="page-grid">
    <div class="stats-grid">
      <StatCard label="任务总数" :value="tasks.length" />
      <StatCard label="已完成" :value="statusCount('COMPLETED')" />
      <StatCard label="处理中" :value="statusCount('PROCESSING')" />
      <StatCard label="其他状态" :value="otherStatusCount" />
    </div>

    <div class="panel">
      <div class="section-title">
        <div>
          <h2>任务列表</h2>
        </div>
        <el-button type="primary" @click="loadTasks">刷新任务</el-button>
      </div>
      <el-table :data="tasks" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="taskType" label="任务类型" min-width="160" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="contextLiteratureIds" label="文献范围" min-width="140" />
        <el-table-column prop="resultSummary" label="结果摘要" min-width="280" show-overflow-tooltip />
        <el-table-column label="时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="detailVisible" title="任务详情" size="50%">
      <template v-if="taskDetail">
        <div class="task-detail">
          <div class="stats-grid">
            <StatCard label="任务 ID" :value="taskDetail.id" />
            <StatCard label="任务类型" :value="taskDetail.taskType" />
            <StatCard label="任务状态" :value="taskDetail.status" />
            <StatCard label="创建时间" :value="formatDateTime(taskDetail.createdAt)" />
          </div>

          <template v-if="isReportTask">
            <div class="panel task-detail__panel">
              <div class="section-title">
                <div>
                  <h2>报告正文</h2>
                </div>
              </div>
              <div class="report-sections">
                <div
                  v-for="section in parsedReportSections"
                  :key="section.title + section.content.slice(0, 12)"
                  class="report-section"
                >
                  <div class="report-section__title">{{ section.title }}</div>
                  <div class="report-section__content">{{ section.content }}</div>
                </div>
              </div>
            </div>

            <div class="panel task-detail__panel">
              <div class="section-title">
                <div>
                  <h2>报告参数</h2>
                </div>
              </div>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="研究问题">{{ taskDetail.inputText || '-' }}</el-descriptions-item>
                <el-descriptions-item label="分析焦点">{{ taskDetail.analysisFocus || '-' }}</el-descriptions-item>
                <el-descriptions-item label="文献范围">{{ taskDetail.contextLiteratureIds || '-' }}</el-descriptions-item>
                <el-descriptions-item label="引用">{{ taskDetail.citations || '-' }}</el-descriptions-item>
              </el-descriptions>
            </div>

            <div class="panel task-detail__panel">
              <div class="section-title">
                <div>
                  <h2>证据片段</h2>
                </div>
              </div>
              <el-table :data="taskDetail.evidence" stripe>
                <el-table-column prop="title" label="文献" min-width="180" show-overflow-tooltip />
                <el-table-column prop="score" label="得分" width="90" />
                <el-table-column prop="evidenceLocator" label="定位" min-width="140" />
                <el-table-column prop="snippet" label="片段" min-width="260" show-overflow-tooltip />
              </el-table>
            </div>

            <div class="panel task-detail__panel">
              <div class="section-title">
                <div>
                  <h2>关联指标</h2>
                </div>
              </div>
              <el-table :data="taskDetail.indicators" stripe>
                <el-table-column prop="indicatorName" label="指标" min-width="140" />
                <el-table-column prop="category" label="类别" width="120" />
                <el-table-column prop="timeWindow" label="时间窗口" min-width="120" />
                <el-table-column prop="cohort" label="队列" min-width="140" />
                <el-table-column prop="reviewStatus" label="复核状态" min-width="120" />
              </el-table>
            </div>
          </template>

          <div v-else class="panel task-detail__panel">
            <div class="section-title">
              <div>
                <h2>原始详情</h2>
              </div>
            </div>
            <JsonPanel :value="taskDetail" />
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatCard from '@/components/StatCard.vue'
import JsonPanel from '@/components/JsonPanel.vue'
import { getTaskDetail, listTasks, deleteTask } from '@/api/modules/tasks'
import type { AnalysisTaskDetailView, AnalysisTaskView } from '@/types/platform'
import { formatDateTime } from '@/utils/format'

interface ReportSection {
  title: string
  content: string
}

const tasks = ref<AnalysisTaskView[]>([])
const taskDetail = ref<AnalysisTaskDetailView | null>(null)
const detailVisible = ref(false)

const otherStatusCount = computed(
  () => tasks.value.filter((item) => !['COMPLETED', 'PROCESSING'].includes(item.status)).length
)

const isReportTask = computed(() => taskDetail.value?.taskType === 'REPORT')

const parsedReportSections = computed<ReportSection[]>(() => {
  const raw = taskDetail.value?.resultSummary?.replace(/\r/g, '').trim()
  if (!raw) {
    return []
  }

  const blocks = raw.split(
    /\n(?=(?:[-*]\s*)?(?:Research conclusion|Evidence synthesis|Indicator interpretation|Risks and limitations|Suggested next validation step|研究结论|证据综合|指标解读|风险与局限|下一步验证建议))/i
  )

  const sections = blocks
    .map((block) => block.trim())
    .filter(Boolean)
    .map((block) => {
      const lines = block.split('\n').map((line) => line.trim()).filter(Boolean)
      const firstLine = lines[0] ?? '报告内容'
      const normalizedTitle = firstLine.replace(/^[-*]\s*/, '').replace(/[:：]\s*$/, '')
      const content = lines.slice(1).join('\n').trim() || block
      return {
        title: normalizedTitle,
        content
      }
    })

  return sections.length ? sections : [{ title: '报告摘要', content: raw }]
})

function statusCount(status: string) {
  return tasks.value.filter((item) => item.status === status).length
}

async function loadTasks() {
  try {
    tasks.value = await listTasks()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

async function openDetail(id: number) {
  try {
    taskDetail.value = await getTaskDetail(id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该任务吗？此操作不可恢复。', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteTask(id)
    ElMessage.success('删除成功')
    await loadTasks()
  } catch (error) {
    if ((error as string) !== 'cancel') {
      ElMessage.error((error as Error).message)
    }
  }
}

onMounted(loadTasks)
</script>

<style scoped>
.task-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-detail__panel {
  margin-top: 0;
}

.report-sections {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.report-section {
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  background: var(--el-fill-color-blank);
}

.report-section__title {
  margin-bottom: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.report-section__content {
  line-height: 1.75;
  white-space: pre-wrap;
  color: var(--el-text-color-regular);
}
</style>
