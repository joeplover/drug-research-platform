import http from '@/api/http'
import type { AnalysisTaskDetailView, AnalysisTaskView } from '@/types/platform'

export async function listTasks() {
  const { data } = await http.get<AnalysisTaskView[]>('/tasks')
  return data
}

export async function getTask(id: number) {
  const { data } = await http.get<AnalysisTaskView>(`/tasks/${id}`)
  return data
}

export async function getTaskDetail(id: number) {
  const { data } = await http.get<AnalysisTaskDetailView>(`/tasks/${id}/detail`)
  return data
}

export async function deleteTask(id: number) {
  await http.delete(`/tasks/${id}`)
}
