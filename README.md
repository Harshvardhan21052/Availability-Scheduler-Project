# Availability Scheduler

Coordinating a meeting time across a group almost always ends up as a long WhatsApp thread — everyone throws out their availability in different formats, someone misses a message, and the back-and-forth takes longer than the meeting itself. Availability Scheduler eliminates that entirely. Each person marks the slots they are busy on a given day, and the app automatically computes every window where the entire group is simultaneously free — no thread, no guessing, no back-and-forth.
Built with a **Spring Boot REST API** backend and a native **Android** client following industry-standard architecture patterns.

---
## Screenshots

| Login | Home Screen | My Busy Slots | Add Slot |
|----------|----------|----------|----------|
| <img src="https://github.com/user-attachments/assets/edeba8be-9106-4734-9fcf-1b0b0a2d6ed6" width="200"/> | <img src="https://github.com/user-attachments/assets/4d89f1d3-e808-460e-8e56-5c20534d589e" width="200"/> | <img src="https://github.com/user-attachments/assets/ca99a106-4753-4e2c-8b03-7a483788587e" width="200"/> | <img src="https://github.com/user-attachments/assets/7316c0f0-c944-4b82-9b3f-8540c5bb0c6d" width="200"/> |

| Slot Added | Find Availability | Availability |
|----------|----------|----------|
| <img src="https://github.com/user-attachments/assets/cf1de0b1-daa7-437e-bb9b-bd92b5496222" width="200"/> | <img src="https://github.com/user-attachments/assets/5c237f26-7363-4f2b-96fe-160ff5338a14" width="200"/> | <img src="https://github.com/user-attachments/assets/56d5ff1d-e77a-48b0-8b5c-725c5d7ac589" width="200"/> |

---

## Tech Stack

**Backend**
| Technology | Purpose |
|---|---|
| Java 17 | Core language |
| Spring Boot 3.2 | Application framework |
| Spring Security | Authentication and endpoint protection |
| JSON Web Tokens (JWT) | Stateless auth via JJWT library |
| PostgreSQL | Primary relational database |
| Spring Data JPA + Hibernate | ORM and repository layer |
| Maven | Build and dependency management |
| Lombok | Boilerplate reduction |
| Swagger / OpenAPI 3 | Auto-generated interactive API documentation |

**Android**
| Technology | Purpose |
|---|---|
| Kotlin | Core language |
| MVVM | Presentation architecture pattern |
| Hilt | Compile-time dependency injection |
| Retrofit 2 + OkHttp | Type-safe HTTP client |
| Navigation Component | Single-activity navigation with back stack management |
| ViewBinding | Null-safe view access |
| LiveData + Coroutines | Reactive UI state with structured concurrency |
| SharedPreferences | Local JWT and session persistence |

---

## Architecture

### Backend — Layered Architecture

The backend enforces a strict separation of concerns across four layers. Each layer has a single responsibility and only communicates with the layer directly below it.

```
HTTP Request
     │
     ▼
┌─────────────┐
│  Controller │  Handles HTTP only — parses input, returns responses
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Service   │  All business logic — validation, algorithms, rules
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Repository  │  JPA interfaces — zero SQL written manually
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  PostgreSQL │  Persistent storage with indexes and FK constraints
└─────────────┘
```

Controllers are kept intentionally thin — they validate HTTP input via `@Valid`, delegate all logic to the service layer, and return DTOs. No business logic lives outside the service layer.

### Android — MVVM + Repository Pattern

```
Fragment (View)
     │  observes LiveData
     ▼
ViewModel
     │  calls suspend functions
     ▼
Repository
     │  wraps results in Resource<T>
     ▼
Retrofit ApiService
     │  HTTP over OkHttp
     ▼
Spring Boot Backend
```

Each screen has exactly one Fragment and one ViewModel. ViewModels survive configuration changes and hold no Android framework references. The `Resource<T>` sealed class (`Loading`, `Success<T>`, `Error`) is the single mechanism driving all UI state transitions — loading spinners, data display, and error Snackbars all stem from one `when` expression observing the same LiveData.

