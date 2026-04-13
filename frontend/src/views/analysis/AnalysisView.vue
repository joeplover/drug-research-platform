<template>
  <div class="page-grid analysis-page">
    <el-alert
      v-if="currentActionLabel"
      type="warning"
      :closable="false"
      :title="`${currentActionLabel}，请耐心等待`"
      :description="currentActionDescription"
    />

    <div class="split-grid">
      <div class="panel" v-loading="generatingReport">
        <div class="section-title">
          <div>
            <div class="section-title__main">
              <h2>生成分析报告</h2>
              <HelpTip title="这个区域是做什么的">
                这里负责把当前文献整理成更容易阅读和展示的报告。<br />
                报告会结合文献证据片段和已抽取的结构化指标。<br />
                如果你只想快速看一篇文献的总结，优先用这里。<br /><br />
                普通使用只需要三步：选文献、选一个问题、点击【生成报告】。
              </HelpTip>
            </div>
          </div>
        </div>

        <el-form :model="reportForm" label-position="top" class="form-grid">
          <el-form-item class="full-span">
            <template #label>
              <span class="label-with-tip">
                文献范围
                <HelpTip title="这里该怎么选">
                  普通使用只选 1 篇。<br />
                  只有做多文献综合分析时，才需要多选。<br />
                  如果你从文献页跳转过来，系统会自动帮你选中当前文献。
                </HelpTip>
              </span>
            </template>
            <el-select
              v-model="reportForm.literatureIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择文献"
              style="width: 100%"
              :disabled="isBusy"
            >
              <el-option v-for="item in literatures" :key="item.id" :label="item.title" :value="item.id" />
            </el-select>
          </el-form-item>

          <el-form-item class="full-span">
            <template #label>
              <span class="label-with-tip">
                你想让系统回答的问题
                <HelpTip title="这个问题该怎么写">
                  你可以把它理解成【我想让系统帮我写什么总结】。<br />
                  问题越明确，返回结果越聚焦。<br />
                  不知道怎么写时，直接点下面的示例问题即可。
                </HelpTip>
              </span>
            </template>
            <el-input
              v-model="reportForm.question"
              type="textarea"
              :rows="4"
              placeholder="例如：请从疗效、安全性、关键指标变化和后续验证建议四个方面，总结这篇文献。"
              :disabled="isBusy"
            />
            <div class="tag-list" style="margin-top: 10px">
              <el-tag
                v-for="item in reportQuestionExamples"
                :key="item"
                class="clickable-tag"
                effect="plain"
                @click="fillReportQuestion(item)"
              >
                {{ item }}
              </el-tag>
            </div>
          </el-form-item>

          <el-form-item>
            <template #label>
              <span class="label-with-tip">
                报告侧重点
                <HelpTip title="这个字段是什么意思">
                  这里是告诉系统【你更想突出什么】。<br />
                  比如你更关心疗效、安全性、作用机制，还是后续验证建议。<br />
                  不知道怎么填时，保持默认值就可以。
                </HelpTip>
              </span>
            </template>
            <el-input
              v-model="reportForm.analysisFocus"
              placeholder="例如：疗效、安全性、机制和后续验证建议"
              :disabled="isBusy"
            />
          </el-form-item>

          <el-form-item>
            <template #label>
              <span class="label-with-tip">
                只使用已确认指标
                <HelpTip title="什么时候打开这个开关">
                  打开后，系统只会使用你人工确认过的指标。<br />
                  这样结果更稳，但可用信息可能会少一些。<br />
                  如果你还没有做人工复核，建议先关闭。
                </HelpTip>
              </span>
            </template>
            <el-switch v-model="onlyConfirmedIndicators" :disabled="isBusy" />
          </el-form-item>
        </el-form>

        <div class="action-row">
          <el-button type="primary" :loading="generatingReport" :disabled="isBusy" @click="handleGenerateReport">生成报告</el-button>
          <el-button plain :disabled="isBusy" @click="fillDemoQuestion">填入答辩示例问题</el-button>
        </div>
      </div>

      <div class="panel">
        <div class="section-title">
          <div>
            <h2>报告结果</h2>
          </div>
          <div class="action-row" v-if="reportResult">
            <el-button text type="primary" @click="copyReport">复制全文</el-button>
            <el-button text @click="showRawReport = !showRawReport">
              {{ showRawReport ? '收起原文' : '查看原文' }}
            </el-button>
          </div>
        </div>

        <el-empty v-if="!reportResult" description="还没有报告结果。先在左侧选择文献和问题，再点击【生成报告】。" />
        <template v-else>
          <div class="stats-grid">
            <StatCard label="任务 ID" :value="reportResult.taskId" />
            <StatCard label="任务状态" :value="reportResult.taskStatus" />
            <StatCard label="证据条数" :value="reportResult.evidence.length" />
            <StatCard label="指标条数" :value="displayIndicators.length" />
          </div>

          <div class="status-note">
            当前报告基于 {{ reportResult.evidence.length }} 条证据片段和 {{ displayIndicators.length }} 条指标生成。
            {{ onlyConfirmedIndicators ? '你已启用【只使用已确认指标】。' : '你当前使用的是全部已抽取指标。' }}
          </div>

          <div class="report-sections" style="margin-top: 16px">
            <div v-for="section in parsedSections" :key="section.title + section.content.slice(0, 12)" class="report-section">
              <div class="report-section__title">{{ section.title }}</div>
              <div class="report-section__content">{{ section.content }}</div>
            </div>
          </div>

          <div v-if="showRawReport" class="mono-block" style="margin-top: 16px">{{ reportResult.reportSummary }}</div>

          <el-divider />

          <div class="section-title">
            <div>
              <h2>证据文献</h2>
            </div>
          </div>
          <el-table :data="reportResult.evidence" stripe>
            <el-table-column prop="title" label="证据文献" min-width="180" />
            <el-table-column prop="score" label="得分" width="100" />
            <el-table-column prop="retrievalMode" label="检索模式" min-width="140" />
            <el-table-column label="证据定位" min-width="180">
              <template #default="{ row }">
                <el-button
                  v-if="row.evidenceLocator"
                  link
                  type="primary"
                  @click="openEvidence(row.literatureId, row.evidenceLocator)"
                >
                  {{ row.evidenceLocator }}
                </el-button>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="snippet" label="证据片段" min-width="280" show-overflow-tooltip />
          </el-table>

          <el-divider />

          <div class="section-title">
            <div>
              <h2>关联指标</h2>
            </div>
          </div>
          <el-table :data="displayIndicators" stripe>
            <el-table-column prop="indicatorName" label="指标" min-width="140" />
            <el-table-column prop="category" label="类别" width="120" />
            <el-table-column prop="timeWindow" label="时间窗口" min-width="120" />
            <el-table-column prop="cohort" label="队列" min-width="140" />
            <el-table-column prop="observedValue" label="观测值" width="100" />
            <el-table-column prop="evidenceLocator" label="证据定位" min-width="160" />
            <el-table-column prop="reviewStatus" label="复核状态" min-width="120" />
          </el-table>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import HelpTip from '@/components/HelpTip.vue'
