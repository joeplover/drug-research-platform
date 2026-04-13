import http from '@/api/http'
import type { RagHitView, RagQueryPayload, RagQueryPaginatedPayload, PageResponse } from '@/types/platform'

export async function queryRag(payload: RagQueryPayload) {
  const { data } = await http.post<RagHitView[]>('/rag/query', payload)
  return data
}

export async function queryRagPaginated(payload: RagQueryPaginatedPayload) {
  const { data } = await http.post<PageResponse<RagHitView>>('/rag/query/page', payload)
  return data
}
