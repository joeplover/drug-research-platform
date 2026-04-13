<template>
  <div class="page-grid workflow-page">
    <div class="panel" v-loading="loadingPage">
      <div class="section-title">
        <div>
          <div class="section-title__main">
            <h2>统一科研流程工作台</h2>
            <HelpTip title="这个页面怎么用">
              如果你不知道下一步做什么，只看“下一步建议”这张卡片就可以。<br />
              这个页面会告诉你当前文献已经处理到哪一步。<br />
              你也可以直接点“一键完成剩余步骤”。
            </HelpTip>
          </div>
        </div>
        <div class="action-row">
          <el-button :disabled="isBusy" @click="refreshAll">刷新状态</el-button>
          <el-button type="primary" @click="router.push('/literatures')">导入新文献</el-button>
        </div>
      </div>

      <el-form label-position="top" class="form-grid">
        <el-form-item class="full-span">
          <template #label>
            <span class="label-with-tip">
              当前文献
              <HelpTip title="这里该怎么选">
                这里选择你现在要处理的那一篇文献。<br />
                工作台下面的状态、建议和按钮都会跟着这篇文献变化。<br />
                普通使用一次只看 1 篇文献就够了。
              </HelpTip>
            </span>
          </template>
          <el-select
            v-model="selectedLiteratureId"
            placeholder="请选择一篇文献"
            style="width: 100%"
            :disabled="isBusy"
            @change="handleLiteratureChange"
          >
            <el-option v-for="item in literatures" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-alert
        v-if="currentActionLabel"
        style="margin-top: 10px"
        type="warning"
        :closable="false"
        :title="currentActionLabel"
        :description="currentActionDescription"
      />

      <div style="margin-top: 16px">
        <el-progress
          :percentage="progressPercent"
          :status="!isBusy && progressPercent === 100 ? 'success' : undefined"
          :stroke-width="18"
        />
      </div>

      <el-steps :active="activeStep" finish-status="success" simple style="margin-top: 16px">
        <el-step title="1 选中文献" />
        <el-step title="2 文献分块" />
        <el-step title="3 文献向量化" />
        <el-step title="4 指标抽取" />
        <el-step title="5 人工复核" />
        <el-step title="6 生成报告" />
      </el-steps>
    </div>

    <div class="split-grid workflow-grid">
      <div class="panel">
        <div class="section-title">
          <div>
            <h2>下一步建议</h2>
          </div>
        </div>

        <div class="workflow-step-card workflow-step-card--highlight">
          <div class="workflow-step-card__title">{{ nextActionTitle }}</div>
          <div class="workflow-step-card__meta">{{ nextActionDescription }}</div>
          <div class="action-row" style="margin-top: 16px">
            <el-button type="primary" :loading="isBusy" :disabled="!selectedLiteratureId || isBusy" @click="runNextAction">
              {{ nextActionButtonText }}
            </el-button>
            <el-button :disabled="!selectedLiteratureId || isBusy" @click="openSelectedLiterature">查看文献详情</el-button>
          </div>
        </div>

        <div class="workflow-step-card" :class="stepCardClass(hasChunks)">
          <div class="workflow-step-card__title">步骤一：文献分块</div>
          <div class="workflow-step-card__meta">
            {{ hasChunks ? `已完成，当前共有 ${vectorStatus?.chunkCount ?? 0} 个分块。` : '还没有分块。系统需要先把文献切成可处理的片段。' }}
          </div>
          <div class="action-row" style="margin-top: 16px">
            <el-button :loading="runningChunk" :disabled="!selectedLiteratureId || isBusy" @click="handleChunk">执行分块</el-button>
            <el-button :disabled="!selectedLiteratureId || isBusy" @click="openSelectedLiterature">查看分块详情</el-button>
          </div>
        </div>

        <div class="workflow-step-card" :class="stepCardClass(hasVectors)">
          <div class="workflow-step-card__title">步骤二：文献向量化</div>
          <div class="workflow-step-card__meta">
            {{
              hasVectors
                ? `已完成，当前向量状态为 ${vectorStatus?.milvusStatus || 'UNKNOWN'}。`
                : '还没有完成向量化。完成后系统才更适合做语义检索和证据召回。'
            }}
          </div>
          <div class="action-row" style="margin-top: 16px">
            <el-button type="primary" :loading="runningVectorize" :disabled="!selectedLiteratureId || isBusy" @click="handleVectorize">执行向量化</el-button>
            <el-button :disabled="!selectedLiteratureId || isBusy" @click="openSelectedLiterature">查看状态</el-button>
          </div>
        </div>

        <div class="workflow-step-card" :class="stepCardClass(hasIndicators)">
          <div class="workflow-step-card__title">步骤三：指标抽取</div>
          <div class="workflow-step-card__meta">
            {{
              hasIndicators
                ? `已抽取 ${indicators.length} 条指标，其中待复核 ${pendingIndicatorCount} 条。`
                : '还没有抽取指标。完成后你才能看到结构化的疗效、安全性和时间点信息。'
            }}
          </div>
          <div class="action-row" style="margin-top: 16px">
            <el-button :loading="runningExtraction" :disabled="!selectedLiteratureId || isBusy" @click="handleExtract">执行抽取</el-button>
            <el-button :disabled="!selectedLiteratureId || isBusy" @click="openExtractionPage">进入复核页面</el-button>
          </div>
        </div>

        <div class="workflow-step-card" :class="stepCardClass(hasReviewCompletion)">
          <div class="workflow-step-card__title">步骤四：人工复核</div>
          <div class="workflow-step-card__meta">
            {{
              hasReviewCompletion
                ? '当前抽取指标都已经完成复核。'
                : hasIndicators
                  ? `当前还有 ${pendingIndicatorCount} 条指标等待人工确认。`
                  : '需要先完成指标抽取，之后才能做人工复核。'
            }}
          </div>
          <div class="action-row" style="margin-top: 16px">
            <el-button :disabled="!selectedLiteratureId || isBusy" @click="openExtractionPage">进入复核页面</el-button>
          </div>
        </div>

        <div class="workflow-step-card" :class="stepCardClass(hasReport)">
          <div class="workflow-step-card__title">步骤五：分析报告</div>
          <div class="workflow-step-card__meta">
            {{
              hasReport
                ? `已生成 ${reportTasks.length} 个报告任务，你可以继续查看最新结果。`
                : '当前还没有生成报告。建议在抽取和复核后继续执行。'
            }}
          </div>
          <div class="action-row" style="margin-top: 16px">
            <el-button type="primary" :loading="runningReport" :disabled="!selectedLiteratureId || isBusy" @click="handleGenerateReport">生成报告</el-button>
            <el-button :disabled="!selectedLiteratureId || isBusy" @click="openAnalysisPage">进入分析页面</el-button>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="section-title">
          <div>
            <h2>当前文献状态</h2>
          </div>
        </div>

        <el-empty v-if="!selectedLiterature" description="暂无可用文献，请先导入文献。" />
        <template v-else>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="标题">{{ selectedLiterature.title }}</el-descriptions-item>
            <el-descriptions-item label="疾病领域">{{ selectedLiterature.diseaseArea || '-' }}</el-descriptions-item>
            <el-descriptions-item label="来源类型">{{ selectedLiterature.sourceType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="当前建议">{{ nextActionTitle }}</el-descriptions-item>
            <el-descriptions-item label="最后同步时间">{{ formatDateTime(selectedLiterature.vectorSyncedAt) }}</el-descriptions-item>
          </el-descriptions>

          <div class="stats-grid" style="margin-top: 16px">
            <StatCard label="分块数量" :value="vectorStatus?.chunkCount ?? 0" hint="文献切分后的片段数量" />
            <StatCard label="向量数量" :value="vectorStatus?.embeddedChunkCount ?? 0" hint="已生成语义向量的片段数" />
            <StatCard label="抽取指标数" :value="indicators.length" hint="当前文献的结构化指标总数" />
            <StatCard label="待复核指标" :value="pendingIndicatorCount" hint="还需要人工确认的指标数量" />
            <StatCard label="已确认指标" :value="confirmedIndicatorCount" hint="已经人工确认的指标数量" />
            <StatCard label="分析报告数" :value="reportTasks.length" hint="当前文献的报告任务数量" />
          </div>

          <div class="status-note">
            当前文献的处理重点：{{ nextActionDescription }}
          </div>

          <el-collapse style="margin-top: 16px">
            <el-collapse-item title="技术状态（高级）">
              <el-alert
                :title="`Milvus：${vectorStatus?.milvusStatus || 'UNKNOWN'} / Embedding：${vectorStatus?.embeddingMode || 'UNKNOWN'}`"
                :description="`分块 ${vectorStatus?.chunkCount ?? 0}，已生成向量 ${vectorStatus?.embeddedChunkCount ?? 0}`"
                type="info"
                :closable="false"
              />
            </el-collapse-item>
          </el-collapse>

          <div style="margin-top: 16px">
            <div class="section-title" style="margin-bottom: 10px">
              <div>
                <h2>最近报告任务</h2>
              </div>
            </div>
            <el-table :data="reportTasks.slice(0, 5)" stripe>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="status" label="状态" width="120" />
              <el-table-column label="创建时间" min-width="180">
                <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column prop="resultSummary" label="摘要" min-width="220" show-overflow-tooltip />
            </el-table>
            <el-alert
              v-if="latestReportTask"
              style="margin-top: 12px"
              type="success"
              :closable="false"
              :title="`已保存最近一次报告：任务 ${latestReportTask.id}`"
              :description="latestReportTask.resultSummary"
            />
          </div>

          <div class="action-row" style="margin-top: 16px">
            <el-button :loading="runningWorkflow" :disabled="!selectedLiteratureId || isBusy" @click="handleRunWorkflow">
              一键完成剩余步骤
            </el-button>
            <el-button :disabled="!selectedLiteratureId || isBusy" @click="router.push('/tasks')">查看任务中心</el-button>
            <el-button v-if="latestReportTask" :disabled="isBusy" @click="openLatestReport">查看最近报告</el-button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import StatCard from '@/components/StatCard.vue'
import HelpTip from '@/components/HelpTip.vue'
import { listLiteratures, getLiteratureVectorStatus, ingestLiterature, vectorizeLiterature } from '@/api/modules/literatures'
import { listLiteratureIndicators, runLiteratureExtraction } from '@/api/modules/extractions'
import { generateAnalysisReport } from '@/api/modules/analysis'
import { listTasks } from '@/api/modules/tasks'
import { processLiteratureWorkflow } from '@/api/modules/workflow'
import type { AnalysisTaskView, IndicatorView, LiteratureVectorStatusView, LiteratureView } from '@/types/platform'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

const literatures = ref<LiteratureView[]>([])
const selectedLiteratureId = ref<number>()
const vectorStatus = ref<LiteratureVectorStatusView | null>(null)
const indicators = ref<IndicatorView[]>([])
const tasks = ref<AnalysisTaskView[]>([])

const loadingPage = ref(false)
const runningChunk = ref(false)
const runningVectorize = ref(false)
const runningExtraction = ref(false)
const runningReport = ref(false)
const runningWorkflow = ref(false)
const currentActionLabel = ref('')
const currentActionDescription = ref('')

const isBusy = computed(
  () =>
    loadingPage.value ||
    runningChunk.value ||
    runningVectorize.value ||
    runningExtraction.value ||
    runningReport.value ||
    runningWorkflow.value
)

const selectedLiterature = computed(
  () => literatures.value.find((item) => item.id === selectedLiteratureId.value) ?? null
)

const hasChunks = computed(() => (vectorStatus.value?.chunkCount ?? 0) > 0)
const hasVectors = computed(() => {
  const chunkCount = vectorStatus.value?.chunkCount ?? 0
  const embeddedChunkCount = vectorStatus.value?.embeddedChunkCount ?? 0
  return chunkCount > 0 && embeddedChunkCount >= chunkCount
})
const hasIndicators = computed(() => indicators.value.length > 0)
const confirmedIndicatorCount = computed(() => indicators.value.filter((item) => item.reviewStatus === '已确认').length)
const rejectedIndicatorCount = computed(() => indicators.value.filter((item) => item.reviewStatus === '已拒绝').length)
const pendingIndicatorCount = computed(() => indicators.value.length - confirmedIndicatorCount.value - rejectedIndicatorCount.value)
const hasReviewCompletion = computed(() => hasIndicators.value && pendingIndicatorCount.value === 0)
const reportTasks = computed(() =>
  tasks.value.filter((item) => item.taskType === 'REPORT' && containsLiteratureId(item.contextLiteratureIds, selectedLiteratureId.value))
)
const latestReportTask = computed(() => reportTasks.value[0] ?? null)
const hasReport = computed(() => reportTasks.value.length > 0)

const activeStep = computed(() => {
  if (!selectedLiterature.value) return 0
  if (!hasChunks.value) return 1
  if (!hasVectors.value) return 2
  if (!hasIndicators.value) return 3
  if (!hasReviewCompletion.value) return 4
  if (!hasReport.value) return 5
  return 6
})

const progressPercent = computed(() => {
  if (runningChunk.value) return 30
  if (runningVectorize.value) return 50
  if (runningExtraction.value) return 70
  if (runningReport.value) return 92
  if (runningWorkflow.value) return 85

  let completed = 0
  if (selectedLiterature.value) completed += 1
  if (hasChunks.value) completed += 1
  if (hasVectors.value) completed += 1
  if (hasIndicators.value) completed += 1
  if (hasReviewCompletion.value) completed += 1
  if (hasReport.value) completed += 1
  return Math.round((completed / 6) * 100)
})

const currentLiteratureText = computed(() => {
  if (!selectedLiterature.value) {
    return '请先选择 1 篇文献，工作台才会显示具体状态。'
  }
  return `当前正在查看：${selectedLiterature.value.title}`
})

const progressSummary = computed(() => {
  if (!selectedLiterature.value) return '还没有选中文献。'
  if (!hasChunks.value) return '当前最先要做的是文献分块。'
  if (!hasVectors.value) return '分块已完成，下一步建议做向量化。'
  if (!hasIndicators.value) return '向量化已完成，下一步建议抽取指标。'
  if (!hasReviewCompletion.value) return '指标已经出来了，建议先做人工复核。'
  if (!hasReport.value) return '可以开始生成分析报告了。'
  return '当前文献的主流程已经完成。'
})

const nextActionTitle = computed(() => {
  if (!selectedLiterature.value) return '请先导入并选择一篇文献'
  if (!hasChunks.value) return '建议先执行文献分块'
  if (!hasVectors.value) return '建议继续执行向量化'
  if (!hasIndicators.value) return '建议执行指标抽取'
  if (!hasReviewCompletion.value) return '建议先完成人工复核'
  if (!hasReport.value) return '建议生成分析报告'
  return '当前文献主流程已经完成'
})

const nextActionDescription = computed(() => {
  if (!selectedLiterature.value) return '导入文献后，系统才能继续执行后续步骤。'
  if (!hasChunks.value) return '先把文献切成可处理的片段，后续的向量化、抽取和分析都依赖这一步。'
  if (!hasVectors.value) return '向量化完成后，系统才能更稳定地进行语义检索和证据召回。'
  if (!hasIndicators.value) return '抽取结构化指标后，你才能看到疗效、安全性、时间点等关键内容。'
  if (!hasReviewCompletion.value) return `当前还有 ${pendingIndicatorCount.value} 条指标待人工确认，建议先完成复核。`
  if (!hasReport.value) return '现在可以基于当前文献直接生成分析报告。'
  return '你可以继续查看报告详情，或者重新执行一键工作流做完整演示。'
})

const nextActionButtonText = computed(() => {
  if (!selectedLiterature.value) return '去导入文献'
  if (!hasChunks.value) return '执行分块'
  if (!hasVectors.value) return '执行向量化'
  if (!hasIndicators.value) return '执行抽取'
  if (!hasReviewCompletion.value) return '进入复核页面'
  if (!hasReport.value) return '生成报告'
  return '查看分析页面'
})

function stepCardClass(done: boolean) {
  return done ? 'workflow-step-card--done' : 'workflow-step-card--pending'
}

function containsLiteratureId(contextLiteratureIds?: string | null, literatureId?: number) {
  if (!contextLiteratureIds || !literatureId) {
    return false
  }
  return contextLiteratureIds
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .includes(String(literatureId))
}

async function refreshAll() {
  loadingPage.value = true
  try {
    literatures.value = await listLiteratures()
    if (!literatures.value.length) {
      selectedLiteratureId.value = undefined
      vectorStatus.value = null
      indicators.value = []
      tasks.value = []
      return
    }

    if (!selectedLiteratureId.value || !literatures.value.find((item) => item.id === selectedLiteratureId.value)) {
      selectedLiteratureId.value = literatures.value[0].id
    }

    await loadSelectedLiteratureData()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loadingPage.value = false
  }
}

async function loadSelectedLiteratureData() {
  if (!selectedLiteratureId.value) {
    return
  }
  const [vectorStatusData, indicatorData, taskData] = await Promise.all([
    getLiteratureVectorStatus(selectedLiteratureId.value),
    listLiteratureIndicators(selectedLiteratureId.value),
    listTasks()
  ])
  vectorStatus.value = vectorStatusData
  indicators.value = indicatorData
  tasks.value = taskData
}

async function handleLiteratureChange() {
  if (!selectedLiteratureId.value) {
    return
  }
  await refreshAll()
}

function openSelectedLiterature() {
  if (!selectedLiteratureId.value) {
    router.push('/literatures')
    return
  }
  router.push({
    path: '/literatures',
    query: {
      literatureId: String(selectedLiteratureId.value)
    }
  })
}

function openExtractionPage() {
  if (!selectedLiteratureId.value) {
    router.push('/extraction/indicators')
    return
  }
  router.push({
    path: '/extraction/indicators',
    query: {
      literatureId: String(selectedLiteratureId.value)
    }
  })
}

function openAnalysisPage() {
  if (!selectedLiteratureId.value) {
    router.push('/analysis')
    return
  }
  router.push({
    path: '/analysis',
    query: {
      literatureId: String(selectedLiteratureId.value)
    }
  })
}

function openLatestReport() {
  if (!selectedLiteratureId.value || !latestReportTask.value) {
    openAnalysisPage()
    return
  }
  router.push({
    path: '/analysis',
    query: {
      literatureId: String(selectedLiteratureId.value),
      taskId: String(latestReportTask.value.id)
    }
  })
}

async function handleChunk() {
  if (!selectedLiteratureId.value) {
    ElMessage.warning('请先选择文献')
    return
  }
  runningChunk.value = true
  currentActionLabel.value = '正在执行文献分块'
  currentActionDescription.value = '系统正在解析原始文献并生成分块。完成后再继续做向量化。'
  try {
    await ingestLiterature(selectedLiteratureId.value)
    ElMessage.success('文献分块完成')
    await refreshAll()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    runningChunk.value = false
    currentActionLabel.value = ''
    currentActionDescription.value = ''
  }
}

async function handleVectorize() {
  if (!selectedLiteratureId.value) {
    ElMessage.warning('请先选择文献')
    return
  }
  runningVectorize.value = true
  currentActionLabel.value = '正在执行文献向量化'
  currentActionDescription.value = '系统正在为分块生成 embedding，并同步到 Milvus。'
  try {
    await vectorizeLiterature(selectedLiteratureId.value)
    ElMessage.success('文献向量化完成')
    await refreshAll()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    runningVectorize.value = false
    currentActionLabel.value = ''
    currentActionDescription.value = ''
  }
}

async function handleExtract() {
  if (!selectedLiteratureId.value) {
    ElMessage.warning('请先选择文献')
    return
  }
  runningExtraction.value = true
  currentActionLabel.value = '正在执行指标抽取'
  currentActionDescription.value = '系统正在从文献分块中提取结构化指标。'
  try {
    indicators.value = await runLiteratureExtraction(selectedLiteratureId.value)
    ElMessage.success('指标抽取完成')
    await refreshAll()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    runningExtraction.value = false
    currentActionLabel.value = ''
    currentActionDescription.value = ''
  }
}

async function handleGenerateReport() {
  if (!selectedLiteratureId.value) {
    ElMessage.warning('请先选择文献')
    return
  }
  runningReport.value = true
  currentActionLabel.value = '正在生成分析报告'
  currentActionDescription.value = '系统正在检索证据、整理指标并生成报告结果。'
  try {
    await generateAnalysisReport({
      question: '请对当前文献的疗效证据、安全性信号、关键指标变化和后续验证建议做结构化总结。',
      literatureIds: [selectedLiteratureId.value],
      analysisFocus: '统一科研流程工作台快速分析',
      onlyConfirmedIndicators: false
    })
    ElMessage.success('分析报告已生成')
    await refreshAll()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    runningReport.value = false
    currentActionLabel.value = ''
    currentActionDescription.value = ''
  }
}

async function handleRunWorkflow() {
  if (!selectedLiteratureId.value) {
    ElMessage.warning('请先选择文献')
    return
  }
  runningWorkflow.value = true
  currentActionLabel.value = '正在执行统一工作流'
  currentActionDescription.value = '系统会自动补齐当前文献缺少的步骤，并生成最新报告。'
  try {
    await processLiteratureWorkflow(selectedLiteratureId.value, {
      question: '请对当前文献生成适合药物研发辅助平台展示的综合分析结论。',
      analysisFocus: '统一科研流程工作台',
      reingest: false,
      reextract: false,
      onlyConfirmedIndicators: false
    })
    ElMessage.success('统一工作流执行完成')
    await refreshAll()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    runningWorkflow.value = false
    currentActionLabel.value = ''
    currentActionDescription.value = ''
  }
}

async function runNextAction() {
  if (!selectedLiterature.value) {
    router.push('/literatures')
    return
  }
  if (!hasChunks.value) {
    await handleChunk()
    return
  }
  if (!hasVectors.value) {
    await handleVectorize()
    return
  }
  if (!hasIndicators.value) {
    await handleExtract()
    return
  }
  if (!hasReviewCompletion.value) {
    openExtractionPage()
    return
  }
  if (!hasReport.value) {
    await handleGenerateReport()
    return
  }
  openAnalysisPage()
}

onMounted(refreshAll)
</script>
