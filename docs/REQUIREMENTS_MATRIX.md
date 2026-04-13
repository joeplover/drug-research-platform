# Requirements Matrix

This matrix aligns the current implementation with `AI_for_ASO药物研发平台_初期报告.md`.

## Status Legend

- `Implemented`: available in the current backend prototype
- `Partial`: basic prototype exists but does not yet match the report target
- `Missing`: not yet implemented

## Functional Matrix

| Report Area | Requirement | Status | Notes |
|---|---|---|---|
| Literature Management | Upload/import literature | Partial | File-path import exists; browser upload is not added yet |
| Literature Management | Metadata management | Implemented | Literature CRUD-lite exists |
| Literature Management | Semantic search | Partial | Hybrid local retrieval exists; dedicated vector DB is not connected |
| Literature Management | Vectorization storage | Partial | Local embedding signature exists; no Milvus/Qdrant backend yet |
| Knowledge Extraction | PDF parsing | Implemented | PDF and Excel parsing are available |
| Knowledge Extraction | Entity recognition | Partial | Indicator-oriented extraction exists; full NER is not implemented |
| Knowledge Extraction | Relation extraction | Partial | Evidence relations are implicit; formal RE pipeline is absent |
| Knowledge Extraction | Batch/task extraction | Missing | No async batch task API yet |
| AI Analysis Task 1 | Single-indicator explanation | Implemented | Report generation API exists |
| AI Analysis Task 2 | Topology build | Implemented | State, transition, topology API, and task persistence are available |
| AI Analysis Task 3 | Full-process analysis | Partial | Workflow exists, but dedicated report output model needs strengthening |
| RAG | Retrieval-augmented generation | Implemented | Chunk retrieval + AI report generation are available |
| Agent | Planning/execution/memory agent | Missing | Workflow orchestration exists, but not a true agent framework |
| Knowledge Graph | Graph build | Implemented | Global graph and literature-scoped graph are available |
| Knowledge Graph | Graph query/reasoning | Partial | Lightweight query API exists; no Cypher or inference engine yet |
| User Management | Register/list users | Implemented | Basic create/list exists |
| User Management | Login/auth/permission | Partial | Lightweight login and role/status update exist; full JWT/security chain is not implemented |
| User Management | Operation logs/history | Implemented | Task history and operation logs are available |
| Infrastructure | MySQL | Implemented | `application.yml` + SQL script are ready |
| Infrastructure | Redis cache | Implemented | Redis cache manager is configured |
| Infrastructure | Neo4j | Missing | Not connected |
| Infrastructure | Frontend (Vue/ECharts) | Missing | Backend only at the moment |

## Current Priority

1. Strengthen workflow output for full-process analysis
2. Add batch extraction/task status support
3. Add full JWT/security chain if needed
4. Add Neo4j or Cypher-compatible graph querying
5. Add frontend demo page for defense presentation
