import http from '@/api/http'
import type {
  AnalysisReportRequest,
  AnalysisReportResponse,
  TopologyBuildRequest,
  TopologyBuildResponse
} from '@/types/platform'

export async function generateAnalysisReport(payload: AnalysisReportRequest) {
  const { data } = await http.post<AnalysisReportResponse>('/analysis/reports', payload)
  return data
}

export async function buildTopology(literatureId: number, payload?: TopologyBuildRequest) {
  const { data } = await http.post<TopologyBuildResponse>(`/analysis/literatures/${literatureId}/topology`, payload)
  return data
}

export async function getTopology(literatureId: number) {
  const { data } = await http.get<TopologyBuildResponse>(`/analysis/literatures/${literatureId}/topology`)
  return data
}
