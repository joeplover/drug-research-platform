<template>
  <div class="page-grid">
    <div class="panel">
      <div class="section-title">
        <div>
          <h2>知识图谱检索</h2>
        </div>
      </div>
      <el-form :model="form" label-position="top" class="form-grid">
        <el-form-item label="文献范围">
          <el-select v-model="form.literatureId" clearable placeholder="不选则加载全量图谱" style="width: 100%" @change="loadGraph">
            <el-option v-for="item in literatures" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="节点类型">
          <el-select v-model="form.nodeType" clearable placeholder="请选择节点类型" style="width: 100%">
            <el-option label="文献" value="文献" />
            <el-option label="疾病领域" value="疾病领域" />
            <el-option label="来源类型" value="来源类型" />
            <el-option label="关键词" value="关键词" />
            <el-option label="指标-已确认" value="指标-已确认" />
            <el-option label="指标-待复核" value="指标-待复核" />
            <el-option label="指标-已拒绝" value="指标-已拒绝" />
            <el-option label="指标类别" value="指标类别" />
            <el-option label="队列" value="队列" />
            <el-option label="时间窗口" value="时间窗口" />
            <el-option label="指标状态" value="指标状态" />
            <el-option label="任务" value="任务" />
          </el-select>
        </el-form-item>
        <el-form-item label="复核状态">
          <el-select v-model="form.reviewStatus" clearable placeholder="按指标复核状态过滤" style="width: 100%">
            <el-option label="已确认" value="已确认" />
            <el-option label="待复核" value="待复核" />
            <el-option label="已拒绝" value="已拒绝" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词" class="full-span">
          <el-input v-model="form.keyword" placeholder="按节点标签关键字过滤图谱" />
        </el-form-item>
      </el-form>
      <div class="action-row">
        <el-button @click="loadGraph">加载图谱</el-button>
        <el-button type="primary" @click="handleQueryGraph">查询图谱</el-button>
      </div>
    </div>

    <div class="stats-grid">
      <StatCard label="节点数" :value="graph.nodes.length" />
      <StatCard label="关系数" :value="graph.edges.length" />
      <StatCard label="文献节点" :value="nodeCount('文献')" />
      <StatCard label="指标节点" :value="nodeCount('指标')" />
    </div>

    <div class="panel">
      <div class="section-title">
        <div>
          <h2>图谱可视化</h2>
        </div>
      </div>
      <GraphChart v-if="graph.nodes.length" :graph="graph" />
      <el-empty v-else description="暂无图谱数据" />
    </div>

    <div class="split-grid">
      <div class="panel">
        <div class="section-title">
          <div>
            <h2>节点列表</h2>
          </div>
        </div>
        <el-table :data="graph.nodes" stripe>
          <el-table-column prop="id" label="节点 ID" min-width="180" />
          <el-table-column prop="label" label="标签" min-width="160" />
          <el-table-column prop="type" label="类型" width="120" />
        </el-table>
      </div>

      <div class="panel">
        <div class="section-title">
          <div>
            <h2>关系列表</h2>
          </div>
        </div>
        <el-table :data="graph.edges" stripe>
          <el-table-column prop="source" label="源节点" min-width="160" />
          <el-table-column prop="target" label="目标节点" min-width="160" />
          <el-table-column prop="relation" label="关系" min-width="140" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import GraphChart from '@/components/GraphChart.vue'
import StatCard from '@/components/StatCard.vue'
import { listLiteratures } from '@/api/modules/literatures'
import { getKnowledgeGraph, getKnowledgeGraphByLiterature, queryKnowledgeGraph } from '@/api/modules/graph'
import type { KnowledgeGraphResponse, LiteratureView } from '@/types/platform'

const literatures = ref<LiteratureView[]>([])
const graph = ref<KnowledgeGraphResponse>({ nodes: [], edges: [] })
const route = useRoute()

const form = reactive({
  literatureId: undefined as number | undefined,
  keyword: '',
  nodeType: '',
  reviewStatus: ''
})

function nodeCount(type: string) {
  if (type === '指标') {
    return graph.value.nodes.filter((item) =>
      ['指标-已确认', '指标-待复核', '指标-已拒绝'].includes(item.type)
    ).length
  }
  return graph.value.nodes.filter((item) => item.type === type).length
}

async function loadOptions() {
  try {
    literatures.value = await listLiteratures()
    const requestedLiteratureId = Number(route.query.literatureId)
    if (requestedLiteratureId) {
      form.literatureId = literatures.value.find((item) => item.id === requestedLiteratureId)?.id
    }
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

async function loadGraph() {
  try {
    graph.value = form.literatureId
      ? await getKnowledgeGraphByLiterature(form.literatureId)
      : await getKnowledgeGraph()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

async function handleQueryGraph() {
  try {
    graph.value = await queryKnowledgeGraph({
      literatureId: form.literatureId || undefined,
      keyword: form.keyword || undefined,
      nodeType: form.nodeType || undefined,
      reviewStatus: form.reviewStatus || undefined
    })
    ElMessage.success('图谱查询完成')
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

onMounted(async () => {
  await loadOptions()
  await loadGraph()
})
</script>
