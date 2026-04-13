# API Examples

This file gives a practical demo sequence for the current backend prototype.

## 1. Login

`POST /api/users/login`

```json
{
  "email": "admin@aiforaso.local",
  "password": "123456"
}
```

Optional registration:

`POST /api/users/register`

```json
{
  "username": "demo_user",
  "email": "demo_user@aiforaso.local",
  "password": "123456"
}
```

## 2. Import a literature file

`POST /api/literatures/import`

```json
{
  "storagePath": "数据示例/Efficacy and Safety of Bepirovirsen in Chronic Hepatitis B Infection.pdf",
  "diseaseArea": "viral hepatitis",
  "keywords": "HBsAg,HBV DNA,ASO"
}
```

## 3. Chunk and vectorize the literature

`POST /api/literatures/1/vectorize`

No request body needed.

## 4. Run chunk-based indicator extraction

`POST /api/extractions/literatures/1/run`

No request body needed.

## 5. Build indicator topology

`POST /api/analysis/literatures/1/topology`

```json
{
  "indicatorName": "HBsAg",
  "rebuildIndicators": false
}
```

## 6. Generate analysis report

`POST /api/analysis/reports`

```json
{
  "question": "Summarize the HBsAg and HBV DNA evidence in this literature.",
  "literatureIds": [1],
  "analysisFocus": "single literature evidence review"
}
```

## 7. Run full workflow

`POST /api/workflows/literatures/1/process`

```json
{
  "question": "Provide a full-process interpretation for this literature.",
  "analysisFocus": "end-to-end ASO evidence analysis",
  "reingest": false,
  "reextract": true
}
```

## 8. Query graph and task history

`GET /api/tasks`

`GET /api/knowledge-graph`

`POST /api/knowledge-graph/query`

```json
{
  "keyword": "HBsAg",
  "nodeType": "indicator"
}
```

## 9. View operation logs

`GET /api/logs`

Optional filter:

`GET /api/logs?operatorEmail=admin@aiforaso.local`
