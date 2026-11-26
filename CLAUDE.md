# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

MDD — an OpenClassrooms full-stack project: a developer social network where users follow topics and read/write posts and comments. The repo holds two independent apps:

- `back/` — Spring Boot 3.5 REST API (Java 21, Maven, Spring Data JPA, MySQL), package `com.openclassrooms.mddapi`
- `front/` — Angular 20 client (Karma/Jasmine tests)

The project is early-stage: only `GET /topic` is implemented end-to-end on the server; `Post`/`Comment` entities and `PostService` are still in progress.

## Commands

### Backend (run from `back/`)

```bash
mvn spring-boot:run                                   # start the API (always use this) → http://localhost:9000
mvn clean package                                     # build jar (runs tests)
mvn test                                              # all tests
mvn test -Dtest=MddApiApplicationTests                # single test class
mvn test -Dtest=MddApiApplicationTests#contextLoads   # single test method
```

Requires a local MySQL with a database `mdd` at `localhost:3306` (credentials `root`/`root`, see `back/src/main/resources/application.properties`). The context-load test also needs this database.

### Frontend (run from `front/`)

Requires Node ≥ 20.19 / 22.12. The system default `node` on this machine is v12 and fails; use an nvm version, e.g. `export PATH="$HOME/.nvm/versions/node/v22.16.0/bin:$PATH"` (or `nvm use 22`) before any npm/ng command.

```bash
npm install
npm run start                                 # start the dev server (always use this) → http://localhost:4200
npm run build                                 # output in dist/
npm test                                      # Karma + Jasmine (Chrome)
npx ng test --include src/app/topic           # run specs for a single folder/file
```

No linter is configured.

## Architecture

### Backend layering

`controller` → `service` (interface `IXxxService` + implementation `XxxService`) → `repository` (Spring Data `JpaRepository`) → `model` (JPA entities).

- Controllers depend on the service **interface**, injected via constructor (no field `@Autowired`).
- Service implementations must be annotated `@Service` to be injected (note: `PostService` currently lacks it).
- Entities are exposed directly from controllers for now (no DTO layer yet).
- Entities use explicit table names (`topics`, `posts`, …) and explicit PK column names (`topic_id`, `post_id`); relations use `@ManyToOne` + `@JoinColumn` on the FK column name.

### Database

`spring.jpa.hibernate.ddl-auto=create` — Hibernate **drops and recreates the schema on every startup**, so all data is lost on restart. There are no migration or seed scripts yet.

### Frontend

Currently NgModule-based (not standalone) despite Angular 20: components are declared in `AppModule` with `standalone: false`. Migration to standalone is planned (see below). There is no routing, HTTP client, or API service wired up yet; the backend runs on port 9000 while the dev server runs on 4200 (no proxy config exists).

## Target conventions (decided, not all implemented yet)

New code should move toward these; existing code is migrated when touched:

- **Back**: DTOs (Java records) + hand-written `@Component` mapper classes (no MapStruct or other mapping library) — never expose JPA entities; Lombok (no `@Data` on entities); Spring Security with stateless JWT, all routes under `/api` and authenticated except `/api/auth/**`; Bean Validation (`spring-boot-starter-validation`) for request DTOs.
- **Front**: standalone components (migrate away from `AppModule`), signals + new control flow (`@if`/`@for`), functional interceptors/guards, relative `/api/...` URLs through an Angular dev proxy to port 9000. No UI library — hand-written styles, shared design tokens as CSS custom properties on `:root` in `src/styles.css`.

## Product scope (MVP) — source: `docsForContext/`

`docsForContext/` holds the client's specs (ORION): `specificationsFonctionnelles.pdf`, `contraintesTechniques.pdf`, `transcriptionVideoPresentationProduit.pdf` and the deliverable template `JustificationsDesChoix.docx`. Re-read them (`pdftotext -layout <file> -`) when a requirement is unclear.

