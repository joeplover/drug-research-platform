<template>
  <el-popover :placement="placement" :width="width" trigger="hover">
    <template #reference>
      <el-button text circle class="help-tip__button">
        <el-icon><QuestionFilled /></el-icon>
      </el-button>
    </template>
    <div class="help-tip">
      <div v-if="title" class="help-tip__title">{{ title }}</div>
      <div class="help-tip__content">
        <slot />
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { QuestionFilled } from '@element-plus/icons-vue'

withDefaults(
  defineProps<{
    title?: string
    width?: number
    placement?: 'top' | 'top-start' | 'top-end' | 'bottom' | 'bottom-start' | 'bottom-end' | 'left' | 'left-start' | 'left-end' | 'right' | 'right-start' | 'right-end'
  }>(),
  {
    title: '',
    width: 360,
    placement: 'top-start'
  }
)
</script>
