<template>
  <div class="page-grid dashboard-page">
    <div class="stats-grid dashboard-stats">
      <StatCard label="文献总数" :value="literatures.length" hint="已导入平台的文献数量" />
      <StatCard label="任务总数" :value="tasks.length" hint="报告、图谱和流程任务总数" />
      <StatCard label="图谱节点" :value="graph.nodes.length" hint="当前知识图谱中的节点数量" />
      <StatCard label="待复核指标" :value="indicatorSummary.pendingCount" hint="还需要人工确认的指标数量" />
      <StatCard label="同步异常文献" :value="vectorSyncWarnings.length" hint="向量同步异常或未完成的文献" />
      <StatCard v-if="userStore.isAdmin" label="系统用户" :value="users.length" :hint="healthLabel" />
    </div>

    <div class="panel dashboard-toolbar">
      <div class="section-title">
        <div>
          <h2>常用入口</h2>
          <p>从这里快速进入最常用的工作页面。</p>
        </div>
        <el-button text type="primary" @click="loadDashboard">刷新概览</el-button>
      </div>
      <div class="dashboard-actions">
        <el-button type="primary" @click="router.push('/literatures')">文献管理</el-button>
        <el-button @click="router.push('/extraction/indicators')">指标抽取</el-button>
        <el-button @click="router.push('/analysis')">分析报告</el-button>
        <el-button @click="router.push('/graph')">知识图谱</el-button>
        <el-button @click="router.push('/workflow')">流程演示</el-button>
        <el-button @click="router.push('/tasks')">任务中心</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="section-title">
        <div>
          <h2>服务状态</h2>
          <p>查看数据库、Redis、Milvus 和 Embedding 的运行情况。</p>
        </div>
      </div>

      <div class="dashboard-kv">
        <div class="dashboard-kv__item">
          <span class="dashboard-kv__label">平台状态</span>
          <span class="dashboard-kv__value">{{ health.status || '-' }}</span>
        </div>
        <div class="dashboard-kv__item">
          <span class="dashboard-kv__label">服务名</span>
          <span class="dashboard-kv__value">{{ health.service || '-' }}</span>
        </div>
        <div class="dashboard-kv__item">
          <span class="dashboard-kv__label">检查时间</span>
          <span class="dashboard-kv__value">{{ formatDateTime(health.timestamp) }}</span>
        </div>
        <div class="dashboard-kv__item">
          <span class="dashboard-kv__label">异常组件数</span>
          <span class="dashboard-kv__value">{{ degradedCount }}</span>
        </div>
        <div class="dashboard-kv__item">
          <span class="dashboard-kv__label">图谱关系数</span>
          <span class="dashboard-kv__value">{{ graph.edges.length }}</span>
        </div>
      </div>

      <el-table :data="health.components || []" stripe style="margin-top: 14px">
        <el-table-column prop="name" label="组件" width="120" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="latencyMs" label="耗时(ms)" width="110" />
        <el-table-column prop="detail" label="说明" min-width="260" show-overflow-tooltip />
      </el-table>

      <el-collapse class="dashboard-collapse">
        <el-collapse-item title="查看原始健康数据">
          <JsonPanel :value="health" />
        </el-collapse-item>
      </el-collapse>
    </div>

    <div class="panel">
      <div class="section-title">
        <div>
          <h2>最近任务</h2>
          <p>查看最近生成的任务和创建时间。</p>
        </div>
        <el-button text type="primary" @click="router.push('/tasks')">查看全部</el-button>
      </div>
      <el-table :data="recentTasks" stripe>
        <el-table-column prop="taskType" label="任务类型" min-width="120" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="contextLiteratureIds" label="文献范围" min-width="120" />
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <div class="panel">
      <div class="section-title">
        <div>
          <h2>指标复核概览</h2>
          <p>快速了解当前指标复核进度。</p>
        </div>
      </div>
      <div class="stats-grid dashboard-inner-stats">
        <StatCard label="总指标数" :value="indicatorSummary.totalCount" />
        <StatCard label="待复核" :value="indicatorSummary.pendingCount" />
        <StatCard label="已确认" :value="indicatorSummary.confirmedCount" />
        <StatCard label="已拒绝" :value="indicatorSummary.rejectedCount" />
      </div>
    </div>

    <div class="panel">
      <div class="section-title">
        <div>
          <h2>向量同步预警</h2>
          <p>这里展示尚未完成向量同步的文献。</p>
        </div>
      </div>
      <el-table :data="vectorSyncWarnings" stripe max-height="300">
        <el-table-column prop="title" label="文献标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="vectorSyncStatus" label="同步状态" width="120" />
        <el-table-column prop="vectorSyncedChunkCount" label="已同步向量数" width="130" />
        <el-table-column prop="vectorSyncDetail" label="说明" min-width="220" show-overflow-tooltip />
      </el-table>
    </div>

    <div v-if="userStore.isAdmin" class="panel">
      <div class="section-title">
        <div>
          <h2>最近操作日志</h2>
          <p>仅管理员可见。</p>
        </div>
        <el-button text type="primary" @click="router.push('/logs')">查看全部</el-button>
      </div>
      <el-table :data="recentLogs" stripe max-height="300">
        <el-table-column prop="operatorEmail" label="操作人" min-width="170" />
        <el-table-column prop="actionType" label="动作" width="110" />
        <el-table-column prop="resourceType" label="资源" width="110" />
        <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip />
        <el-table-column label="时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <div class="panel">
      <div class="section-title">
        <div>
          <h2>知识图谱预览</h2>
          <p>展示当前平台已经构建出的知识图谱关系。</p>
        </div>
        <el-button text type="primary" @click="router.push('/graph')">查看完整图谱</el-button>
      </div>
      <GraphChart v-if="graph.nodes.length" :graph="graph" />
      <el-empty v-else description="暂无图谱数据" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import StatCard from '@/components/StatCard.vue'
