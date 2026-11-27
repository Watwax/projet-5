# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

MDD — an OpenClassrooms full-stack project: a developer social network where users follow topics and read/write posts and comments. The repo holds two independent apps:

- `back/` — Spring Boot 4.2 REST API (Java 21, Maven wrapper 3.9, Spring Data JPA / Hibernate 7, Spring Security, MySQL 8), package `com.openclassrooms.mddapi`
- `front/` — Angular 22 client (standalone, zoneless, TypeScript 6), unit tests with Jest, E2E with Cypress

Status: the environment is set up (dependencies, configuration, full model layer, security skeleton, Swagger, test tooling). Features are not implemented yet: only the legacy `GET /topic` exists (now answers 401, see Backend notes).

## Working mode: step-by-step

- If a step contradicts a convention here (naming, library, structure…), follow the step, point out the conflict, and offer to update this file.
- If a step's instructions are ambiguous, ask before coding.
- At the end, say which parts of the step are done and what the user still has to do themselves (e.g. a deliverable or a manual check).

## Commands

### Backend (run from `back/`)

```bash
./mvnw spring-boot:run                                   # start the API (always use this) → http://localhost:9000
./mvnw verify                                            # build + all tests + JaCoCo report (target/site/jacoco/index.html)
./mvnw test                                              # all tests
./mvnw test -Dtest=MddApiApplicationTests                # single test class
./mvnw test -Dtest=MddApiApplicationTests#contextLoads   # single test method
```

Running the API needs a local MySQL 8 (installed on the machine, no Docker — decided) with an empty database `mdd` at `localhost:3306`, and a `back/.env` file (git-ignored; template `back/.env.example`) providing `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MS`. It is loaded by `spring.config.import=optional:file:.env[.properties]`, relative to the working directory, so start the API from `back/`.

Tests do **not** need MySQL nor `.env`: `src/test/resources/application.properties` replaces the main config with H2 in MySQL mode and a test JWT key.

Swagger UI: `http://localhost:9000/api/swagger` (OpenAPI JSON: `/api/openapi`).

### Frontend (run from `front/`)

Requires Node ≥ 24.15 (or ≥ 22.22.3) — Angular 22 refuses older versions. The system default `node` is v12; use `export PATH="$HOME/.nvm/versions/node/v24.22.3/bin:$PATH"` (or `nvm use 24`) before any npm/ng command. npm 11 blocks install scripts, so after a fresh `npm install` run `npx cypress install` once.

```bash
npm install
npm run start                     # dev server (always use this) → http://localhost:4200, proxies /api → :9000
npm run build                     # output in dist/
npm test                          # Jest unit tests
npx jest src/app/topic            # specs of a single folder/file
npm run test:coverage             # Jest + coverage (coverage/jest/index.html)
npm run e2e                       # Cypress headless (needs API + dev server running)
npm run cypress:open              # Cypress UI
```

No linter is configured.

## Architecture

### Backend layering

`controller` → `service` (interface `IXxxService` + implementation `XxxService`) → `repository` (Spring Data `JpaRepository`) → `model` (JPA entities).

- Controllers depend on the service **interface**, injected via constructor (no field `@Autowired`).
- Service implementations must be annotated `@Service` to be injected (note: `PostService` currently lacks it).
- The legacy `TopicController` (`GET /topic`) still returns entities; it must become `GET /api/topics` with DTOs when the topics feature is built.
- Model (done, Lombok `@Getter/@Setter/@NoArgsConstructor`): `User` (`users`: email/username unique, BCrypt password, `@ManyToMany subscriptions` via join table `subscriptions`), `Topic` (`topics`: unique name, description), `Post` (`posts`: title, TEXT content, `created_at` via `@CreationTimestamp`, LAZY `topic` + `author`), `Comment` (`comments`: TEXT content, `created_at`, LAZY `post` + `author`). PK columns are `<entity>_id`.
- `config/SecurityConfiguration`: stateless, CSRF/basic/form login disabled, `/api/auth/**` and Swagger paths public, everything else authenticated, 401 answered as ProblemDetail JSON, `BCryptPasswordEncoder` bean. **No JWT filter yet** (added by the authentication feature), so every protected route answers 401. Until a `UserDetailsService` is declared, Boot logs a "generated security password" warning: harmless.
- `config/OpenApiConfiguration`: API info + Bearer JWT scheme for Swagger UI.
- Spring Boot 4 specifics: starters are `spring-boot-starter-webmvc` and per-module test starters (`spring-boot-starter-webmvc-test`, `-data-jpa-test`, `-security-test`); test annotations moved, e.g. `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` / `AutoConfigureMockMvc` and `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`; Jackson 3 (`tools.jackson.*`) is the default JSON library.