---

## Features

### JWT Authentication
Users register and log in via `/auth/signup` and `/auth/login`. Passwords are hashed using BCrypt before storage — plaintext is never persisted. On successful login the server returns a signed HS256 JWT containing the username as the subject claim. On Android, the token is stored in SharedPreferences and automatically attached to every outgoing request by a custom OkHttp `AuthInterceptor`, so no screen beyond login needs to think about authentication.

### Busy Slot Management
Users mark time windows when they are unavailable. The backend enforces two hard rules on every slot: the date must be today or in the future, and the duration must be at least 15 minutes. Ownership is verified on every update and delete — attempting to modify another user's slot returns a `403 Forbidden`. The Android UI uses system `DatePickerDialog` and `TimePickerDialog` for input, keeping the experience native and requiring no manual text formatting from the user.

### Common Availability Algorithm
The core feature. Given a list of usernames and a date, the algorithm computes every time window during that day where all listed users are simultaneously free.

**How it works:**
1. Each username is resolved to a user ID — the request fails immediately if any username does not exist
2. Each user's busy slots for the given date are fetched in a **separate query per user** — this avoids a Hibernate deduplication issue that occurs when using `IN` clause joins on lazily-loaded associations
3. All intervals are collected into a single list and sorted by start time
4. A linear scan merges overlapping and adjacent intervals into a minimal set of non-overlapping busy blocks
5. The merged busy list is inverted against the full day (`00:00–23:59`) to produce free windows
6. Any free window shorter than 15 minutes is filtered out

**Example:**
```
User A busy:  14:00–15:00,  17:00–18:15
User B busy:  00:00–17:30
User C busy:  14:01–18:00,  21:00–23:00

Merged busy:  00:00–18:15,  21:00–23:00
Common free:  18:15–21:00,  23:00–23:59
```

The merge step runs in O(n log n) due to sorting, where n is the total number of busy slots across all users. The inversion and output steps are O(n).

### User Search
A debounced search endpoint returns users whose username partially matches the query, case-insensitively. On Android, search triggers 400ms after the user stops typing to avoid flooding the backend with requests on every keystroke.

---

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/auth/signup` | No | Register a new user, returns JWT |
| POST | `/auth/login` | No | Authenticate, returns JWT |
| POST | `/availability` | Yes | Mark a busy time slot |
| GET | `/availability/my` | Yes | Fetch all your busy slots |
| PUT | `/availability/{id}` | Yes | Update an existing busy slot |
| DELETE | `/availability/{id}` | Yes | Delete a busy slot |
| GET | `/availability/common?usernames=a,b&date=yyyy-MM-dd` | Yes | Find common free slots across users |
| GET | `/users/search?query=xyz` | Yes | Search users by partial username |

All protected endpoints return `401 Unauthorized` when the token is missing or invalid, and `403 Forbidden` when the authenticated user attempts to modify a resource they do not own.

### Error Response Format
Every error — validation failures, not found, conflicts, server errors — returns a consistent JSON shape:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "End time must be after start time",
  "path": "/availability",
  "timestamp": "2026-04-01T10:00:00"
}
```
Validation errors additionally include a `fieldErrors` map so clients can highlight the exact field that failed.

---

## Database Schema

```sql
users
├── id          BIGSERIAL PRIMARY KEY
├── username    VARCHAR(50) UNIQUE NOT NULL
└── password    VARCHAR      NOT NULL        -- BCrypt hash

busy_slots
├── id          BIGSERIAL PRIMARY KEY
├── user_id     BIGINT NOT NULL REFERENCES users(id)
├── date        DATE NOT NULL
├── start_time  TIME NOT NULL
└── end_time    TIME NOT NULL
```

**Indexes:**
- `idx_users_username` on `users(username)` — fast lookup during login and search
- `idx_busy_slots_user_id` on `busy_slots(user_id)` — fast fetch of all slots per user
- `idx_busy_slots_date` on `busy_slots(date)` — fast filtering by date
- `idx_busy_slots_user_date` on `busy_slots(user_id, date)` — composite index for the common availability query (one query per user per date)