import JsonPanel from '@/components/JsonPanel.vue'
import GraphChart from '@/components/GraphChart.vue'
import { getIndicatorReviewSummary } from '@/api/modules/extractions'
import { getKnowledgeGraph } from '@/api/modules/graph'
import { getHealth } from '@/api/modules/health'
import { listLiteratures } from '@/api/modules/literatures'
import { listLogs } from '@/api/modules/logs'
import { listTasks } from '@/api/modules/tasks'
import { listUsers } from '@/api/modules/users'
import { useUserStore } from '@/stores/user'
import type {
  AnalysisTaskView,
  HealthInfo,
  IndicatorReviewSummaryView,
  KnowledgeGraphResponse,
  LiteratureView,
  OperationLogView
} from '@/types/platform'
import type { UserView } from '@/types/user'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()

const health = ref<HealthInfo>({ status: '', service: '', timestamp: '', components: [] })
const literatures = ref<LiteratureView[]>([])
const tasks = ref<AnalysisTaskView[]>([])
const logs = ref<OperationLogView[]>([])
const users = ref<UserView[]>([])
const graph = ref<KnowledgeGraphResponse>({ nodes: [], edges: [] })
const indicatorSummary = ref<IndicatorReviewSummaryView>({
  totalCount: 0,
  pendingCount: 0,
  confirmedCount: 0,
  rejectedCount: 0
})

const recentTasks = computed(() => tasks.value.slice(0, 5))
const recentLogs = computed(() => logs.value.slice(0, 6))
const healthLabel = computed(() => (health.value.status ? `服务状态：${health.value.status}` : '等待加载'))
const degradedCount = computed(() => (health.value.components || []).filter((item) => item.status !== 'UP').length)
const vectorSyncWarnings = computed(() =>
  literatures.value.filter((item) => item.vectorSyncStatus && item.vectorSyncStatus !== 'SYNCED')
)

async function loadDashboard() {
  try {
    const [healthData, literatureData, taskData, graphData, indicatorSummaryData] = await Promise.all([
      getHealth(),
      listLiteratures(),
      listTasks(),
      getKnowledgeGraph(),
      getIndicatorReviewSummary()
    ])

    health.value = healthData
    literatures.value = literatureData
    tasks.value = taskData
    graph.value = graphData
    indicatorSummary.value = indicatorSummaryData

    if (userStore.isAdmin) {
      try {
        const [logData, userData] = await Promise.all([listLogs(), listUsers()])
        logs.value = logData
        users.value = userData
      } catch {
        logs.value = []
        users.value = []
      }
    }
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-page {
  gap: 16px;
}

.dashboard-stats {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.dashboard-toolbar {
  padding-bottom: 14px;
}

.dashboard-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.dashboard-kv {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
}

.dashboard-kv__item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  background: var(--el-fill-color-blank);
}

.dashboard-kv__label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.dashboard-kv__value {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.dashboard-inner-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.dashboard-collapse {
  margin-top: 14px;
}

@media (max-width: 900px) {
  .dashboard-inner-stats {
    grid-template-columns: 1fr;
  }
}
</style>
