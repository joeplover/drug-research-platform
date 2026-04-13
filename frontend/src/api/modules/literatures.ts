import http from '@/api/http'
import type {
  BatchLiteratureImportPayload,
  LiteratureChunkView,
  LiteratureCreatePayload,
  LiteratureImportPayload,
  LiteratureOverviewView,
  LiteratureVectorStatusView,
  LiteratureView,
  RagHitView
} from '@/types/platform'

export async function listLiteratures(keyword?: string) {
  const { data } = await http.get<LiteratureView[]>('/literatures', {
    params: keyword ? { keyword } : undefined
  })
  return data
}

export async function getLiterature(id: number) {
  const { data } = await http.get<LiteratureView>(`/literatures/${id}`)
  return data
}

export async function deleteLiterature(id: number) {
  await http.delete(`/literatures/${id}`)
}

export async function createLiterature(payload: LiteratureCreatePayload) {
  const { data } = await http.post<LiteratureView>('/literatures', payload)
  return data
}

export async function importLiterature(payload: LiteratureImportPayload) {
  const { data } = await http.post<LiteratureView>('/literatures/import', payload)
  return data
}

export async function uploadLiterature(formData: FormData) {
  const { data } = await http.post<LiteratureView>('/literatures/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
  return data
}

export async function batchImportLiteratures(payload: BatchLiteratureImportPayload) {
  const { data } = await http.post<LiteratureView[]>('/literatures/batch-import', payload)
  return data
}

export async function ingestLiterature(literatureId: number) {
  const { data } = await http.post<LiteratureChunkView[]>(`/literatures/${literatureId}/ingest`)
  return data
}

export async function vectorizeLiterature(literatureId: number) {
  const { data } = await http.post<LiteratureChunkView[]>(`/literatures/${literatureId}/vectorize`)
  return data
}

export async function listLiteratureChunks(literatureId: number) {
  const { data } = await http.get<LiteratureChunkView[]>(`/literatures/${literatureId}/chunks`)
  return data
}

export async function getLiteratureVectorStatus(literatureId: number) {
  const { data } = await http.get<LiteratureVectorStatusView>(`/literatures/${literatureId}/vector-status`)
  return data
}

export async function getLiteratureOverview(literatureId: number) {
  const { data } = await http.get<LiteratureOverviewView>(`/literatures/${literatureId}/overview`)
  return data
}

export async function searchLiteratures(keyword: string, topK = 5, literatureId?: number) {
  const { data } = await http.get<RagHitView[]>('/literatures/search', {
    params: {
      keyword,
      topK,
      literatureId
    }
  })
  return data
}
