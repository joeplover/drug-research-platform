<template>
  <div class="stat-card">
    <div class="stat-card__label">{{ label }}</div>
    <div class="stat-card__value">{{ value }}</div>
    <div v-if="hint" class="muted">{{ hint }}</div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
  hint?: string
}>()
</script>
