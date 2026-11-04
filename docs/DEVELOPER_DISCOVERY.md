# BuildSync — Developer Discovery & Search System

## 1. Overview & Architecture

The **Developer Discovery System** (Phase 3, Part 5) provides high-performance, multi-criteria filtering, pagination, and sorting for developer profiles on BuildSync. It enables creators, recruiters, and engineering teams to find prospective teammates by technical skills, experience level, availability, location, and keywords across headlines, biographies, and usernames.

The search architecture adheres to strict security, privacy, and performance standards:
- **Zero Privacy Leakage:** Private email addresses, encrypted passwords, and authentication tokens are strictly excluded from public discovery payloads.
- **Dynamic Database Queries:** Filtering is executed in PostgreSQL using Spring Data JPA `Specification` and Criteria queries with pagination (`Pageable`).
- **N+1 and Duplicate Prevention:** Skill matching uses subqueries to prevent Cartesian product explosion on root developer entities, and page skills are loaded in one batch query.
- **Cyber Ember Red (#05) UI/UX:** Built with Next.js 16 App Router, Tailwind CSS 4, debounced input, responsive skeleton loaders, quick filter chips, and accessible pagination.

```
                      ┌──────────────────────────────────────────────┐
                      │              Next.js Frontend                │
                      │  - Debounced search (350ms)                  │
                      │  - Quick skill chips & multi-select filters  │
                      │  - Skeletons, empty states, pagination       │
                      └──────────────────────┬───────────────────────┘
                                             │ GET /api/v1/profiles?keyword=...
                                             ▼
                      ┌──────────────────────────────────────────────┐
                      │          Spring Boot REST Controller         │
                      │     DeveloperProfileController.search(...)    │
                      └──────────────────────┬───────────────────────┘
                                             │
                                             ▼
                      ┌──────────────────────────────────────────────┐
                      │           DeveloperProfileService            │
                      │  - Query specification builder               │
                      │  - Sort mapping & pagination validation      │
                      │  - Batch skill retrieval (1 SQL query)       │
                      └──────────────────────┬───────────────────────┘
                                             │
                                             ▼
                      ┌──────────────────────────────────────────────┐
                      │             PostgreSQL 17 Database           │
                      │  - B-tree indexes on filtered columns        │
                      │  - Subquery-based skill containment          │
                      └──────────────────────────────────────────────┘
```

---

## 2. REST API Specification

### Endpoint
`GET /api/v1/profiles` (Public / Unauthenticated)

### Query Parameters
| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `keyword` | String | No | None | Case-insensitive search matching name, username, headline, bio, or location. |
| `skill` | String | No | None | Matches developer profiles possessing this technical skill. |
| `experienceLevel` | Enum | No | None | `JUNIOR`, `MID_LEVEL`, `SENIOR`, `LEAD` |
| `availability` | Enum | No | None | `AVAILABLE`, `BUSY`, `MENTORING`, `NOT_AVAILABLE` |
| `location` | String | No | None | Case-insensitive substring match on developer location. |
| `page` | Integer | No | `0` | 0-based page index. Must be `>= 0`. |
| `size` | Integer | No | `12` | Number of items per page. Range: `1` to `50`. |
| `sort` | String | No | `recently_updated` | Supported options: `recently_updated`, `newest`, `name_asc`, `name_desc`. |

### Sample Response (`200 OK`)
```json
{
  "content": [
    {
      "id": "ea75a10e-33d0-4108-ba70-f89ac6bb3ef2",
      "userId": "bc8ba184-a57d-4a00-bda0-161705072a35",
      "username": "abhishek26",
      "fullName": "Abhishek Karn",
      "headline": "Senior Full-Stack Engineer",
      "bio": "Building scalable developer tools and distributed systems.",
      "location": "Dhanbad",
      "avatarUrl": "http://localhost:8080/api/v1/media/avatars/avatar_bc8ba184.png",
      "experienceLevel": "SENIOR",
      "yearsOfExperience": 5,
      "availabilityStatus": "AVAILABLE",
      "skills": [
        {
          "id": "17901132-9510-445d-b879-b796800a51ff",
          "skillId": "11651cf9-a804-4c9e-8b42-eea666782e60",
          "name": "Java",
          "category": "PROGRAMMING_LANGUAGE",
          "proficiencyLevel": "EXPERT",
          "createdAt": "2026-11-01T20:52:52.628377Z",
          "updatedAt": "2026-11-01T20:52:52.781574Z"
        }
      ],
      "createdAt": "2026-11-02T06:06:11.828791Z",
      "updatedAt": "2026-11-02T06:07:01.888151Z"
    }
  ],
  "page": 0,
  "size": 12,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

## 3. Database Indexes (`V6__add_developer_search_indexes.sql`)

To accelerate search performance across large datasets, Flyway migration `V6` adds targeted B-tree indexes to `developer_profiles`:

```sql
CREATE INDEX IF NOT EXISTS idx_dev_profiles_experience_level 
    ON developer_profiles(experience_level);

CREATE INDEX IF NOT EXISTS idx_dev_profiles_availability_status 
    ON developer_profiles(availability_status);

CREATE INDEX IF NOT EXISTS idx_dev_profiles_location 
    ON developer_profiles(location);

CREATE INDEX IF NOT EXISTS idx_dev_profiles_created_at 
    ON developer_profiles(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_dev_profiles_updated_at 
    ON developer_profiles(updated_at DESC);
```

---

## 4. Query Specification & Subquery Strategy

### Cartesian Product Prevention
Directly joining `DeveloperProfile` with `@OneToMany` `skills` in JPA Specifications produces multiple duplicate rows per profile when multiple skills match, invalidating count queries and corrupting pagination limits. BuildSync isolates skill matching into a subquery:

```java
Subquery<UUID> skillSubquery = query.subquery(UUID.class);
Root<DeveloperProfileSkill> skillRoot = skillSubquery.from(DeveloperProfileSkill.class);
Join<DeveloperProfileSkill, Skill> skillJoin = skillRoot.join("skill");

skillSubquery.select(skillRoot.get("profile").get("id"))
    .where(builder.equal(
        builder.lower(skillJoin.get("name")), 
        skillName.toLowerCase()
    ));

predicates.add(root.get("id").in(skillSubquery));
```

### N+1 Free Skill Hydration
Rather than querying skills lazily per card (which causes `N` additional roundtrips), the service gathers all profile IDs from the current page content and runs a single `IN` query:
```java
List<DeveloperProfileSkill> profileSkills = 
    profileSkillRepository.findByProfileIdInWithSkill(profileIds);
Map<UUID, List<ProfileSkillResponse>> skillsByProfile = profileSkills.stream()
    .collect(Collectors.groupingBy(ps -> ps.getProfile().getId(), ...));
```

---

## 5. Frontend Implementation (`/developers`)

The discoverability page in `frontend/src/app/developers/page.tsx` features:
1. **Debounced Search:** 350ms delay eliminates typing jitter and avoids server load spikes.
2. **Fast Popular Skills Pills:** Quick toggle chips for Java, Spring Boot, React, Next.js, TypeScript, PostgreSQL, Python, Docker, Kubernetes, Tailwind CSS, GraphQL, AWS.
3. **Filter Controls:** Experience level select, Availability status select, and Sort Order select.
4. **Active Filters Bar & Reset:** Displays total matched count and one-click "Reset Filters".
5. **Loading Skeletons:** Animated Cyber Ember red pulse placeholders matching the responsive card grid.
6. **Card Showcase:** Avatar with initials fallback, online status badge, role/headline, location, experience summary, bio truncation, skill tags with proficiency tooltip, "View Profile" link, and interactive "Connect" toggle.
7. **Accessible Pagination:** Previous / Next buttons and pagination summary.

---

## 6. Verification & Test Coverage

### Automated Backend Tests
- **DeveloperSearchIntegrationTest (13 test cases):**
  1. `searchDevelopers_NoFilters_ReturnsAllProfiles`
  2. `searchDevelopers_ByFullNameKeyword_ReturnsMatchingProfiles`
  3. `searchDevelopers_ByUsernameKeyword_ReturnsMatchingProfiles`
  4. `searchDevelopers_ByHeadlineOrBioKeyword_ReturnsMatchingProfiles`
  5. `searchDevelopers_BySkillName_ReturnsProfilesWithSkill`
  6. `searchDevelopers_ByExperienceLevel_ReturnsMatchingProfiles`
  7. `searchDevelopers_ByAvailability_ReturnsMatchingProfiles`
  8. `searchDevelopers_ByLocation_ReturnsMatchingProfiles`
  9. `searchDevelopers_CombinedFilters_ReturnsIntersection`
  10. `searchDevelopers_Pagination_ReturnsCorrectPageAndSize`
  11. `searchDevelopers_Sorting_ReturnsOrderedProfiles`
  12. `searchDevelopers_NoResults_ReturnsEmptyPagedResponse`
  13. `searchDevelopers_ExcludesPrivateEmailAndPasswords`
  14. `searchDevelopers_PublicAccess_NoTokenRequired`
  15. `searchDevelopers_InvalidPagination_ReturnsBadRequest`

**Total Backend Suite:** 131 tests passing (`131/131 passed, 0 failures, 0 errors`).

### Frontend Verification
- `npm run lint`: 0 errors, 0 warnings.
- `npm run build --webpack`: Successful production bundle compilation.
- Next.js development server running cleanly on port 3000.

