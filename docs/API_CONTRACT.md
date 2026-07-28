# API Contract

## List cases

### `GET /api/cases`

Returns the latest non-archived cases for reusable case-query use cases.

### Request

The endpoint accepts no request body or authentication headers in the current MVP.

Optional query parameters:

| Parameter | Matching rule |
| --- | --- |
| `caseNumberPrefix` | Case number starts with the supplied value. |
| `caseNamePrefix` | Case name starts with the supplied value. |
| `status` | Exact backend status enum value. |
| `leadLawyerName` | Exact lead-lawyer snapshot name. |
| `archiveState` | `ACTIVE` or `ARCHIVED`; defaults to `ACTIVE`. |

Supported status values:

- `PENDING_FILING`
- `PRE_TRIAL_PREPARATION`
- `IN_TRIAL`
- `JUDGMENT_PENDING_APPEAL`
- `APPEAL_IN_PROGRESS`
- `FINAL_JUDGMENT`
- `IN_ENFORCEMENT`
- `CLOSED`

Blank parameters are treated as absent. When multiple parameters are supplied, all criteria must match.

Local development URL:

```text
http://localhost:8080/api/cases
```

Example:

```text
http://localhost:8080/api/cases?caseNumberPrefix=(2016)浙01&status=IN_TRIAL
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
      "caseCause": "劳动争议",
      "plaintiff": "张三",
      "leadLawyerName": "张律师",
      "filingDate": "2026-07-01",
      "hearingDate": "2026-08-15",
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
| `caseCause` | string or null | Legal cause or category of the case. |
| `plaintiff` | string | Required plaintiff or applicant snapshot. |
| `leadLawyerName` | string | Required lead-lawyer snapshot for the case. |
| `filingDate` | string or null | Filing date in `YYYY-MM-DD` format. |
| `hearingDate` | string or null | Hearing date in `YYYY-MM-DD` format. |
| `createdAt` | string | ISO-style local date-time when the case was created. |
| `updatedAt` | string | ISO-style local date-time when the case was last updated. |
| `archived` | boolean | Whether the case is archived. Current list results are non-archived. |

### Current query behavior

- Only non-archived cases are returned.
- Supplying `archiveState=ARCHIVED` returns archived cases instead.
- Results are sorted by creation time descending.
- At most 10 cases are returned.
- Case number and case name use prefix matching rather than arbitrary contains matching.
- Status and lead-lawyer name use exact matching.
- Pagination and caller-controlled sorting are not supported.

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
  "archived": false,
  "version": 0
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
| `version` | number | Concurrency version to send with a subsequent Case update. |

### Not found response

When no case exists for the supplied ID:

```text
404 Not Found
```

The endpoint does not return the JPA entity directly.

### Frontend integration TODO

The legacy Case Detail mock model also supplied fields that are not available from this endpoint:

- Case tags.
- Supporting lawyers or other case team members.
- Related documents.
- Related reminders.

The documents and reminders sections remain mock-backed and normally show their existing empty states for numeric backend case IDs. Tags and supporting members are no longer synthesized from legacy case mocks. All four areas remain deferred until their relationship models and APIs are implemented.

## Create case

### `POST /api/cases`

Creates one non-archived case using the approved core Case model.

### Request

Content type:

```text
application/json
```

Example:

```json
{
  "caseNumber": "(2026)沪0115民初1001号",
  "caseName": "张三诉某公司劳动争议案",
  "status": "IN_TRIAL",
  "courtName": "上海市浦东新区人民法院",
  "caseCause": "劳动争议",
  "plaintiff": "张三",
  "defendant": "某公司",
  "leadLawyerName": "李律师",
  "filingDate": "2026-07-01",
  "hearingDate": "2026-08-15",
  "judgmentDate": null,
  "description": "劳动合同解除争议"
}
```

Required fields:

- `caseNumber`
- `caseName`
- `status`
- `plaintiff`
- `defendant`
- `leadLawyerName`

Optional fields:

- `courtName`
- `caseCause`
- `filingDate`
- `hearingDate`
- `judgmentDate`
- `description`

Dates use `YYYY-MM-DD`. Status uses one of the backend enum values documented by the Case List endpoint.

The client cannot supply the database ID, creation or update timestamps, or archived state. A newly created case is always non-archived.

### Successful response

Status: `201 Created`

The `Location` response header identifies the new Case Detail resource:

```text
Location: http://localhost:8080/api/cases/1
```

The response body uses the complete Case Detail response contract.

### Error responses

| Status | Meaning |
| --- | --- |
| `400 Bad Request` | A required value is absent or blank, a value exceeds its supported length, the status is unsupported, or a date is malformed. |
| `409 Conflict` | The supplied case number already exists. |

File upload is not part of this endpoint. Case-related PDF and Word documents will be introduced as a separate capability against an already-created Case resource.

## Update case

### `PUT /api/cases/{id}`

Replaces the editable scalar fields of an existing case. It does not change the Case ID, timestamps, or archived state.

### Request

Content type:

```text
application/json
```

The request contains the same required and optional scalar fields as Case creation, plus the required `version` returned by Case Detail:

```json
{
  "caseNumber": "(2026)沪0115民初1001号",
  "caseName": "张三诉某公司劳动争议案",
  "status": "IN_TRIAL",
  "courtName": "上海市浦东新区人民法院",
  "caseCause": "劳动争议",
  "plaintiff": "张三",
  "defendant": "某公司",
  "leadLawyerName": "李律师",
  "filingDate": "2026-07-01",
  "hearingDate": null,
  "judgmentDate": null,
  "description": "更新后的案件说明",
  "version": 0
}
```

Omitted or null optional fields are cleared. Required fields, supported lengths, date formats, and status values follow the Case creation contract.

### Successful response

Status: `200 OK`

The response uses the complete Case Detail contract and contains the incremented `version`.

### Error responses

| Status | Meaning |
| --- | --- |
| `400 Bad Request` | A required value or version is absent or invalid, a value is too long, the status is unsupported, or a date is malformed. |
| `404 Not Found` | No case exists for the supplied ID. |
| `409 Conflict` | The case number belongs to another case, or the submitted version is stale. |

Archive/restore, file upload, and authorization are separate capabilities and are not part of this endpoint.

## Archive and restore case

### Endpoints

```text
POST /api/cases/{id}/archive
POST /api/cases/{id}/restore
```

Both operations accept the current optimistic-locking version:

```json
{
  "version": 1
}
```

On success, the endpoint returns `200 OK` with the complete Case Detail response. A state change increments `version`. Repeating the operation when the case is already in the requested state is idempotent and does not increment the version.

| Status | Meaning |
| --- | --- |
| `400 Bad Request` | Version is missing or invalid. |
| `404 Not Found` | No case exists for the supplied ID. |
| `409 Conflict` | The submitted version is stale. |

Archiving is reversible and is not physical deletion. The default Case List returns active cases; archived cases are discoverable with `archiveState=ARCHIVED`.

## Upload case document

### `POST /api/cases/{caseId}/documents`

Uploads one document against an existing case. The binary content is stored
outside MySQL; the API response exposes document metadata but never exposes the
storage key.

### Request

Content type:

```text
multipart/form-data
```

The multipart request must contain one part named `file`.

Supported formats and media types:

| Format | File extension | Media type |
| --- | --- | --- |
| PDF | `.pdf` | `application/pdf` |
| Word 97–2003 | `.doc` | `application/msword` |
| Word Open XML | `.docx` | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` |

