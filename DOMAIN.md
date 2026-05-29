# InTrackerOne — Domain Model

## Entity Relationship Diagram

```
User (1) ── (N) ApiToken
  │
  ├── (1) ── (N) Company ── (1) ── (N) Project ── (N) ── (M) Recruiter
  │                                        │
  │                                        └── (1) ── (N) JobApplication (1) ── (N) InterviewEvent
  │                                                                 │
  │                                                                 ├── (1) ── (N) StatusChange
  │                                                                 │
  │                                                                 └── (1) ── (N) CompensationEntry
  │
  └── (1) ── (N) PipelineStatus
```

## Tables

### users

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| username | VARCHAR(100) | NOT NULL, UNIQUE |
| password | VARCHAR(255) | NOT NULL (BCrypt hash) |
| email | VARCHAR(255) | |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE |
| role | VARCHAR(20) | NOT NULL (ADMIN / MODERATOR / USER) |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### api_tokens

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(100) | NOT NULL |
| token_hash | VARCHAR(255) | NOT NULL |
| user_id | BIGINT | FK → users, NOT NULL |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE |
| last_used_at | TIMESTAMP | |
| created_at | TIMESTAMP | NOT NULL |

### pipeline_statuses

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(100) | NOT NULL |
| sort_order | INT | NOT NULL |
| color | VARCHAR(7) | NOT NULL (hex, e.g. #FF0000) |
| type | VARCHAR(20) | NOT NULL (ACTIVE / TERMINAL) |
| user_id | BIGINT | FK → users, NOT NULL |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

UNIQUE(user_id, name)

### companies

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(255) | NOT NULL |
| website | VARCHAR(255) | |
| notes | TEXT | |
| owner_id | BIGINT | FK → users, NOT NULL |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### recruiters

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(255) | NOT NULL |
| email | VARCHAR(255) | |
| phone | VARCHAR(50) | |
| notes | TEXT | |
| company_id | BIGINT | FK → companies, NOT NULL |
| owner_id | BIGINT | FK → users, NOT NULL |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### projects

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(255) | NOT NULL |
| description | TEXT | |
| company_id | BIGINT | FK → companies, NOT NULL |
| owner_id | BIGINT | FK → users, NOT NULL |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### recruiter_projects (join table)

| Column | Type | Constraints |
|---|---|---|
| recruiter_id | BIGINT | FK → recruiters, PK |
| project_id | BIGINT | FK → projects, PK |

### job_applications

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| position | VARCHAR(255) | NOT NULL |
| job_description | TEXT | |
| job_url | VARCHAR(500) | |
| location | VARCHAR(255) | |
| salary_range | VARCHAR(100) | (from posting, free text) |
| job_type | VARCHAR(20) | NOT NULL (FULL_TIME / PART_TIME / CONTRACT / INTERNSHIP) |
| source | VARCHAR(30) | NOT NULL (LINKEDIN / COMPANY_PORTAL / REFERRAL / RECRUITER / GLASSDOOR / LEVELS_FYI / TELEGRAM_GROUP / OTHER) |
| source_meta | VARCHAR(500) | (link, phone, notes about source) |
| notes | TEXT | (general research notes) |
| project_id | BIGINT | FK → projects, NOT NULL |
| recruiter_id | BIGINT | FK → recruiters, NULLABLE |
| current_status_id | BIGINT | FK → pipeline_statuses, NOT NULL |
| owner_id | BIGINT | FK → users, NOT NULL |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### status_changes

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| job_application_id | BIGINT | FK → job_applications, NOT NULL |
| from_status_id | BIGINT | FK → pipeline_statuses, NULLABLE (null = initial) |
| to_status_id | BIGINT | FK → pipeline_statuses, NOT NULL |
| changed_at | TIMESTAMP | NOT NULL |
| decided_by | VARCHAR(50) | (free text: YOU / COMPANY / MUTUAL or custom) |
| notes | TEXT | |
| created_at | TIMESTAMP | NOT NULL |

### interview_events

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| job_application_id | BIGINT | FK → job_applications, NOT NULL |
| title | VARCHAR(255) | NOT NULL (e.g. "Round 1: Technical") |
| event_date | TIMESTAMP | NOT NULL |
| duration_minutes | INT | |
| format | VARCHAR(20) | NOT NULL (PHONE / VIDEO / ONSITE / TAKE_HOME) |
| interviewer_name | VARCHAR(255) | |
| notes | TEXT | |
| created_at | TIMESTAMP | NOT NULL |

### compensation_entries

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| job_application_id | BIGINT | FK → job_applications, NOT NULL |
| rate_type | VARCHAR(20) | NOT NULL (ANNUAL_GROSS / MONTHLY / HOURLY) |
| amount | DECIMAL(12,2) | NOT NULL |
| currency | VARCHAR(5) | NOT NULL (EUR / USD / GBP / AUD / PLN) |
| category | VARCHAR(20) | NOT NULL (POSTED / MARKET_RESEARCH / OFFER / TARGET) |
| notes | VARCHAR(500) | |
| created_at | TIMESTAMP | NOT NULL |

## Enums

| Enum | Values |
|---|---|
| Role | ADMIN, MODERATOR, USER |
| JobType | FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP |
| Source | LINKEDIN, COMPANY_PORTAL, REFERRAL, RECRUITER, GLASSDOOR, LEVELS_FYI, TELEGRAM_GROUP, OTHER |
| RateType | ANNUAL_GROSS, MONTHLY, HOURLY |
| Currency | EUR, USD, GBP, AUD, PLN |
| CompCategory | POSTED, MARKET_RESEARCH, OFFER, TARGET |
| EventFormat | PHONE, VIDEO, ONSITE, TAKE_HOME |
| StatusType | ACTIVE, TERMINAL |

## Default Pipeline (seeded per user on registration)

| # | Active | Color |
|---|---|---|
| 1 | Preparation | #9E9E9E (Grey) |
| 2 | Applied | #2196F3 (Blue) |
| 3 | Phone Screen | #00BCD4 (Cyan) |
| 4 | Technical Interview | #FF9800 (Orange) |
| 5 | On-site / Final | #9C27B0 (Purple) |
| 6 | Offer | #4CAF50 (Green) |
| 7 | Negotiation | #009688 (Teal) |
| **#** | **Terminal** | **Color** |
| 8 | Accepted | #1B5E20 (Dark Green) |
| 9 | Rejected | #F44336 (Red) |
| 10 | Withdrawn | #616161 (Dark Grey) |
| 11 | Ghosted | #795548 (Brown) |
