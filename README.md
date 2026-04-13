# ASO Platform Prototype

This directory contains the first Spring Boot prototype for the AI-assisted ASO drug R&D platform described in `开题报告.txt` and `AI_for_ASO药物研发平台_初期报告.md`.

## Current scope

- Literature registration and listing
- File-based literature import and chunk ingestion
- Chunk-level indicator extraction with evidence locators
- Hybrid retrieval over stored literature chunks
- Analysis report generation with a mock LLM client
- End-to-end literature processing workflow
- Knowledge graph view for literature-to-indicator links
- Vue 3 frontend covering login, dashboard, literature, extraction, analysis, topology, graph, tasks, logs, and users

## Main endpoints

- `GET /api/health`
- `GET /api/literatures`
- `GET /api/literatures/{id}`
- `GET /api/literatures/search?keyword=...`
- `POST /api/literatures`
- `POST /api/literatures/import`
- `POST /api/literatures/batch-import`
- `POST /api/literatures/{literatureId}/ingest`
- `POST /api/literatures/{literatureId}/vectorize`
- `GET /api/literatures/{literatureId}/chunks`
- `GET /api/users`
- `POST /api/users`
- `POST /api/users/login`
- `POST /api/users/register`
- `PATCH /api/users/{id}/role`
- `POST /api/extractions/indicators`
- `GET /api/extractions/literatures/{literatureId}/indicators`
- `POST /api/extractions/literatures/{literatureId}/run`
- `POST /api/rag/query`
- `POST /api/analysis/reports`
- `POST /api/analysis/literatures/{literatureId}/topology`
- `GET /api/analysis/literatures/{literatureId}/topology`
- `GET /api/tasks`
- `GET /api/tasks/{id}`
- `POST /api/workflows/literatures/{literatureId}/process`
- `GET /api/knowledge-graph`
- `GET /api/knowledge-graph/literatures/{literatureId}`
- `POST /api/knowledge-graph/query`
- `GET /api/logs`

## Notes

- The default configuration now connects directly to MySQL and Redis at `192.168.8.128`.
- Redis, vector DB, and real LLM integration are represented by replaceable service layers in this first iteration.
- AI report generation now defaults to `DeepSeek` chat completion, and embedding generation defaults to local `Ollama`.
- PDF and Excel parsing are wired for chunk ingestion; unsupported formats currently fall back to metadata-only import text.
- Chunk retrieval now combines keyword overlap with a local semantic signature, so it can later be swapped to pgvector or Milvus without changing the controller layer.
- Demo literature records are loaded on startup from titles aligned with the sample data in the repository.

## Configuration

- Default MySQL connection is `192.168.8.128:3306`, database `drug_research_platform`, username `root`, password `root123`.
- Default Redis connection is `192.168.8.128:6379`, database `0`.
- Optional environment variables: `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_DATABASE`, `AI_PROVIDER`, `AI_MODEL`, `AI_BASE_URL`, `DEEPSEEK_API_KEY`, `EMBEDDING_PROVIDER`, `EMBEDDING_MODEL`, `EMBEDDING_BASE_URL`, `EMBEDDING_FALLBACK_ENABLED`.

## AI defaults

- Chat model: `deepseek-chat`
- Chat base URL: `https://api.deepseek.com`
- Embedding provider: local `Ollama`
- Embedding model: `qwen3-embedding:4b`
- Embedding base URL: `http://127.0.0.1:11434`
- When Ollama is unavailable, the system falls back to a local deterministic embedding so the demo can still run.

## SQL

- MySQL initialization script: `sql/init_mysql.sql`
- The script includes database creation, table creation, indexes, foreign keys, and demo seed data.

## Docs

- Requirements matrix: `docs/REQUIREMENTS_MATRIX.md`
- API demo sequence: `docs/API_EXAMPLES.md`

## Frontend

- Frontend directory: `frontend`
- Tech stack: `Vue 3 + Vite + TypeScript + Pinia + Element Plus + ECharts`
- Install dependencies: `npm.cmd install`
- Start local frontend: `npm.cmd run dev`
- Build frontend: `npm.cmd run build`
- Default dev proxy target: `http://localhost:8080`
- Demo login emails: `admin@aiforaso.local`, `reviewer@aiforaso.local`
- Demo default password: `123456`
"# drug-research-platform" 
