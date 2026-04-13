<template>
  <div class="app-shell">
    <aside class="app-sidebar">
      <div class="brand">
        <div class="brand__mark">DR</div>
        <div>
          <div class="brand__title">药物研发辅助平台</div>
          <div class="brand__subtitle">Drug Research Assistant Platform</div>
        </div>
      </div>

      <el-menu :default-active="route.path" class="menu" router>
        <el-menu-item index="/dashboard">
          <el-icon><House /></el-icon>
          <span>平台概览</span>
        </el-menu-item>
        <el-menu-item index="/literatures">
          <el-icon><Document /></el-icon>
          <span>文献管理</span>
        </el-menu-item>
        <el-sub-menu index="extraction">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>指标抽取</span>
          </template>
          <el-menu-item index="/extraction/indicators">文献指标抽取</el-menu-item>
          <el-menu-item index="/extraction/evidence">文献内证据检索</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/graph">
          <el-icon><Share /></el-icon>
          <span>知识图谱</span>
        </el-menu-item>
        <el-menu-item index="/topology">
          <el-icon><Connection /></el-icon>
          <span>拓扑分析</span>
        </el-menu-item>
        <el-menu-item index="/analysis">
          <el-icon><DocumentCopy /></el-icon>
          <span>分析报告</span>
        </el-menu-item>
        <el-menu-item index="/workflow">
          <el-icon><VideoPlay /></el-icon>
          <span>流程演示</span>
        </el-menu-item>
        <el-menu-item index="/tasks">
          <el-icon><List /></el-icon>
          <span>任务中心</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/logs">
          <el-icon><Tickets /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <main class="app-main">
      <div class="app-header">
        <div>
          <h1>{{ title }}</h1>
          <p>{{ subtitle }}</p>
        </div>
        <div class="header-actions">
          <div class="user-chip">{{ userStore.username }}（{{ userStore.role }}）</div>
          <el-button type="danger" plain size="small" @click="logout">退出登录</el-button>
        </div>
      </div>

      <div class="app-content">
        <RouterView :key="route.fullPath" />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  Connection,
  DataAnalysis,
  Document,
  DocumentCopy,
  House,
  List,
  Share,
  Tickets,
  User,
  VideoPlay
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const metaMap: Record<string, { title: string; subtitle: string }> = {
  '/dashboard': { title: '平台概览', subtitle: '查看系统状态、平台数据和当前整体运行情况。' },
  '/literatures': { title: '文献管理', subtitle: '导入文献后，查看摘要、关键指标、证据片段和图谱概览。' },
  '/extraction/indicators': { title: '文献指标抽取', subtitle: '执行结构化指标抽取，并查看证据片段。' },
  '/extraction/evidence': { title: '文献内证据检索', subtitle: '基于语义检索文献中的相关证据片段。' },
  '/analysis': { title: '分析报告', subtitle: '生成分析报告，并执行端到端文献处理流程。' },
  '/graph': { title: '知识图谱', subtitle: '查看当前文献和全库文献的结构化关系图谱。' },
  '/workflow': { title: '流程演示', subtitle: '用于展示完整处理流程。' },
  '/topology': { title: '拓扑分析', subtitle: '构建指标状态与转移关系的拓扑结构。' },
  '/tasks': { title: '任务中心', subtitle: '查看分析、拓扑和工作流产生的任务记录。' },
  '/logs': { title: '操作日志', subtitle: '跟踪系统关键业务动作与审计信息。' },
  '/users': { title: '用户管理', subtitle: '维护演示账号、角色和状态。' }
}

const title = computed(() => metaMap[route.path]?.title ?? '药物研发辅助平台')
const subtitle = computed(() => metaMap[route.path]?.subtitle ?? '面向药物研发文献分析与决策支持的平台。')

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>