### Database

`spring.jpa.hibernate.ddl-auto=create` — Hibernate creates the schema from the entities and **drops it on every startup** (decided for the MVP, data loss accepted; Flyway is a post-MVP evolution). `src/main/resources/data.sql` then seeds the 8 topics (`spring.jpa.defer-datasource-initialization=true`, `spring.sql.init.mode=always`). Only topics are seeded — test accounts, posts and comments are created by hand through the app.

### Frontend

Standalone and zoneless (no zone.js): `main.ts` → `bootstrapApplication(AppComponent, appConfig)`; `app.config.ts` provides the router (`withComponentInputBinding`) and `provideHttpClient(withInterceptors([]))` — register functional interceptors there. Routes live in `app.routes.ts` (only a placeholder lazy `topics` route for now). `proxy.conf.json` forwards `/api` to `http://localhost:9000`. Jest uses `jest-preset-angular` with `setupZonelessTestEnv()` (`setup-jest.ts`); Cypress specs live in `cypress/e2e/`. `src/styles.css` only has a reset: design tokens must come from the Figma mockups.

## Target conventions (decided)

New code follows these; existing code is migrated when touched:

- **Back**: DTOs (Java records) in `dto/request` (input) and `dto/response` (output) + hand-written `@Component` mapper classes (no MapStruct or other mapping library) — never expose JPA entities; Lombok (no `@Data` on entities); Spring Security with stateless JWT (jjwt 0.13), all routes under `/api` and authenticated except `/api/auth/**` and the Swagger paths; Bean Validation (`spring-boot-starter-validation`) for request DTOs; errors as business exceptions in `exception/` turned into **ProblemDetail** (RFC 9457) by a single `@RestControllerAdvice`; API documented with **springdoc-openapi** (Swagger UI at `/api/swagger`, OpenAPI JSON at `/api/openapi`); secrets in `.env`; **JavaDoc** on every public method of services and controllers.
- **Back tests**: JUnit 5 + Mockito for services and mappers, `@WebMvcTest` + MockMvc for controllers, `@SpringBootTest` integration tests on **H2 in MySQL mode** (test scope, no MySQL needed), coverage with **JaCoCo**.
- **Front**: standalone components (default: never write `standalone: true`), signals + new control flow (`@if`/`@for`), functional interceptors/guards, relative `/api/...` URLs through the dev proxy. No UI library — hand-written styles, shared design tokens as CSS custom properties on `:root` in `src/styles.css`. Tests with Jest (`jest.fn()` mocks) + Cypress E2E for sensitive flows.

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

**Deliverable**: `JustificationsDesChoix.docx` must list every technical choice with objective pros and cons, the endpoint table, the test strategy **with coverage**, and the tasks delegated to AI and how they were verified. Work summaries should provide that material. Sections 1 and 2 (architecture, choices, API) are drafted; several choices reuse or fix those of the author's previous OpenClassrooms projects (`../../Projet3/chatop`, `../../Projet4/Testez-et-am-liorez-une-application-full-stack`) — look there for working examples (jjwt service, security config, `.env`, Swagger config, Jest/Cypress setup).

## Project skills and agents

- `/back-feature <description>` — `.claude/skills/back-feature/SKILL.md`: step-by-step recipe for a server feature (entity → DTO/hand-written mapper → service → controller → tests).
- `/front-feature <description>` — `.claude/skills/front-feature/SKILL.md`: recipe for a client feature (model → HTTP service → standalone components → routes → tests).
- `reviewer` agent (read-only) — reviews the current diff against these conventions and the MVP spec.
- `tester` agent — writes and runs tests; only edits test files and reports production bugs instead of fixing them.
