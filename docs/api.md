# Entry API Documentation

This document describes the RESTful API for managing blog entries.

## Base URL

The API supports both tenant-specific and default endpoints:

- Default: `/entries` (uses default tenant ID: `_`)
- Tenant-specific: `/tenants/{tenantId}/entries`

## Authentication

The API uses Spring Security with Basic Authentication. Some endpoints require authentication and specific authorities.

### Authentication Methods

1. **Basic Authentication**: Use HTTP Basic Authentication with username and password
2. **User Types**:
   - Default users with roles (configured via Spring Security properties)
   - Tenant-specific users (configured via TenantUserProps)

### Required Authorities

| Operation                | Authority Required |
|--------------------------|--------------------|
| Create Entry (POST)      | `entry:edit`       |
| Update Entry (PUT/PATCH) | `entry:edit`       |
| Delete Entry (DELETE)    | `entry:delete`     |
| Import Entries           | `entry:import`     |
| Generate Summary (POST)  | `entry:edit`       |
| S3 Presign URL (POST)    | `entry:edit`       |

For tenant-specific endpoints, authorities are checked against the tenant context.

### Authentication Example

```bash
# Basic Authentication
curl -u username:password -X POST http://localhost:8080/entries \
  -H "Content-Type: text/markdown" \
  -d @entry.md
```

## Data Models

### Entry

Represents a blog entry with content and metadata.

```json
{
  "entryId": 12345,
  "tenantId": "_",
  "frontMatter": {
    "title": "Sample Blog Post",
    "summary": "A brief description of the post",
    "categories": [
      {
        "name": "Programming"
      },
      {
        "name": "Java"
      }
    ],
    "tags": [
      {
        "name": "Spring Boot",
        "version": "3.0"
      },
      {
        "name": "REST API"
      }
    ]
  },
  "content": "The full markdown content of the blog post...",
  "created": {
    "name": "john.doe",
    "date": "2024-01-01T10:00:00Z"
  },
  "updated": {
    "name": "jane.doe",
    "date": "2024-01-15T14:30:00Z"
  }
}
```

### SearchCriteria

Used for filtering entries in search operations.

| Field      | Type          | Description              |
|------------|---------------|--------------------------|
| query      | string        | Search query text        |
| categories | array[string] | Filter by category names |
| tag        | string        | Filter by tag name       |

### CursorPageRequest

Used for cursor-based pagination.

| Field     | Type                       | Description               |
|-----------|----------------------------|---------------------------|
| cursor    | string (ISO-8601 datetime) | Cursor position (Instant) |
| size      | integer                    | Page size (default: 20)   |
| direction | string                     | NEXT or PREVIOUS          |

### CursorPage Response

```json
{
  "content": [],
  "size": 20,
  "hasPrevious": false,
  "hasNext": false,
  "nextCursor": "2024-01-15T14:30:00Z",
  "previousCursor": null
}
```

Note: When using cursor-based pagination, the `nextCursor` and `previousCursor` fields are only present when there are more pages available.

## Endpoints

### 1. Get Entries (Paginated)

Retrieve a paginated list of entries ordered by update date.

**Request:**

```
GET /entries
GET /tenants/{tenantId}/entries
```

**Query Parameters:**

- `query` (optional): Search query text
- `categories` (optional): Comma-separated category names
- `tag` (optional): Tag name to filter by
- `cursor` (optional): Cursor for pagination
- `size` (optional): Page size (default: 20)
- `direction` (optional): NEXT or PREVIOUS

**Response:**

- Status: 200 OK
- Body: CursorPage<Entry>

Note: When retrieving a list of entries, the `content` field is returned as an empty string to reduce payload size. To get the full content, fetch individual entries.

**Example:**

```bash
curl "http://localhost:8080/entries?tag=Spring&size=10"
```

### 2. Get Entries by IDs

Retrieve multiple entries by their IDs.

**Request:**

```
GET /entries?entryIds=1,2,3
GET /tenants/{tenantId}/entries?entryIds=1,2,3
```

**Query Parameters:**

- `entryIds` (required): Comma-separated list of entry IDs

**Response:**

- Status: 200 OK
- Body: List<Entry>

### 3. Get Single Entry

Retrieve a single entry by ID.

**Request:**

```
GET /entries/{entryId}
GET /tenants/{tenantId}/entries/{entryId}
```

**Path Parameters:**

- `entryId`: The ID of the entry (numeric)

**Response:**

- Status: 200 OK
- Body: Entry
- Headers:
    - Cache-Control: max-age=3600
    - Last-Modified: {update timestamp}

**Error Response:**

- Status: 404 Not Found
- Body: Problem Detail (RFC 9457)

```json
{
  "detail": "Entry not found: 00001",
  "instance": "/entries/1",
  "status": 404,
  "title": "Not Found",
  "type": null
}
```

### 4. Get Entry as Markdown

Retrieve an entry in Markdown format with YAML front matter.

**Request:**

```
GET /entries/{entryId}.md
GET /tenants/{tenantId}/entries/{entryId}.md
```

**Response:**

- Status: 200 OK
- Content-Type: text/markdown
- Body: Markdown text with YAML front matter

**Example Response:**

```markdown
---
title: Sample Blog Post
summary: A brief description
tags: ["Spring Boot", "REST API"]
categories: ["Programming", "Java"]
date: 2024-01-01T10:00:00Z
updated: 2024-01-15T14:30:00Z
---

The full markdown content of the blog post...
```

### 5. Create Entry from Markdown

Create a new entry from Markdown content. **Requires authentication.**

**Request:**

