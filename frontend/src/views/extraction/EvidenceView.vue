<template>
  <div class="page-grid">
    <el-alert
      v-if="currentActionLabel"
      type="warning"
      :closable="false"
      :title="`${currentActionLabel}，请稍候`"
      :description="currentActionDescription"
    />

    <div class="panel" v-loading="searchingRag">
      <div class="section-title">
        <div>
          <h2>文献内证据检索</h2>
        </div>
      </div>

      <el-form :model="ragForm" label-position="top" class="form-grid">
        <el-form-item label="选择检索范围（文献）">
          <el-select v-model="selectedLiteratureIds" multiple placeholder="可多选文献，不选则检索全部" style="width: 100%" :disabled="isBusy">
            <el-option v-for="item in literatures" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="你想确认什么内容" class="full-span">
          <el-input v-model="ragForm.query" placeholder="例如：这篇文献的主要疗效终点是什么？" :disabled="isBusy" />
        </el-form-item>
        <el-form-item label="返回片段数">
          <el-input-number v-model="ragForm.topK" :min="1" :max="50" :disabled="isBusy" />
        </el-form-item>
      </el-form>

      <div class="quick-help">
        <div class="quick-help__label">常用提问示例：</div>
        <div class="tag-list">
          <el-tag class="clickable-tag" effect="plain" @click="fillRagQuery('这篇文献的主要疗效终点是什么？')">疗效终点</el-tag>
          <el-tag class="clickable-tag" effect="plain" @click="fillRagQuery('这篇文献报告了哪些安全性信号？')">安全性信号</el-tag>
          <el-tag class="clickable-tag" effect="plain" @click="fillRagQuery('这篇文献提到了哪些关键生物标志物变化？')">生物标志物</el-tag>
          <el-tag class="clickable-tag" effect="plain" @click="fillRagQuery('治疗组和对照组的疗效差异是什么？')">疗效差异</el-tag>
        </div>
      </div>

      <div class="action-row">
        <el-button type="primary" :loading="searchingRag" :disabled="isBusy" @click="handleRagQuery">执行检索</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="section-title">
        <div>
          <h2>检索命中</h2>
          <span class="result-count">共 {{ pagination.total }} 条</span>
        </div>
      </div>

      <el-empty v-if="!ragHits.length" description="还没有检索结果。先在上方输入问题，再点【执行检索】。" />
      <template v-else>
        <el-table :data="ragHits" stripe>
          <el-table-column prop="title" label="文献" min-width="180" show-overflow-tooltip />
          <el-table-column prop="score" label="得分" width="100">
            <template #default="{ row }">
              {{ row.score?.toFixed(4) ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="retrievalMode" label="检索模式" min-width="120" />
          <el-table-column prop="evidenceLocator" label="定位" min-width="160" />
          <el-table-column prop="snippet" label="片段" min-width="300">
            <template #default="{ row }">
              <el-tooltip :content="row.snippet" placement="top" :show-after="500">
                <span class="snippet-text">{{ row.snippet }}</span>
              </el-tooltip>
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
            @size-change="handleRagQuery"
            @current-change="handleRagQuery"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listLiteratures } from '@/api/modules/literatures'
import { queryRagPaginated } from '@/api/modules/rag'
import type { LiteratureView, RagHitView } from '@/types/platform'

const literatures = ref<LiteratureView[]>([])
const selectedLiteratureIds = ref<number[]>([])
const ragHits = ref<RagHitView[]>([])

const searchingRag = ref(false)
const currentActionLabel = ref('')
const currentActionDescription = ref('')

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const isBusy = computed(() => searchingRag.value)

const ragForm = reactive({
  query: '这篇文献的主要疗效终点是什么？',
  topK: 10
})

async function loadLiteratureOptions() {
  try {
    literatures.value = await listLiteratures()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

function fillRagQuery(query: string) {
  ragForm.query = query
}

async function handleRagQuery() {
  if (!ragForm.query) {
    ElMessage.warning('请输入检索问题')
    return
  }
  searchingRag.value = true
  currentActionLabel.value = '正在执行 RAG 检索'
  currentActionDescription.value = '系统正在检索相关证据片段并排序。'
  try {
    const result = await queryRagPaginated({
      query: ragForm.query,
      topK: ragForm.topK,
      literatureIds: selectedLiteratureIds.value.length > 0 ? selectedLiteratureIds.value : undefined,
      page: pagination.page,
      size: pagination.size
    })
    ragHits.value = result.content
    pagination.total = result.totalElements
    ElMessage.success('检索完成')
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    searchingRag.value = false
    currentActionLabel.value = ''
    currentActionDescription.value = ''
  }
}

onMounted(loadLiteratureOptions)
</script>

<style scoped>
.quick-help {
  margin: 10px 0 12px;
}

.quick-help__label {
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--muted);
}

.clickable-tag {
  cursor: pointer;
}

.clickable-tag:hover {
  opacity: 0.8;
}

.result-count {
  font-size: 12px;
  color: var(--muted);
  margin-left: 8px;
}

.snippet-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
