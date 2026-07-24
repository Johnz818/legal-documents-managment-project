# API Contract

## List cases

### `GET /api/cases`

Returns the latest non-archived cases for reusable case-query use cases.

### Request

The endpoint accepts no request body, query parameters, or authentication headers in the current MVP.

Local development URL:

```text
http://localhost:8080/api/cases
```

### Successful response

Status: `200 OK`

```json
{
  "data": [
    {
      "id": 1,
      "caseNumber": "(2026)京01民初1号",
      "caseName": "示例案件",
      "status": "审理中",
      "courtName": "北京市第一中级人民法院",
      "leadLawyerName": "张律师",
      "createdAt": "2026-07-23T10:00:00",
      "updatedAt": "2026-07-23T10:00:00",
      "archived": false
    }
  ]
}
```

When no cases exist, the endpoint returns:

```json
{
  "data": []
}
```

### Field meanings

| Field | Type | Meaning |
| --- | --- | --- |
| `id` | number | Database identifier for the case. |
| `caseNumber` | string | Unique legal case number. |
| `caseName` | string | Display name of the case. |
| `status` | string | Chinese display value for the case status. |
| `courtName` | string or null | Court handling the case. |
| `leadLawyerName` | string | Required lead-lawyer snapshot for the case. |
| `createdAt` | string | ISO-style local date-time when the case was created. |
| `updatedAt` | string | ISO-style local date-time when the case was last updated. |
| `archived` | boolean | Whether the case is archived. Current list results are non-archived. |

### Current query behavior

- Only non-archived cases are returned.
- Results are sorted by creation time descending.
- At most 10 cases are returned.
- Search, filtering, pagination, and caller-controlled sorting are not supported.

### Error handling

Non-2xx responses are treated as request failures by the frontend. The Case List displays an error state without replacing existing data in other frontend modules.

### Future compatibility considerations

- Externalize the backend base URL when deployment environments are introduced.
- Add query parameters only when search, filtering, sorting, or pagination contracts are defined.
- Preserve backward compatibility when adding fields; coordinate breaking field changes between backend and frontend.

## Get case detail

### `GET /api/cases/{id}`

Returns the complete persisted scalar details for one case. Archived cases remain readable.

### Request

The path parameter `id` is the numeric database identifier returned by the Case List API.

Example:

```text
GET http://localhost:8080/api/cases/1
```

### Successful response

Status: `200 OK`

```json
{
  "id": 1,
  "caseNumber": "CASE-001",
  "caseName": "张三诉某公司劳动争议案",
  "status": "审理中",
  "courtName": "上海市浦东新区人民法院",
  "caseCause": "劳动争议",
  "plaintiff": "张三",
  "defendant": "某公司",
  "leadLawyerName": "李律师",
  "filingDate": "2026-07-01",
  "hearingDate": null,
  "judgmentDate": null,
  "description": "劳动合同解除争议",
  "createdAt": "2026-07-22T22:23:53",
  "updatedAt": "2026-07-22T22:23:53",
  "archived": false
}
```

### Field meanings

| Field | Type | Meaning |
| --- | --- | --- |
| `id` | number | Database identifier for the case. |
| `caseNumber` | string | Required, exactly unique legal case number. |
| `caseName` | string | Required display name of the case. |
| `status` | string | Required Chinese display value for the case status. |
| `courtName` | string or null | Court handling the case. |
| `caseCause` | string or null | Legal cause or category of the case. |
| `plaintiff` | string | Required plaintiff or applicant snapshot. |
| `defendant` | string | Required defendant or respondent snapshot. |
| `leadLawyerName` | string | Required lead-lawyer snapshot. |
| `filingDate` | string or null | Filing date in `YYYY-MM-DD` format. |
| `hearingDate` | string or null | Hearing date in `YYYY-MM-DD` format. |
| `judgmentDate` | string or null | Judgment date in `YYYY-MM-DD` format. |
| `description` | string or null | Free-form case description. |
| `createdAt` | string | ISO-style local creation date-time. |
| `updatedAt` | string | ISO-style local last-update date-time. |
| `archived` | boolean | Whether the case is archived. |

### Not found response

When no case exists for the supplied ID:

```text
404 Not Found
```

The endpoint does not return the JPA entity directly.
