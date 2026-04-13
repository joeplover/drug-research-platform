<template>
  <div class="page-grid">
    <div class="split-grid result-first-grid">
      <div class="panel" v-loading="savingCreate || savingImport || savingBatch">
        <div class="section-title">
          <div>
            <div class="section-title__main">
              <h2>文献导入与登记</h2>
              <HelpTip title="如何使用文献页">
                建议先用"拖拽/选择文件"导入文献。<br />
                导入后系统会自动完成解析、向量化和初步指标准备。<br />
                你优先看右侧文献概览，再决定是否进入高级处理。
              </HelpTip>
            </div>
          </div>
        </div>
        <el-tabs v-model="activeTab">
          <el-tab-pane label="拖拽/选择文件" name="import">
            <el-form :model="importForm" label-position="top" class="form-grid">
              <el-form-item label="上传文件" class="full-span">
                <el-upload
                  drag
                  action="#"
                  :auto-upload="false"
                  :show-file-list="true"
                  :limit="1"
                  :on-change="handleUploadFileChange"
                  :on-remove="handleUploadFileRemove"
                  class="upload-full-width"
                >
                  <el-icon style="font-size: 28px"><UploadFilled /></el-icon>
                  <div class="el-upload__text">将文件拖到这里，或 <em>点击选择文件</em></div>
                </el-upload>
              </el-form-item>
              <el-form-item label="自动处理">
                <el-switch v-model="importForm.autoIngest" />
                <span style="margin-left: 10px; color: #909399; font-size: 13px">开启后自动执行分块、向量化和指标抽取</span>
              </el-form-item>
            </el-form>
            <el-alert
              style="margin-bottom: 12px"
              type="info"
              :closable="false"
              :title="selectedUploadFile ? `已选择文件：${selectedUploadFile.name}` : '请选择或拖入一个文件'"
              :description="selectedUploadFile ? '导入后系统会自动生成文献概览、关键指标和图谱预览。' : '如果浏览器无法直接访问本地路径，请优先使用拖拽上传。'"
            />
            <div class="action-row">
              <el-button type="primary" :loading="savingImport" :disabled="isBusy || !selectedUploadFile" @click="handleImport">导入文件</el-button>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <div class="panel" v-loading="loadingOverview || runningIngest || runningVectorize || loadingChunks || loadingVectorStatus">
        <div class="section-title">
          <div>
            <div class="section-title__main">
              <h2>当前文献概览</h2>
              <HelpTip title="为什么先看概览">
                这里优先回答"这篇文献做了什么、有哪些关键结果、证据在哪里"。<br />
                分块、向量化等技术动作被放到了下方高级区域。<br />
                如果你只是做毕业设计演示，先看这一块最清楚。
              </HelpTip>
            </div>
          </div>
          <div class="action-row" v-if="selectedLiterature">
            <el-button text type="primary" @click="openAnalysisPage">生成研发报告</el-button>
            <el-button text @click="openGraphPage()">查看完整图谱</el-button>
            <el-button text type="danger" :disabled="isBusy" @click="handleDelete(selectedLiterature)">删除文献</el-button>
          </div>
        </div>
        <el-empty v-if="!selectedLiterature" description="请先在下方列表中选择文献" />
        <template v-else>
          <el-progress :percentage="stagePercent" :stroke-width="16" :status="stagePercent >= 100 ? 'success' : undefined" />

          <div class="stats-grid compact-stats" style="margin-top: 14px">
            <StatCard label="处理阶段" :value="overview?.processingStage || '准备中'" />
            <StatCard label="分块数" :value="overview?.chunkCount ?? 0" />
            <StatCard label="关键指标数" :value="overview?.indicatorCount ?? 0" />
            <StatCard label="下一步" :value="nextStepText" />
          </div>

          <el-descriptions :column="1" border style="margin-top: 14px">
            <el-descriptions-item label="标题">{{ selectedLiterature.title }}</el-descriptions-item>
            <el-descriptions-item label="疾病领域">{{ selectedLiterature.diseaseArea }}</el-descriptions-item>
            <el-descriptions-item label="来源类型">{{ selectedLiterature.sourceType }}</el-descriptions-item>
            <el-descriptions-item label="关键词">{{ selectedLiterature.keywords || '-' }}</el-descriptions-item>
            <el-descriptions-item label="发布日期">{{ formatDate(selectedLiterature.publicationDate) }}</el-descriptions-item>
            <el-descriptions-item label="向量状态">
              <el-tag :type="milvusTagType">{{ vectorStatus?.milvusStatus || 'UNKNOWN' }}</el-tag>
              <span class="muted" style="margin-left: 8px">{{ vectorStatus?.message || '等待状态更新' }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <div class="insight-card insight-card--summary" style="margin-top: 14px">
            <div class="insight-card__title">一句话概览</div>
            <div class="insight-card__content">{{ overview?.overviewSummary || selectedLiterature.summary || '暂无概览摘要' }}</div>
          </div>

          <div class="insight-grid" style="margin-top: 14px">
            <div class="insight-card">
              <div class="insight-card__title">研究目标</div>
              <div class="insight-card__content">{{ overview?.researchFocus || '暂无结果' }}</div>
            </div>
            <div class="insight-card">
              <div class="insight-card__title">研究方法</div>
              <div class="insight-card__content">{{ overview?.methodSummary || '暂无结果' }}</div>
            </div>
            <div class="insight-card">
              <div class="insight-card__title">关键结果</div>
              <div class="insight-card__content">{{ overview?.resultSummary || '暂无结果' }}</div>
            </div>
            <div class="insight-card">
              <div class="insight-card__title">安全性提示</div>
              <div class="insight-card__content">{{ overview?.safetySummary || '暂无结果' }}</div>
            </div>
          </div>

          <div class="insight-card" style="margin-top: 14px">
            <div class="insight-card__title">结论与意义</div>
            <div class="insight-card__content">{{ overview?.conclusionSummary || '暂无结果' }}</div>
          </div>

          <div class="split-grid overview-content-grid" style="margin-top: 14px">
            <div class="insight-card">
              <div class="insight-card__title">关键概念</div>
              <div class="tag-list">
                <el-tag v-for="item in overview?.keyConcepts || []" :key="item" effect="plain">{{ item }}</el-tag>
                <span v-if="!(overview?.keyConcepts || []).length" class="muted">暂无关键概念</span>
              </div>

              <div class="insight-card__title" style="margin-top: 14px">阅读要点</div>
              <ul class="simple-list">
                <li v-for="item in overview?.keyPoints || []" :key="item">{{ item }}</li>
                <li v-if="!(overview?.keyPoints || []).length" class="muted">暂无阅读要点</li>
              </ul>
            </div>

            <div class="insight-card">
              <div class="insight-card__title">证据片段</div>
              <div class="evidence-list">
                <div v-for="item in overview?.evidenceHighlights || []" :key="item" class="evidence-item">
                  {{ item }}
                </div>
                <div v-if="!(overview?.evidenceHighlights || []).length" class="muted">暂无证据片段</div>
              </div>
            </div>
          </div>

          <div class="split-grid overview-content-grid" style="margin-top: 14px">
            <div class="insight-card">
              <div class="section-title" style="margin-bottom: 10px">
                <div>
                  <h2>关键指标</h2>
                </div>
                <el-button text type="primary" @click="openExtractionPage">进入抽取页</el-button>
              </div>
              <el-table :data="overview?.keyIndicators || []" stripe max-height="260">
                <el-table-column prop="indicatorName" label="指标" min-width="130" />
                <el-table-column prop="category" label="类别" width="100" />
                <el-table-column prop="timeWindow" label="时间窗口" min-width="120" />
                <el-table-column label="证据定位" min-width="160">
                  <template #default="{ row }">
                    <el-button v-if="row.evidenceLocator" link type="primary" @click="openChunks(selectedLiterature.id, row.evidenceLocator)">
                      {{ row.evidenceLocator }}
                    </el-button>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="!(overview?.keyIndicators || []).length" class="muted">当前文献尚未形成关键指标。</div>
            </div>

            <div class="insight-card">
              <div class="section-title" style="margin-bottom: 10px">
                <div>
                  <h2>图谱预览</h2>
                </div>
                <el-button text type="primary" @click="openGraphPage()">打开图谱页</el-button>
              </div>
              <GraphChart v-if="overview?.graph?.nodes?.length" :graph="overview.graph" />
              <el-empty v-else description="当前文献暂无图谱数据" />
            </div>
          </div>

          <el-collapse style="margin-top: 14px">
            <el-collapse-item title="高级处理控制">
              <el-alert
                v-if="currentActionLabel"
                style="margin-bottom: 12px"
                type="warning"
                :closable="false"
                :title="`${currentActionLabel}，请稍候`"
                :description="currentActionDescription"
              />
              <div class="action-row">
                <el-button :loading="runningIngest" :disabled="isBusy" @click="handleIngest(selectedLiterature.id)">重新分块</el-button>
                <el-button type="primary" :loading="runningVectorize" :disabled="isBusy" @click="handleVectorize(selectedLiterature.id)">重新向量化</el-button>
                <el-button :loading="loadingChunks" :disabled="isBusy" @click="openChunks(selectedLiterature.id)">查看分块</el-button>
              </div>
              <div class="mono-block" style="margin-top: 12px">{{ selectedLiterature.summary }}</div>
            </el-collapse-item>
          </el-collapse>
        </template>
      </div>
    </div>

    <div class="panel" v-loading="loadingLiteratures">
      <div class="section-title">
        <div>
          <h2>文献列表</h2>
        </div>
        <div class="table-toolbar">
          <el-input v-model="keyword" placeholder="按标题或疾病领域搜索" clearable style="width: 280px" :disabled="isBusy" @keyup.enter="loadLiteratures" />
          <el-button type="primary" :loading="loadingLiteratures" :disabled="isBusy" @click="loadLiteratures">查询</el-button>
        </div>
      </div>
      <el-table :data="literatures" stripe highlight-current-row :current-row-key="selectedLiterature?.id" row-key="id" @current-change="handleCurrentChange">
        <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
        <el-table-column prop="diseaseArea" label="疾病领域" min-width="140" />
        <el-table-column prop="sourceType" label="来源" width="120" />
        <el-table-column label="发布日期" min-width="140">
          <template #default="{ row }">{{ formatDate(row.publicationDate) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="240" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" :disabled="isBusy" @click="selectCurrentLiterature(row)">查看概览</el-button>
              <el-button link :disabled="isBusy" @click="openChunks(row.id)">查看分块</el-button>
              <el-button link :disabled="isBusy" @click="openGraphPage(row.id)">图谱</el-button>
              <el-button link type="danger" :disabled="isBusy" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="chunksVisible" title="文献分块详情" width="70%">
      <div v-if="chunkLabelFilter" style="margin-bottom: 12px">
        <el-tag type="primary">当前定位：{{ chunkLabelFilter }}</el-tag>
        <el-button link @click="clearChunkFilter">清除定位</el-button>
      </div>
      <el-table :data="displayChunks" stripe :row-class-name="chunkRowClassName">
        <el-table-column prop="chunkIndex" label="序号" width="80" />
        <el-table-column prop="sourceSection" label="章节" width="140" />
        <el-table-column label="标签" min-width="180">
          <template #default="{ row }">
            <el-tag :type="row.chunkLabel === chunkLabelFilter ? 'danger' : 'info'">
              {{ row.chunkLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="400" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import HelpTip from '@/components/HelpTip.vue'
import GraphChart from '@/components/GraphChart.vue'
import StatCard from '@/components/StatCard.vue'
import {
  batchImportLiteratures,
  createLiterature,
  deleteLiterature,
  getLiteratureOverview,
  getLiteratureVectorStatus,
  importLiterature,
  ingestLiterature,
  listLiteratureChunks,
  listLiteratures,
  uploadLiterature,
  vectorizeLiterature
} from '@/api/modules/literatures'
import type {
  LiteratureChunkView,
  LiteratureOverviewView,
  LiteratureVectorStatusView,
  LiteratureView
} from '@/types/platform'
import type { UploadFile, UploadFiles } from 'element-plus'
import { formatDate, formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const activeTab = ref('import')
const keyword = ref('')
const literatures = ref<LiteratureView[]>([])
const selectedLiterature = ref<LiteratureView | null>(null)
const overview = ref<LiteratureOverviewView | null>(null)
const chunks = ref<LiteratureChunkView[]>([])
const chunksVisible = ref(false)
const vectorStatus = ref<LiteratureVectorStatusView | null>(null)
const chunkLabelFilter = ref('')

const savingImport = ref(false)
const loadingLiteratures = ref(false)
const loadingOverview = ref(false)
const runningIngest = ref(false)
const runningVectorize = ref(false)
const loadingChunks = ref(false)
const loadingVectorStatus = ref(false)
const currentActionLabel = ref('')
const currentActionDescription = ref('')
const selectedUploadFile = ref<File | null>(null)

const importForm = reactive({
  title: '',
  sourceType: 'PDF',
  diseaseArea: '',
  keywords: '',
  publicationDate: '',
  autoIngest: false
})

const displayChunks = computed(() => {
  if (!chunkLabelFilter.value) {
    return chunks.value
  }
  const filtered = chunks.value.filter((item) => item.chunkLabel === chunkLabelFilter.value)
  return filtered.length ? filtered : chunks.value
})

const isBusy = computed(
  () =>
    savingImport.value ||
    loadingLiteratures.value ||
    loadingOverview.value ||
    runningIngest.value ||
    runningVectorize.value ||
    loadingChunks.value ||
    loadingVectorStatus.value
)

const nextStepText = computed(() => {
  if (!vectorStatus.value || !overview.value) {
    return '正在准备'
  }
  if (!vectorStatus.value.chunkCount) {
    return '建议重新分块'
  }
  if (vectorStatus.value.embeddedChunkCount < vectorStatus.value.chunkCount) {
    return '建议重新向量化'
  }
  if (!overview.value.indicatorCount) {
    return '建议进入抽取页检查'
  }
  return '可以进入抽取、图谱或分析页'
})

const stagePercent = computed(() => {
  if (!selectedLiterature.value) {
    return 0
  }
  const chunkCount = vectorStatus.value?.chunkCount ?? 0
  const embeddedChunkCount = vectorStatus.value?.embeddedChunkCount ?? 0
  const indicatorCount = overview.value?.indicatorCount ?? 0

  if (chunkCount <= 0) return 20
  if (embeddedChunkCount < chunkCount) return 55
  if (indicatorCount <= 0) return 80
  return 100
})

const milvusTagType = computed(() => {
  if (vectorStatus.value?.milvusStatus === 'UP') {
    return 'success'
  }
  if (vectorStatus.value?.milvusStatus === 'DEGRADED') {
    return 'warning'
  }
  return 'info'
})

async function loadLiteratures(preferredLiteratureId?: number) {
  loadingLiteratures.value = true
  try {
    const data = await listLiteratures(keyword.value || undefined)
    literatures.value = data
    if (!data.length) {
      selectedLiterature.value = null
      overview.value = null
      vectorStatus.value = null
      return
    }

    const requestedLiteratureId = preferredLiteratureId ?? Number(route.query.literatureId)
    selectedLiterature.value =
      data.find((item) => item.id === requestedLiteratureId) ??
      data.find((item) => item.id === selectedLiterature.value?.id) ??
      data[0]

    if (selectedLiterature.value) {
      const currentQueryId = Number(route.query.literatureId)
      if (currentQueryId !== selectedLiterature.value.id) {
        await router.replace({
          path: '/literatures',
          query: {
            literatureId: String(selectedLiterature.value.id)
          }
        })
      } else {
        await loadSelectedDetails(selectedLiterature.value.id)
      }
    }
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loadingLiteratures.value = false
  }
}

async function loadSelectedDetails(literatureId: number) {
  loadingOverview.value = true
  loadingVectorStatus.value = true
  try {
    const [overviewData, vectorStatusData] = await Promise.all([
      getLiteratureOverview(literatureId),
      getLiteratureVectorStatus(literatureId)
    ])
    overview.value = overviewData
    vectorStatus.value = vectorStatusData
  } catch (error) {
    overview.value = null
    vectorStatus.value = null
    ElMessage.error((error as Error).message)
  } finally {
    loadingOverview.value = false
    loadingVectorStatus.value = false
  }
}

async function handleCreate() {
  if (!createForm.title || !createForm.summary || !createForm.diseaseArea) {
    ElMessage.warning('请先填写标题、疾病领域和摘要')
    return
  }
  savingCreate.value = true
  try {
    const created = await createLiterature({ ...createForm })
    ElMessage.success('文献已保存')
    Object.assign(createForm, {
      title: '',
      sourceType: 'PDF',
      diseaseArea: '',
      summary: '',
      keywords: '',
      publicationDate: '',
      storagePath: ''
    })
    await loadLiteratures(created.id)
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    savingCreate.value = false
  }
}

async function handleImport() {
  if (!selectedUploadFile.value) {
    ElMessage.warning('请先选择要上传的文件')
    return
  }
  savingImport.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedUploadFile.value)
    formData.append('autoIngest', String(importForm.autoIngest))

    const imported = await uploadLiterature(formData)
    ElMessage.success('文件导入成功')
    Object.assign(importForm, {
      title: '',
      sourceType: 'PDF',
      diseaseArea: '',
      keywords: '',
      publicationDate: '',
      autoIngest: false
    })
    selectedUploadFile.value = null
    await loadLiteratures(imported.id)
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    savingImport.value = false
  }
}

async function handlePathImport() {
  if (!pathImportForm.storagePath) {
    ElMessage.warning('请填写文件路径')
    return
  }
  savingImport.value = true
  try {
    const imported = await importLiterature({ ...pathImportForm })
    ElMessage.success('按路径导入成功')
    Object.assign(pathImportForm, {
      storagePath: '',
      title: '',
      sourceType: 'PDF',
      diseaseArea: '',
      keywords: '',
      publicationDate: ''
    })
    await loadLiteratures(imported.id)
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    savingImport.value = false
  }
}

async function handleBatchImport() {
  const items = batchForm.paths
    .split(/\r?\n/)
    .map((path) => path.trim())
    .filter(Boolean)
    .map((storagePath) => ({
      storagePath,
      sourceType: batchForm.sourceType,
      diseaseArea: batchForm.diseaseArea,
      keywords: batchForm.keywords
    }))
  if (!items.length) {
    ElMessage.warning('请至少输入一个文件路径')
    return
  }
  savingBatch.value = true
  try {
    const imported = await batchImportLiteratures({ items, autoIngest: batchForm.autoIngest })
    ElMessage.success('批量导入完成')
    Object.assign(batchForm, {
      paths: '',
      sourceType: 'PDF',
      diseaseArea: '',
      keywords: '',
      autoIngest: false
    })
    await loadLiteratures(imported[0]?.id)
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    savingBatch.value = false
  }
}

function handleUploadFileChange(uploadFile: UploadFile, _uploadFiles: UploadFiles) {
  const rawFile = uploadFile.raw
  if (!rawFile) {
    return
  }
  selectedUploadFile.value = rawFile
  if (!importForm.title) {
    importForm.title = rawFile.name.replace(/\.[^.]+$/, '')
  }
  if (!importForm.sourceType || importForm.sourceType === 'PDF') {
    importForm.sourceType = detectSourceTypeByName(rawFile.name)
  }
}

function handleUploadFileRemove() {
  selectedUploadFile.value = null
}

function detectSourceTypeByName(fileName: string) {
  const lowerName = fileName.toLowerCase()
  if (lowerName.endsWith('.pdf')) return 'PDF'
  if (lowerName.endsWith('.xlsx') || lowerName.endsWith('.xls')) return 'EXCEL'
  if (lowerName.endsWith('.doc') || lowerName.endsWith('.docx')) return 'DOC'
  if (lowerName.endsWith('.ppt') || lowerName.endsWith('.pptx')) return 'PPT'
  if (lowerName.endsWith('.md') || lowerName.endsWith('.txt')) return 'TEXT'
  return 'FILE'
}

async function handleIngest(literatureId: number) {
  if (isBusy.value) {
    return
  }
  runningIngest.value = true
  currentActionLabel.value = '正在执行分块'
  currentActionDescription.value = '系统正在解析文献并生成分块，请不要重复点击。'
  try {
    await ingestLiterature(literatureId)
    ElMessage.success('文献分块完成')
    await loadSelectedDetails(literatureId)
    await openChunks(literatureId)
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    runningIngest.value = false
    currentActionLabel.value = ''
    currentActionDescription.value = ''
  }
}

async function handleVectorize(literatureId: number) {
  if (isBusy.value) {
    return
  }
  runningVectorize.value = true
  currentActionLabel.value = '正在执行向量化'
  currentActionDescription.value = '系统正在生成向量并同步状态，这个过程可能持续几十秒。'
  try {
    await vectorizeLiterature(literatureId)
    ElMessage.success('文献向量化完成')
    await loadSelectedDetails(literatureId)
    await loadLiteratures(literatureId)
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    runningVectorize.value = false
    currentActionLabel.value = ''
    currentActionDescription.value = ''
  }
}

async function openChunks(literatureId: number, chunkLabel?: string) {
  loadingChunks.value = true
  try {
    chunks.value = await listLiteratureChunks(literatureId)
    chunkLabelFilter.value = chunkLabel ?? (typeof route.query.chunkLabel === 'string' ? route.query.chunkLabel : '')
    chunksVisible.value = true
    await router.replace({
      path: '/literatures',
      query: {
        literatureId: String(literatureId),
        ...(chunkLabelFilter.value ? { chunkLabel: chunkLabelFilter.value } : {})
      }
    })
    await nextTick()
    scrollToActiveChunk()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loadingChunks.value = false
  }
}

function handleCurrentChange(row?: LiteratureView) {
  if (!row) {
    selectedLiterature.value = null
    return
  }
  selectCurrentLiterature(row)
}

function selectCurrentLiterature(row: LiteratureView) {
  selectedLiterature.value = row
  const currentQueryId = Number(route.query.literatureId)
  if (currentQueryId === row.id) {
    loadSelectedDetails(row.id)
    return
  }
  router.replace({
    path: '/literatures',
    query: {
      literatureId: String(row.id)
    }
  })
}

function clearChunkFilter() {
  chunkLabelFilter.value = ''
  router.replace({ path: '/literatures', query: selectedLiterature.value ? { literatureId: String(selectedLiterature.value.id) } : {} })
}

function chunkRowClassName({ row }: { row: LiteratureChunkView }) {
  return row.chunkLabel === chunkLabelFilter.value ? 'chunk-row--active' : ''
}

function scrollToActiveChunk() {
  if (!chunkLabelFilter.value) {
    return
  }
  const activeRow = document.querySelector('.chunk-row--active')
  if (activeRow && 'scrollIntoView' in activeRow) {
    ;(activeRow as HTMLElement).scrollIntoView({ block: 'center', behavior: 'smooth' })
  }
}

function openExtractionPage() {
  if (!selectedLiterature.value) {
    return
  }
  router.push({
    path: '/extraction',
    query: { literatureId: String(selectedLiterature.value.id) }
  })
}

function openAnalysisPage() {
  if (!selectedLiterature.value) {
    return
  }
  router.push({
    path: '/analysis',
    query: { literatureId: String(selectedLiterature.value.id) }
  })
}

function openGraphPage(literatureId?: number) {
  const targetLiteratureId = literatureId ?? selectedLiterature.value?.id
  router.push({
    path: '/graph',
    query: targetLiteratureId ? { literatureId: String(targetLiteratureId) } : {}
  })
}

async function handleDelete(row: LiteratureView) {
  try {
    await ElMessageBox.confirm(
      `确定删除文献“${row.title}”吗？系统会同时清理该文献的分块、指标、状态和 Milvus 向量记录。`,
      '删除文献',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }
    )

    await deleteLiterature(row.id)
    ElMessage.success('文献已删除')

    if (selectedLiterature.value?.id === row.id) {
      selectedLiterature.value = null
      overview.value = null
      vectorStatus.value = null
    }
    if (chunksVisible.value) {
      chunksVisible.value = false
      chunks.value = []
    }

    await router.replace({ path: '/literatures', query: {} })
    await loadLiteratures()
  } catch (error) {
    const message = (error as Error).message
    if (message === 'cancel' || message === 'close') {
      return
    }
    ElMessage.error(message)
  }
}

watch(
  () => route.query,
  async (query) => {
    const literatureId = Number(query.literatureId)
    if (!literatureId || !literatures.value.length) {
      return
    }
    const matched = literatures.value.find((item) => item.id === literatureId)
    if (matched) {
      const shouldReload = matched.id !== selectedLiterature.value?.id || !overview.value
      selectedLiterature.value = matched
      if (shouldReload) {
        await loadSelectedDetails(matched.id)
      }
      if (query.chunkLabel && !chunksVisible.value) {
        await openChunks(matched.id, String(query.chunkLabel))
      }
    }
  }
)

onMounted(loadLiteratures)
</script>

<style scoped>
.upload-full-width {
  width: 100% !important;
}

.upload-full-width :deep(.el-upload-dragger) {
  width: 100% !important;
}

:deep(.chunk-row--active) {
  background-color: rgba(64, 158, 255, 0.12);
}

.compact-stats :deep(.stat-card__value) {
  font-size: 18px;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.overview-content-grid {
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
}

.insight-card {
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--surface-strong);
  padding: 12px;
}

.insight-card--summary {
  border-color: rgba(15, 118, 110, 0.2);
}

.insight-card__title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 8px;
}

.insight-card__content {
  font-size: 12px;
  line-height: 1.7;
  color: var(--text);
  white-space: pre-wrap;
}

.simple-list {
  margin: 0;
  padding-left: 18px;
  line-height: 1.8;
  font-size: 12px;
}

.evidence-list {
  display: grid;
  gap: 8px;
}

.evidence-item {
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px dashed var(--line);
  background: #fffefb;
  font-size: 12px;
  line-height: 1.7;
}

@media (max-width: 1366px) {
  .insight-grid,
  .overview-content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
