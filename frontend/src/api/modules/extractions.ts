import http from '@/api/http'
import type { IndicatorExtractionPayload, IndicatorReviewPayload, IndicatorReviewSummaryView, IndicatorView, PageResponse } from '@/types/platform'

export async function extractIndicators(payload: IndicatorExtractionPayload) {
  const { data } = await http.post<IndicatorView[]>('/extractions/indicators', payload)
  return data
}

export async function listLiteratureIndicators(literatureId: number) {
  const { data } = await http.get<IndicatorView[]>(`/extractions/literatures/${literatureId}/indicators`)
  return data
}

export async function listLiteratureIndicatorsPaginated(literatureId: number, page: number = 1, size: number = 10) {
  const { data } = await http.get<PageResponse<IndicatorView>>(`/extractions/literatures/${literatureId}/indicators/page`, {
    params: { page: page - 1, size }
  })
  return data
}

export async function runLiteratureExtraction(literatureId: number) {
  const { data } = await http.post<IndicatorView[]>(`/extractions/literatures/${literatureId}/run`)
  return data
}

export async function reviewIndicator(indicatorId: number, payload: IndicatorReviewPayload) {
  const { data } = await http.post<IndicatorView>(`/extractions/indicators/${indicatorId}/review`, payload)
  return data
}

export async function reviewAllIndicators(literatureId: number, reviewStatus: string, reviewerNote?: string) {
  const { data } = await http.post<number>(`/extractions/literatures/${literatureId}/indicators/review-all`, null, {
    params: { reviewStatus, reviewerNote }
  })
  return data
}

export async function getIndicatorReviewSummary() {
  const { data } = await http.get<IndicatorReviewSummaryView>('/extractions/summary')
  return data
}
