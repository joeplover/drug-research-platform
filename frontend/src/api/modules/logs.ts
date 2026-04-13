import http from '@/api/http'
import type { OperationLogView } from '@/types/platform'

export async function listLogs(operatorEmail?: string) {
  const { data } = await http.get<OperationLogView[]>('/logs', {
    params: operatorEmail ? { operatorEmail } : undefined
  })
  return data
}