---

## Project Structure

```
availability-scheduler/                         # Spring Boot backend
├── controller/
│   ├── AuthController.java                     # POST /auth/signup, /auth/login
│   ├── AvailabilityController.java             # CRUD + GET /availability/common
│   └── UserController.java                     # GET /users/search
├── service/
│   ├── AuthService.java                        # Signup, login, token generation
│   ├── AvailabilityService.java                # Slot CRUD + interval merge algorithm
│   └── UserService.java                        # User search
├── repository/
│   ├── UserRepository.java
│   └── BusySlotRepository.java
├── entity/
│   ├── User.java
│   └── BusySlot.java
├── dto/
│   ├── request/                                # SignupRequest, LoginRequest, BusySlotRequest
│   └── response/                               # AuthResponse, BusySlotResponse, TimeSlotResponse, ApiErrorResponse
├── security/
│   ├── JwtTokenProvider.java                   # Token generation and validation
│   ├── JwtAuthenticationFilter.java            # OncePerRequestFilter for JWT
│   ├── JwtAuthenticationEntryPoint.java        # JSON 401 responses
│   └── CustomUserDetailsService.java           # Loads user from DB for Spring Security
├── config/
│   ├── SecurityConfig.java                     # Filter chain, public routes, session policy
│   └── OpenApiConfig.java                      # Swagger with Bearer auth
└── exception/
    ├── GlobalExceptionHandler.java             # @RestControllerAdvice
    └── [BadRequestException, ConflictException, ForbiddenException, ResourceNotFoundException]

availability-scheduler-android/                 # Android frontend
├── data/
│   ├── model/Models.kt                         # All request and response DTOs
│   ├── remote/
│   │   ├── ApiService.kt                       # Retrofit interface — all endpoints
│   │   └── AuthInterceptor.kt                  # Attaches Bearer token to every request
│   ├── local/TokenManager.kt                   # JWT stored in SharedPreferences
│   └── repository/
│       ├── AuthRepository.kt                   # Signup, login, logout, session state
│       └── AvailabilityRepository.kt           # Slots CRUD + common availability + search
├── di/NetworkModule.kt                         # Hilt: OkHttp, Retrofit, ApiService providers
├── ui/
│   ├── MainActivity.kt                         # Single activity — NavHostFragment
│   ├── auth/                                   # LoginFragment, SignupFragment + ViewModels
│   ├── home/                                   # HomeFragment — dashboard with three nav tiles
│   ├── availability/                           # MyAvailabilityFragment, BusySlotAdapter + ViewModel
│   ├── common/                                 # CommonAvailabilityFragment + ViewModel
│   └── search/                                 # SearchUsersFragment, UserAdapter + ViewModel
└── util/
    ├── Resource.kt                             # Sealed class: Loading | Success<T> | Error
    └── Extensions.kt                           # safeApiCall, show/hide, showSnackbar
```

---

## Testing

The backend includes both unit and integration tests.

| Test Class | Type | What It Covers |
|---|---|---|
| `AvailabilityServiceTest` | Unit (Mockito) | Interval merge algorithm, slot validation, bug regression for multi-user fetch |
| `JwtTokenProviderTest` | Unit | Token generation, expiry, signature tampering |
| `AuthControllerIntegrationTest` | Integration (H2) | Signup, login, duplicate username, field validation |
| `AvailabilityControllerIntegrationTest` | Integration (H2) | Full CRUD, ownership enforcement, common availability end-to-end |
| `UserControllerIntegrationTest` | Integration (H2) | Search, case insensitivity, password not leaked in response |

Integration tests run against an **H2 in-memory database** with a separate `application-test.properties` profile — no PostgreSQL installation required to run the test suite.

A dedicated regression test (`findCommon_exactBugScenario_carolSecondSlotIncluded`) explicitly verifies the interval merge algorithm against the exact multi-user, multi-slot scenario that exposed the original Hibernate lazy-join deduplication bug.
