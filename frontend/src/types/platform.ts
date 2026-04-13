export interface HealthInfo {
  status: string
  service: string
  timestamp: string
  components: HealthComponentView[]
}

export interface HealthComponentView {
  name: string
  status: string
  detail: string
  latencyMs: number
}

export interface LiteratureView {
  id: number
  title: string
  sourceType: string
  diseaseArea: string
  summary: string
  keywords: string
  publicationDate: string | null
  storagePath: string | null
  vectorSyncStatus: string | null
  vectorSyncDetail: string | null
  vectorSyncedChunkCount: number | null
  vectorSyncedAt: string | null
  createdAt: string
  createdBy: number | null
}

export interface LiteratureCreatePayload {
  title: string
  sourceType: string
  diseaseArea: string
  summary: string
  keywords?: string
  publicationDate?: string
  storagePath?: string
}

export interface LiteratureImportPayload {
  storagePath: string
  title?: string
  sourceType?: string
  diseaseArea?: string
  keywords?: string
  publicationDate?: string
}

export interface BatchLiteratureImportPayload {
  items: LiteratureImportPayload[]
  autoIngest: boolean
}

export interface LiteratureChunkView {
  id: number
  literatureId: number
  chunkIndex: number
  sourceSection: string
  chunkLabel: string
  content: string
}

export interface LiteratureVectorStatusView {
  literatureId: number
  chunkCount: number
  embeddedChunkCount: number
  milvusStatus: string
  embeddingMode: string
  message: string
}

export interface LiteratureOverviewView {
  literatureId: number
  title: string
  processingStage: string
  chunkCount: number
  indicatorCount: number
  overviewSummary: string
  researchFocus: string
  methodSummary: string
  resultSummary: string
  safetySummary: string
  conclusionSummary: string
  keyPoints: string[]
  keyConcepts: string[]
  evidenceHighlights: string[]
  keyIndicators: IndicatorView[]
  graph: KnowledgeGraphResponse
}

export interface IndicatorView {
  id: number
  literatureId: number
  indicatorName: string
  category: string
  timeWindow: string
  cohort: string
  observedValue: number | null
  confidenceScore: number | null
  evidenceSnippet: string
  evidenceLocator: string
  reviewStatus: string | null
  reviewerNote: string | null
  reviewedAt: string | null
}

export interface IndicatorExtractionPayload {
  literatureId?: number
  content: string
  cohort?: string
  timeWindow?: string
}

export interface IndicatorReviewPayload {
  reviewStatus: string
  reviewerNote?: string
}

export interface RagQueryPayload {
  query: string
  topK: number
  literatureIds?: number[]
}

export interface RagHitView {
  literatureId: number
  title: string
  snippet: string
  score: number
  sourceType: string
  publicationDate: string | null
  evidenceLocator: string
  retrievalMode: string
}

export interface AnalysisReportRequest {
  question: string
  literatureIds?: number[]
  analysisFocus?: string
  onlyConfirmedIndicators?: boolean
}

export interface AnalysisReportResponse {
  taskId: number
  taskStatus: string
  reportSummary: string
  evidence: RagHitView[]
  indicators: IndicatorView[]
}

export interface WorkflowProcessRequest {
  question?: string
  analysisFocus?: string
  reingest: boolean
  reextract: boolean
  onlyConfirmedIndicators?: boolean
}

export interface IndicatorReviewSummaryView {
  totalCount: number
  pendingCount: number
  confirmedCount: number
  rejectedCount: number
}

export interface WorkflowProcessResponse {
  literatureId: number
  chunkCount: number
  indicatorCount: number
  report: AnalysisReportResponse
  chunks: LiteratureChunkView[]
  indicators: IndicatorView[]
}

export interface IndicatorStateView {
  id: number
  literatureId: number
  indicatorName: string
  stageType: string
  stateOrder: number
  stateLabel: string
  description: string
  evidenceLocator: string
}

export interface StateTransitionView {
  id: number
  literatureId: number
  fromStateId: number
  toStateId: number
  conditionText: string
  transitionProbability: number | null
  evidenceLocator: string
}

export interface TopologyBuildRequest {
  indicatorName?: string
  rebuildIndicators: boolean
}

export interface KnowledgeGraphNode {
  id: string
  label: string
  type: string
}

export interface KnowledgeGraphEdge {
  source: string
  target: string
  relation: string
}

export interface KnowledgeGraphResponse {
  nodes: KnowledgeGraphNode[]
  edges: KnowledgeGraphEdge[]
}

export interface TopologyBuildResponse {
  taskId: number
  literatureId: number
  indicatorName: string
  states: IndicatorStateView[]
  transitions: StateTransitionView[]
  graph: KnowledgeGraphResponse
}

export interface GraphQueryPayload {
  literatureId?: number
  keyword?: string
  nodeType?: string
  reviewStatus?: string
}

export interface AnalysisTaskView {
  id: number
  taskType: string
  status: string
  inputText: string
  resultSummary: string
  citations: string
  contextLiteratureIds: string
  createdAt: string
}

export interface AnalysisTaskDetailView extends AnalysisTaskView {
  analysisFocus: string | null
  evidence: RagHitView[]
  indicators: IndicatorView[]
}

export interface OperationLogView {
  id: number
  operatorEmail: string
  actionType: string
  resourceType: string
  resourceId: string
  detail: string
  createdAt: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

export interface RagQueryPaginatedPayload extends RagQueryPayload {
  page?: number
  size?: number
}
