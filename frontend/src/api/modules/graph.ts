import http from '@/api/http'
import type { GraphQueryPayload, KnowledgeGraphResponse } from '@/types/platform'

export async function getKnowledgeGraph() {
  const { data } = await http.get<KnowledgeGraphResponse>('/knowledge-graph')
  return data
}

export async function getKnowledgeGraphByLiterature(literatureId: number) {
  const { data } = await http.get<KnowledgeGraphResponse>(`/knowledge-graph/literatures/${literatureId}`)
  return data
}

export async function queryKnowledgeGraph(payload?: GraphQueryPayload) {
  const { data } = await http.post<KnowledgeGraphResponse>('/knowledge-graph/query', payload)
  return data
}
