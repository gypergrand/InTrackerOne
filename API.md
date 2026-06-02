# InTrackerOne — REST API v1

## Base URL

All endpoints are under `/api/v1/`.

## Authentication

All endpoints require `Authorization: Bearer <token>` header.
Tokens use `itr_` prefix. Authentication handled by `TokenAuthenticationFilter` which extracts token identifier, looks up token, and verifies SHA-256 hash.
`ApiExceptionHandler` maps exceptions: `ResourceNotFoundException` → 404, `UnauthorizedException` → 403, `SecurityException` → 403, `IllegalArgumentException` → 400.

### Tokens

```
POST   /api/v1/tokens                  # Create token (returns token value once)
GET    /api/v1/tokens                  # List own tokens (no token value, only metadata)
DELETE /api/v1/tokens/{id}             # Revoke token
```

### Companies

```
GET    /api/v1/companies               # List (ADMIN/MODERATOR: all; USER: own)
POST   /api/v1/companies               # Create
GET    /api/v1/companies/{id}          # Single
PUT    /api/v1/companies/{id}          # Update
DELETE /api/v1/companies/{id}          # Delete (cascades projects, jobs, recruiters)
```

### Projects

```
GET    /api/v1/projects?companyId=     # List, filterable by company
POST   /api/v1/projects                # Create (owner = current user)
GET    /api/v1/projects/{id}           # Single
PUT    /api/v1/projects/{id}           # Update
DELETE /api/v1/projects/{id}           # Delete
```

### Recruiters

```
GET    /api/v1/recruiters?companyId=   # List, filterable by company
POST   /api/v1/recruiters              # Create (owner = current user)
GET    /api/v1/recruiters/{id}         # Single
PUT    /api/v1/recruiters/{id}         # Update
DELETE /api/v1/recruiters/{id}         # Delete
```

### Jobs

```
GET    /api/v1/jobs?status=&company=&project=  # List, filterable (uses JOIN FETCH for N+1 fix)
POST   /api/v1/jobs                            # Create
GET    /api/v1/jobs/{id}                       # Single (includes status history, events, compensation)
PUT    /api/v1/jobs/{id}                       # Update
DELETE /api/v1/jobs/{id}                       # Delete
PATCH  /api/v1/jobs/{id}/status                # Change status
       # Body: { "statusId": 5, "notes": "...", "decidedBy": "COMPANY" }
       # Validation: statusId required; decidedBy max 50 chars; notes max 500 chars
```

### Interview Events

```
GET    /api/v1/jobs/{jobId}/events     # List events for a job
POST   /api/v1/jobs/{jobId}/events     # Create event
GET    /api/v1/events/{id}             # Single
PUT    /api/v1/events/{id}             # Update
DELETE /api/v1/events/{id}             # Delete
```

### Compensation

```
GET    /api/v1/jobs/{jobId}/compensation     # List compensation entries
POST   /api/v1/jobs/{jobId}/compensation     # Create entry
GET    /api/v1/compensation/{id}             # Single
PUT    /api/v1/compensation/{id}             # Update
DELETE /api/v1/compensation/{id}             # Delete
```

### Status History

```
GET    /api/v1/jobs/{jobId}/history    # StatusChange timeline for a job
```

### Pipeline Statuses (per-user)

```
GET    /api/v1/statuses                # List pipeline statuses (current user)
POST   /api/v1/statuses                # Create custom status
PUT    /api/v1/statuses/{id}           # Update (name, color, sortOrder)
DELETE /api/v1/statuses/{id}           # Delete (fails if status is in use)
PUT    /api/v1/statuses/{id}/move      # Reorder
       # Body: { "newOrder": 3 }
```

### Users (ADMIN only)

```
GET    /api/v1/users                   # List all users
POST   /api/v1/users                   # Create user (password required)
GET    /api/v1/users/{id}              # Single user
PUT    /api/v1/users/{id}              # Update (password optional)
DELETE /api/v1/users/{id}              # Delete (fails if target is "admin" user)
```

### Dashboard

```
GET    /api/v1/dashboard/summary       # Stats: total jobs by status, by company (aggregate query)
GET    /api/v1/dashboard/recent        # Recent status changes (last 20)
```

## UI-Only Features (no REST endpoints)

The following features are available exclusively through the Vaadin UI (no REST API):

| Feature | View | Description |
|---------|------|-------------|
| Default Statuses | `/default-statuses` | ADMIN manages 14 default pipeline status templates |
| Default Industries | `/default-industries` | ADMIN manages shared industry list |
| Change Password | Header button | Any authenticated user changes own password |
| Pipeline Kanban | `/pipeline` | Drag-and-drop board with ComboBox status picker |

## Error Responses

### 404 — Resource Not Found
```json
{
  "message": "Resource not found: JobApplication with id 42",
  "status": 404,
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### 403 — Access Denied
```json
{
  "message": "Access denied",
  "status": 403,
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### 400 — Validation Error
```json
{
  "message": "Validation failed: statusId must not be null",
  "status": 400,
  "timestamp": "2025-01-15T10:30:00Z"
}
```
