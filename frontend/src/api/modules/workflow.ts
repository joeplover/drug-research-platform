import http from '@/api/http'
import type { WorkflowProcessRequest, WorkflowProcessResponse } from '@/types/platform'

export async function processLiteratureWorkflow(literatureId: number, payload?: WorkflowProcessRequest) {
  const { data } = await http.post<WorkflowProcessResponse>(`/workflows/literatures/${literatureId}/process`, payload)
  return data
}