The maximum file size is 5 MB by default. The backend validates the file name,
declared media type, extension, and content signature. Macro-enabled DOCX
content is not accepted.

### Successful response

Status: `201 Created`

```json
{
  "id": 1,
  "caseId": 10,
  "originalFileName": "证据材料.pdf",
  "documentSource": "UPLOADED",
  "fileFormat": "PDF",
  "contentType": "application/pdf",
  "fileSize": 1024,
  "createdAt": "2026-07-28T10:00:00",
  "updatedAt": "2026-07-28T10:00:00"
}
```

### Error responses

| Status | Meaning |
| --- | --- |
| `400 Bad Request` | The file is absent, empty, or has an unsafe file name. |
| `404 Not Found` | No case exists for the supplied Case ID. |
| `413 Payload Too Large` | The file exceeds the configured 5 MB default limit. |
| `415 Unsupported Media Type` | The extension, media type, or content does not represent a supported PDF, DOC, or safe DOCX file. |
| `500 Internal Server Error` | Storage, content reading, or metadata persistence fails. |

Authentication and authorization are deferred to the security phase. Image
upload, document list/download, and template-generated documents are separate
capabilities.

## List case documents

### `GET /api/cases/{caseId}/documents`

Returns metadata for documents belonging to one case, ordered by creation time
descending and then document ID descending. Storage keys are never exposed.

### Successful response

Status: `200 OK`

```json
{
  "data": [
    {
      "id": 1,
      "caseId": 10,
      "originalFileName": "证据材料.pdf",
      "documentSource": "UPLOADED",
      "fileFormat": "PDF",
      "contentType": "application/pdf",
      "fileSize": 1024,
      "createdAt": "2026-07-28T10:00:00",
      "updatedAt": "2026-07-28T10:00:00"
    }
  ]
}
```

An existing case without documents returns `200 OK` with an empty `data`
array. A missing case returns `404 Not Found`.

## Download case document

### `GET /api/cases/{caseId}/documents/{documentId}/content`

Streams the binary content of a document that belongs to the case identified in
the URL.

### Successful response

Status: `200 OK`

The response includes:

- the stored validated media type in `Content-Type`;
- the stored byte count in `Content-Length`;
- a UTF-8-safe attachment filename in `Content-Disposition`.

The response body contains the original stored bytes.

### Error responses

| Status | Meaning |
| --- | --- |
| `404 Not Found` | The document does not exist or does not belong to the supplied Case ID. |
| `500 Internal Server Error` | Metadata exists but the storage provider cannot open the binary content. |

Range requests, inline preview, caching policy, and download authorization are
deferred. Documents remain readable for archived cases until future
authorization or retention policies define a different rule.
