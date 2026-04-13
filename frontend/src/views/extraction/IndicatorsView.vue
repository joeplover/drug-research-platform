<template>
  <div class="page-grid">
    <el-alert
      v-if="currentActionLabel"
      type="warning"
      :closable="false"
      :title="`${currentActionLabel}，请稍候`"
      :description="currentActionDescription"
    />

    <div class="panel" v-loading="runningExtraction">
      <div class="section-title">
        <div>
          <div class="section-title__main">
            <h2>文献指标抽取</h2>
            <HelpTip title="如何使用抽取页">
              先选择文献，再点"运行抽取"。<br />
              抽取完成后，下方会直接显示结果。<br />
              需要更稳妥时，再对结果做人工复核。
            </HelpTip>
          </div>
        </div>
      </div>

      <el-form label-position="top">
        <el-form-item label="当前要分析的文献">
          <el-select v-model="selectedLiteratureId" placeholder="请选择文献" style="width: 100%" :disabled="isBusy" @change="loadIndicators">
            <el-option v-for="item in literatures" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <div class="action-row">
        <el-button :disabled="isBusy" @click="loadIndicators">刷新结果</el-button>
        <el-button type="primary" :loading="runningExtraction" :disabled="isBusy" @click="runChunkExtraction">运行抽取</el-button>
      </div>
    </div>

    <div class="panel">
      <el-collapse>
        <el-collapse-item title="高级调试：直接文本抽取">
          <div class="section-title" style="margin-top: 4px">
            <div>
              <h2>直接文本抽取</h2>
            </div>
          </div>

          <div v-loading="runningDirect">
            <el-form :model="directForm" label-position="top" class="form-grid">
              <el-form-item label="所属文献 ID">
                <el-input-number v-model="directForm.literatureId" :min="1" style="width: 100%" :disabled="isBusy" />
              </el-form-item>
              <el-form-item label="研究人群（选填）">
                <el-input v-model="directForm.cohort" placeholder="例如：overall population" :disabled="isBusy" />
              </el-form-item>
              <el-form-item label="观察时间点（选填）">
                <el-input v-model="directForm.timeWindow" placeholder="例如：week 12" :disabled="isBusy" />
              </el-form-item>
              <el-form-item label="直接粘贴一段原文" class="full-span">
                <el-input v-model="directForm.content" type="textarea" :rows="5" :disabled="isBusy" />
              </el-form-item>
            </el-form>

            <div class="action-row">
              <el-button type="primary" :loading="runningDirect" :disabled="isBusy" @click="runDirectExtraction">执行文本抽取</el-button>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <div class="panel">
      <div class="section-title">
        <div>
          <h2>抽取结果</h2>
          <span class="result-count">共 {{ pagination.total }} 条</span>
        </div>
        <div class="batch-actions" v-if="indicators.length > 0">
          <el-button type="success" :disabled="isBusy" @click="handleBatchReview('已确认')">一键确认全部</el-button>
          <el-button type="danger" :disabled="isBusy" @click="handleBatchReview('已拒绝')">一键驳回全部</el-button>
        </div>
      </div>

      <el-empty v-if="!indicators.length" description="还没有抽取结果。先选文献，再点上方【运行抽取】。" />
      <template v-else>
        <el-table :data="indicators" stripe>
          <el-table-column prop="indicatorName" label="指标" min-width="150" />
          <el-table-column prop="category" label="类别" width="120" />
          <el-table-column prop="timeWindow" label="时间窗口" min-width="120" />
          <el-table-column prop="cohort" label="队列" min-width="120" />
          <el-table-column prop="observedValue" label="观测值" width="100" />
          <el-table-column prop="confidenceScore" label="置信度" width="100" />
          <el-table-column prop="evidenceLocator" label="证据定位" min-width="150" />
          <el-table-column label="复核状态" min-width="140">
            <template #default="{ row }">
              <el-tag :type="reviewTagType(row.reviewStatus)">{{ row.reviewStatus || '待复核' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="复核操作" min-width="220">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="success" @click="handleReview(row, '已确认')">确认</el-button>
                <el-button link type="danger" @click="handleReview(row, '已拒绝')">驳回</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadIndicators"
            @current-change="loadIndicators"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import HelpTip from '@/components/HelpTip.vue'
import { listLiteratures } from '@/api/modules/literatures'
import { extractIndicators, listLiteratureIndicatorsPaginated, reviewIndicator, reviewAllIndicators, runLiteratureExtraction } from '@/api/modules/extractions'
import type { IndicatorView, LiteratureView } from '@/types/platform'

const route = useRoute()
const literatures = ref<LiteratureView[]>([])
const selectedLiteratureId = ref<number>()
const indicators = ref<IndicatorView[]>([])

const runningExtraction = ref(false)
const runningDirect = ref(false)
const currentActionLabel = ref('')
const currentActionDescription = ref('')

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const isBusy = computed(() => runningExtraction.value || runningDirect.value)

const directForm = reactive({
  literatureId: 1,
  cohort: 'overall population',
  timeWindow: 'week 12',
  content: 'Patients achieved meaningful biomarker decline at week 12 with a favorable safety profile and sustained response in follow-up.'
})

async function loadLiteratureOptions() {
  try {
    const data = await listLiteratures()
    literatures.value = data
    const requestedLiteratureId = Number(route.query.literatureId)
    if (data.length && !selectedLiteratureId.value) {
      selectedLiteratureId.value = data.find((item) => item.id === requestedLiteratureId)?.id ?? data[0].id
      directForm.literatureId = selectedLiteratureId.value
      await loadIndicators()
    }
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

async function loadIndicators() {
  if (!selectedLiteratureId.value) {
    ElMessage.warning('请先选择文献')
    return
  }
  try {
    directForm.literatureId = selectedLiteratureId.value
    const result = await listLiteratureIndicatorsPaginated(selectedLiteratureId.value, pagination.page, pagination.size)
    indicators.value = result.content
    pagination.total = result.totalElements
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

async function runChunkExtraction() {
  if (!selectedLiteratureId.value) {
    ElMessage.warning('请先选择文献')
    return
  }
  runningExtraction.value = true
  currentActionLabel.value = '正在执行指标抽取'
  currentActionDescription.value = '系统正在从文献分块中提取结构化指标。'
  try {
    await runLiteratureExtraction(selectedLiteratureId.value)
    pagination.page = 1
    await loadIndicators()
    ElMessage.success('抽取完成')
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    runningExtraction.value = false
    currentActionLabel.value = ''
    currentActionDescription.value = ''
  }
}

async function runDirectExtraction() {
  if (!directForm.content) {
    ElMessage.warning('请输入要抽取的文本')
    return
  }
  runningDirect.value = true
  currentActionLabel.value = '正在执行文本抽取'
  currentActionDescription.value = '系统正在解析输入文本，请不要重复点击。'
  try {
    await extractIndicators({ ...directForm })
    pagination.page = 1
    await loadIndicators()
    ElMessage.success('文本抽取完成')
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    runningDirect.value = false
    currentActionLabel.value = ''
    currentActionDescription.value = ''
  }
}

function reviewTagType(reviewStatus?: string | null) {
  if (reviewStatus === '已确认') {
    return 'success'
  }
  if (reviewStatus === '已拒绝') {
    return 'danger'
  }
  return 'warning'
}

async function handleReview(indicator: IndicatorView, reviewStatus: '已确认' | '已拒绝') {
  try {
    const { value } = await ElMessageBox.prompt(
      `请填写${reviewStatus === '已确认' ? '确认' : '驳回'}说明`,
      '指标复核',
      {
        inputPlaceholder: '可选填写复核备注',
        inputValue: indicator.reviewerNote ?? '',
        confirmButtonText: '提交',
        cancelButtonText: '取消',
        distinguishCancelAndClose: true
      }
    )
    const updated = await reviewIndicator(indicator.id, {
      reviewStatus,
      reviewerNote: value || ''
    })
    indicators.value = indicators.value.map((item) => (item.id === updated.id ? updated : item))
    ElMessage.success('复核状态已更新')
  } catch (error) {
    const message = (error as Error).message
    if (message === 'cancel' || message === 'close') {
      return
    }
    ElMessage.error(message)
  }
}

async function handleBatchReview(reviewStatus: '已确认' | '已拒绝') {
  if (!selectedLiteratureId.value) {
    ElMessage.warning('请先选择文献')
    return
  }
  try {
    const { value } = await ElMessageBox.prompt(
      `请填写批量${reviewStatus === '已确认' ? '确认' : '驳回'}说明`,
      '批量复核',
      {
        inputPlaceholder: '可选填写复核备注',
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        distinguishCancelAndClose: true
      }
    )
    const count = await reviewAllIndicators(selectedLiteratureId.value, reviewStatus, value || '')
    ElMessage.success(`已${reviewStatus === '已确认' ? '确认' : '驳回'} ${count} 条指标`)
    await loadIndicators()
  } catch (error) {
    const message = (error as Error).message
    if (message === 'cancel' || message === 'close') {
      return
    }
    ElMessage.error(message)
  }
}

onMounted(loadLiteratureOptions)
</script>

<style scoped>
.result-count {
  font-size: 12px;
  color: var(--muted);
  margin-left: 8px;
}

.batch-actions {
  display: flex;
  gap: 8px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
