<template>
  <div ref="containerRef" class="graph-chart"></div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { KnowledgeGraphResponse } from '@/types/platform'

const props = defineProps<{
  graph: KnowledgeGraphResponse
}>()

const containerRef = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null

function renderChart() {
  if (!containerRef.value) {
    return
  }
  if (!chart) {
    chart = echarts.init(containerRef.value)
  }

  const categories = Array.from(new Set(props.graph.nodes.map((node) => node.type))).map((name) => ({ name }))

  chart.setOption({
    tooltip: {
      formatter: (params: { data?: { label?: string; relation?: string; type?: string } }) => {
        if (!params.data) {
          return ''
        }
        if (params.data.relation) {
          return `${params.data.relation}`
        }
        return `${params.data.label}<br/>${params.data.type}`
      }
    },
    legend: [{ data: categories.map((item) => item.name) }],
    series: [
      {
        type: 'graph',
        layout: 'force',
        roam: true,
        symbolSize: 44,
        edgeSymbol: ['none', 'arrow'],
        force: {
          repulsion: 220,
          edgeLength: 110
        },
        label: {
          show: true,
          fontSize: 11,
          formatter: '{b}'
        },
        categories,
        data: props.graph.nodes.map((node) => ({
          ...node,
          name: node.label,
          category: categories.findIndex((item) => item.name === node.type)
        })),
        links: props.graph.edges.map((edge) => ({
          ...edge,
          relation: edge.relation,
          lineStyle: {
            width: 2
          },
          label: {
            show: true,
            formatter: edge.relation
          }
        }))
      }
    ]
  })
}

function resizeChart() {
  chart?.resize()
}

onMounted(async () => {
  await nextTick()
  renderChart()
  window.addEventListener('resize', resizeChart)
})

watch(
  () => props.graph,
  async () => {
    await nextTick()
    renderChart()
  },
  { deep: true }
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
})
</script>