import { listLiteratures } from '@/api/modules/literatures'
import { generateAnalysisReport } from '@/api/modules/analysis'
import { getTaskDetail, listTasks } from '@/api/modules/tasks'
import StatCard from '@/components/StatCard.vue'
import type {
  AnalysisTaskDetailView,
  AnalysisReportResponse,
  LiteratureView
} from '@/types/platform'

const router = useRouter()
const route = useRoute()

interface ReportSection {
  title: string
  content: string
}

const reportQuestionExamples = [
  '请从疗效、安全性、关键指标变化和后续验证建议四个方面，总结这篇文献。',
  '请概括这篇文献的研究目标、研究方法、主要结果和研究意义。',
  '请总结这篇文献最值得继续验证的发现，并说明原因。'
]

const literatures = ref<LiteratureView[]>([])
const reportResult = ref<AnalysisReportResponse | null>(null)
const generatingReport = ref(false)
const showRawReport = ref(false)
const onlyConfirmedIndicators = ref(false)
const currentActionLabel = ref('')
const currentActionDescription = ref('')

const isBusy = computed(() => generatingReport.value)

const reportForm = reactive({
  literatureIds: [] as number[],
  analysisFocus: '药物研发证据综合分析',
  question: '请基于已选文献，总结关键疗效证据、安全性信号、主要指标变化，以及后续验证建议。'
})