**Imposed constraints**: separate front/back communicating through a secured API; SOLID; Spring Core IoC/DI, and always prefer a Spring module (Spring Data JPA, Spring Security, validation…) over a third-party lib; Angular best practices incl. the Angular security guide (no `bypassSecurityTrust*`, no `[innerHTML]` with user content); one Git repo for everything.

**Domain** (glossary: User, Topic, Post, Subscription, Feed, Comment):
- User: email, username (both unique), password (BCrypt). Subscribes to many Topics (join table `subscriptions`).
- Topic: name, description. Topics are seeded data — there is no UI to create them.
- Post: title, content, topic, author, createdAt. Comment: content, post, author, createdAt — flat, one post, **no replies**.
- Feed: posts of the user's subscribed topics, newest first by default, sortable asc/desc.

**Business rules**:
- Register with email + username + password; log in with **email or username** + password; login must **persist across browser sessions** (token in `localStorage`, long-enough expiry). Logout is client-side.
- Password: ≥ 8 chars with at least one digit, one lowercase, one uppercase and one special character — validated on **both** front and back (`^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z\d]).{8,}$`).
- Author and date of posts and comments are set by the server (current user + `@CreationTimestamp`), never sent by the client.
- Profile: view email, username and subscriptions; edit email, username and password; unsubscribe **from the profile page**. Subscribe **from the topics page**, where the button then becomes disabled and reads « Déjà abonné ».
- Since email and username are editable, the JWT subject is the **user id**.
- Every screen is responsive (mobile + desktop). No back-office/admin, and nothing beyond the spec (no post edit/delete, no nested comments, no likes…).

**Target REST API** (keep front and back in sync with this table):

| Method & path | Body → Response |
|---|---|
| `POST /api/auth/register` | `{email, username, password}` → `201 {token}` |
| `POST /api/auth/login` | `{identifier, password}` (email or username) → `{token}` |
| `GET /api/users/me` | → `{id, email, username, subscriptions: Topic[]}` |
| `PUT /api/users/me` | `{email, username, password?}` → profile |
| `GET /api/topics` | → `[{id, name, description, subscribed}]` |
| `POST` / `DELETE /api/topics/{id}/subscription` | → `204` |
| `GET /api/posts?sort=desc\|asc` | → feed `[{id, title, content, createdAt, topicName, authorUsername}]` |
| `POST /api/posts` | `{topicId, title, content}` → `201` post |
| `GET /api/posts/{id}` | → post + `comments: [{id, content, createdAt, authorUsername}]` |
| `POST /api/posts/{id}/comments` | `{content}` → `201` comment |

Errors: `400` validation, `401` unauthenticated, `404` unknown resource, `409` email/username already used.

**Screens** (per the product video): public home with links to login/register → register → login → feed (home when logged in, sort toggle, "create post" button) → topics → post detail with comment form → create-post form → profile (edit form + subscriptions with unsubscribe). Mockups (desktop + mobile) are in Figma: https://www.figma.com/design/Rflr3TVBog35BNMnn0DF09/Maquettes-MDD--desktop-et-mobile-?node-id=0-1&p=f — read them through the Figma MCP tools when available; otherwise ask the user for a screenshot/export of the screen before designing it.

**Deliverable**: `JustificationsDesChoix.docx` must list every technical choice with objective pros and cons, the endpoint table, the test strategy **with coverage**, and the tasks delegated to AI and how they were verified. Work summaries should provide that material.

## Project skills and agents

- `/back-feature <description>` — `.claude/skills/back-feature/SKILL.md`: step-by-step recipe for a server feature (entity → DTO/hand-written mapper → service → controller → tests).
- `/front-feature <description>` — `.claude/skills/front-feature/SKILL.md`: recipe for a client feature (model → HTTP service → standalone components → routes → tests).
- `reviewer` agent (read-only) — reviews the current diff against these conventions and the MVP spec.
- `tester` agent — writes and runs tests; only edits test files and reports production bugs instead of fixing them.
