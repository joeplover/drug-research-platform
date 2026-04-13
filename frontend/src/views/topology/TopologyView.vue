<template>
  <div class="page-grid">
    <div class="panel">
      <div class="section-title">
        <div>
          <h2>指标拓扑构建</h2>
        </div>
      </div>
      <el-form :model="form" label-position="top" class="form-grid">
        <el-form-item label="文献">
          <el-select v-model="form.literatureId" placeholder="请选择文献" style="width: 100%">
            <el-option v-for="item in literatures" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="核心指标名称">
          <el-input v-model="form.indicatorName" placeholder="例如 HBsAg / biomarker / efficacy endpoint" />
        </el-form-item>
        <el-form-item label="重新抽取指标">
          <el-switch v-model="form.rebuildIndicators" />
        </el-form-item>
      </el-form>
      <div class="action-row">
        <el-button @click="loadExistingTopology">加载现有拓扑</el-button>
        <el-button type="primary" :loading="building" @click="handleBuild">生成拓扑</el-button>
      </div>
    </div>

    <div class="split-grid">
      <div class="panel">
        <div class="section-title">
          <div>
            <h2>状态列表</h2>
          </div>
        </div>
        <el-table :data="topology?.states ?? []" stripe>
          <el-table-column prop="stateOrder" label="序号" width="90" />
          <el-table-column prop="indicatorName" label="指标" min-width="140" />
          <el-table-column prop="stageType" label="阶段" width="120" />
          <el-table-column prop="stateLabel" label="状态标签" min-width="160" />
          <el-table-column prop="evidenceLocator" label="证据定位" min-width="150" />
        </el-table>
      </div>

      <div class="panel">
        <div class="section-title">
          <div>
            <h2>状态转移</h2>
          </div>
        </div>
        <el-table :data="transitionRows" stripe>
          <el-table-column prop="fromLabel" label="起始状态" min-width="140" />
          <el-table-column prop="toLabel" label="目标状态" min-width="140" />
          <el-table-column prop="conditionText" label="条件描述" min-width="220" show-overflow-tooltip />
          <el-table-column prop="transitionProbability" label="概率" width="100" />
          <el-table-column prop="evidenceLocator" label="证据定位" min-width="140" />
        </el-table>
      </div>
    </div>

    <div class="panel">
      <div class="section-title">
        <div>
          <h2>拓扑图谱</h2>
        </div>
      </div>
      <GraphChart v-if="topology?.graph.nodes.length" :graph="topology.graph" />
      <el-empty v-else description="暂无拓扑图数据" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import GraphChart from '@/components/GraphChart.vue'
import { listLiteratures } from '@/api/modules/literatures'
import { buildTopology, getTopology } from '@/api/modules/analysis'
import type { LiteratureView, TopologyBuildResponse } from '@/types/platform'

const literatures = ref<LiteratureView[]>([])
const topology = ref<TopologyBuildResponse | null>(null)
const building = ref(false)

const form = reactive({
  literatureId: undefined as number | undefined,
  indicatorName: '',
  rebuildIndicators: false
})

const transitionRows = computed(() => {
  if (!topology.value) {
    return []
  }
  const stateMap = new Map(topology.value.states.map((item) => [item.id, item.stateLabel]))
  return topology.value.transitions.map((item) => ({
    ...item,
    fromLabel: stateMap.get(item.fromStateId) ?? item.fromStateId,
    toLabel: stateMap.get(item.toStateId) ?? item.toStateId
  }))
})

async function loadOptions() {
  try {
    const data = await listLiteratures()
    literatures.value = data
    if (data.length && !form.literatureId) {
      form.literatureId = data[0].id
    }
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

async function loadExistingTopology() {
  if (!form.literatureId) {
    ElMessage.warning('请先选择文献')
    return
  }
  try {
    topology.value = await getTopology(form.literatureId)
    ElMessage.success('已加载现有拓扑')
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

async function handleBuild() {
  if (!form.literatureId) {
    ElMessage.warning('请先选择文献')
    return
  }
  building.value = true
  try {
    topology.value = await buildTopology(form.literatureId, {
      indicatorName: form.indicatorName || undefined,
      rebuildIndicators: form.rebuildIndicators
    })
    ElMessage.success('拓扑构建完成')
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    building.value = false
  }
}

onMounted(loadOptions)
</script>