```
POST /entries
POST /tenants/{tenantId}/entries
Content-Type: text/markdown
```

**Request Body:**
Markdown content with YAML front matter.

**Response:**

- Status: 201 Created
- Location: /entries/{entryId}
- Body: Entry

### 6. Update Entry from Markdown

Update an existing entry. **Requires authentication.**

**Request:**

```
PUT /entries/{entryId}
PUT /tenants/{tenantId}/entries/{entryId}
Content-Type: text/markdown
```

**Request Body:**
Markdown content with YAML front matter.

**Response:**

- Status: 200 OK
- Body: Entry

### 7. Update Entry Summary

Update only the summary of an entry. **Requires authentication.**

**Request:**

```
PATCH /entries/{entryId}/summary
PATCH /tenants/{tenantId}/entries/{entryId}/summary
Content-Type: application/json
```

**Request Body:**

```json
{
  "summary": "New summary text"
}
```

**Response:**

- Status: 200 OK
- Body: Entry (with updated summary)

**Error Response:**

- Status: 404 Not Found
- Body: Problem Detail (RFC 9457)

```json
{
  "detail": "Entry not found: 00001",
  "instance": "/entries/1",
  "status": 404,
  "title": "Not Found",
  "type": null
}
```

### 8. Delete Entry

Delete an entry. **Requires authentication.**

**Request:**

```
DELETE /entries/{entryId}
DELETE /tenants/{tenantId}/entries/{entryId}
```

**Response:**

- Status: 204 No Content

### 9. Get Categories

Retrieve all categories as a nested list structure.

**Request:**

```
GET /categories
GET /tenants/{tenantId}/categories
```

**Response:**

- Status: 200 OK
- Body: List<List<Category>>

**Example Response:**

```json
[
  [
    {
      "name": "Programming"
    },
    {
      "name": "Java"
    }
  ],
  [
    {
      "name": "Programming"
    },
    {
      "name": "Python"
    }
  ],
  [
    {
      "name": "DevOps"
    },
    {
      "name": "Docker"
    }
  ]
]
```

### 10. Get Tags with Count

Retrieve all tags with their usage count.

**Request:**

```
GET /tags
GET /tenants/{tenantId}/tags
```

**Response:**

- Status: 200 OK
- Body: List<TagAndCount>

**Example Response:**

```json
[
  {
    "name": "Spring Boot",
    "version": "3.0",
    "count": 25
  },
  {
    "name": "Docker",
    "count": 15
  },
  {
    "name": "Kubernetes",
    "count": 10
  }
]
```

### 11. Get Entry Template

Get a Markdown template for creating new entries.

**Request:**

```
GET /entries/template.md
```

**Response:**

- Status: 200 OK
- Content-Type: text/markdown
- Body: Markdown template with example content

### 12. Generate Summary

Generate a summary for the given content using AI. **Requires authentication.**

**Request:**

```
POST /tenants/{tenantId}/summary
Content-Type: application/json
```

**Request Body:**

```json
{
  "content": "The blog content to summarize..."
}
```

**Response:**

- Status: 200 OK
- Body:

```json
{
  "summary": "Generated summary text..."
}
```

**Error Responses:**

- Status: 400 Bad Request (when content is empty or blank)

```json
{
  "detail": "Content must not be empty",
  "status": 400,
  "title": "Bad Request"
}
```

- Status: 401 Unauthorized (when not authenticated)
- Status: 403 Forbidden (when user lacks `entry:edit` authority for the tenant)

**Example:**

```bash
curl -u editor:password -X POST http://localhost:8080/tenants/_/summary \
  -H "Content-Type: application/json" \
  -d '{"content": "Sample blog content about Spring Boot"}'
```

### 13. Get S3 Presigned URL

Generate a presigned URL for uploading files to S3. The uploaded files will be publicly accessible. **Requires authentication.**

**Request:**

```
POST /tenants/{tenantId}/s3/presign
Content-Type: application/json
```

**Request Body:**

```json
{
  "fileName": "image.png"
}
```

**Response:**

- Status: 200 OK
- Body:

```json
{
  "url": "https://s3.example.com/{tenantId}/image.png?X-Amz-Algorithm=..."
}
```

The returned URL can be used to upload a file via HTTP PUT request. After upload, the file is publicly accessible at the URL without query parameters.

**Error Responses:**

- Status: 401 Unauthorized (when not authenticated)
- Status: 403 Forbidden (when user lacks `entry:edit` authority for the tenant)

**Example:**

```bash
# Step 1: Get presigned URL
curl -u editor:password -X POST http://localhost:8080/tenants/_/s3/presign \
  -H "Content-Type: application/json" \
  -d '{"fileName": "my-image.png"}'

# Step 2: Upload file using presigned URL
curl -X PUT "${PRESIGNED_URL}" \
  -H "Content-Type: image/png" \
  --data-binary @my-image.png

# Step 3: Access file publicly (URL without query parameters)
curl "https://s3.example.com/_/my-image.png"
```

## Error Handling

The API returns standard HTTP status codes and uses RFC 9457 Problem Details for error responses:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Entry not found: 00001|tenant1"
}
```

## Caching

- Entry responses include cache headers
- Supports conditional requests using `If-Modified-Since` header
- Cache-Control: max-age=3600 for entry responses

## Notes

- Entry IDs are formatted as 5-digit strings with leading zeros (e.g., "00001")
- Tenant ID defaults to `_` if not specified
- The default tenant ID `_` is used when no tenant is specified in the URL
- All timestamps use ISO-8601 format in UTC
- Markdown content supports YAML front matter for metadata