const parsedSections = computed<ReportSection[]>(() => {
  if (!reportResult.value?.reportSummary) {
    return []
  }

  const raw = reportResult.value.reportSummary.replace(/\r/g, '').trim()
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

const displayIndicators = computed(() => {
  const indicators = reportResult.value?.indicators ?? []
  if (!onlyConfirmedIndicators.value) {
    return indicators
  }
  return indicators.filter((item) => item.reviewStatus === '已确认')
})

function fillReportQuestion(question: string) {
  reportForm.question = question
}

function fillDemoQuestion() {
  reportForm.analysisFocus = '药物研发证据综合分析'
  reportForm.question = '请从疗效、安全性、指标变化和后续验证建议四个方面，对已选文献进行结构化总结。'
  ElMessage.success('已填入答辩示例问题')
}

function openEvidence(literatureId: number, evidenceLocator: string) {
  router.push({
    path: '/literatures',
    query: {
      literatureId: String(literatureId),
      chunkLabel: evidenceLocator
    }
  })
}

async function copyReport() {
  if (!reportResult.value?.reportSummary) {
    return
  }
  try {
    await navigator.clipboard.writeText(reportResult.value.reportSummary)
    ElMessage.success('报告内容已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

async function loadOptions() {
  try {
    const data = await listLiteratures()
    literatures.value = data
    const requestedLiteratureId = Number(route.query.literatureId)
    if (data.length && !reportForm.literatureIds.length) {
      reportForm.literatureIds = [data.find((item) => item.id === requestedLiteratureId)?.id ?? data[0].id]
    }
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
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

function applyTaskDetail(detail: AnalysisTaskDetailView) {
  reportResult.value = {
    taskId: detail.id,
    taskStatus: detail.status,
    reportSummary: detail.resultSummary,
    evidence: detail.evidence ?? [],
    indicators: detail.indicators ?? []
  }
  if (detail.inputText) {
    reportForm.question = detail.inputText
  }
  if (detail.analysisFocus) {
    reportForm.analysisFocus = detail.analysisFocus
  }
  if (detail.contextLiteratureIds) {
    reportForm.literatureIds = detail.contextLiteratureIds
      .split(',')
      .map((item) => Number(item.trim()))
      .filter((item) => Number.isFinite(item))
  }
}

async function restoreSavedReport() {
  const explicitTaskId = Number(route.query.taskId)
  if (explicitTaskId) {
    try {
      const detail = await getTaskDetail(explicitTaskId)
      if (detail.taskType === 'REPORT') {
        applyTaskDetail(detail)
      }
      return
    } catch {
      return
    }
  }

  const currentLiteratureId = reportForm.literatureIds[0]
  if (!currentLiteratureId) {
    return
  }

  try {
    const tasks = await listTasks()
    const latestTask = tasks.find(
      (item) => item.taskType === 'REPORT' && containsLiteratureId(item.contextLiteratureIds, currentLiteratureId)
    )
    if (!latestTask) {
      return
    }
    const detail = await getTaskDetail(latestTask.id)
    applyTaskDetail(detail)
  } catch (error) {
    ElMessage.warning(`历史报告恢复失败：${(error as Error).message}`)
  }
}

async function handleGenerateReport() {
  if (!reportForm.question || !reportForm.literatureIds.length) {
    ElMessage.warning('请先选择文献，并填写你想让系统回答的问题')
    return
  }
  generatingReport.value = true
  currentActionLabel.value = '正在生成分析报告'
  currentActionDescription.value = '系统正在检索证据、整理指标并生成报告，结果返回前请不要重复点击。'
  try {
    reportResult.value = await generateAnalysisReport({
      ...reportForm,
      onlyConfirmedIndicators: onlyConfirmedIndicators.value
    })
    showRawReport.value = false
    ElMessage.success('分析报告已生成')
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    generatingReport.value = false
    currentActionLabel.value = ''
    currentActionDescription.value = ''
  }
}

onMounted(async () => {
  await loadOptions()
  await restoreSavedReport()
})
</script>

<style scoped>
.analysis-page :deep(.el-collapse-item__header) {
  font-size: 13px;
}
</style>
