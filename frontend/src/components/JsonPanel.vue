<template>
  <div class="mono-block">{{ content }}</div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  value: unknown
}>()

const content = computed(() => JSON.stringify(props.value, null, 2))
</script>
