---
name: fullstack-prd-builder
description: "Generate full-stack Spring Boot + Vue3 applications from PRD documents. Use when the user provides a PRD and asks to build a complete full-stack app, generate DDL scripts, create API docs, scaffold backend controllers/services/entities, or scaffold Vue3 + ant-design-vue frontend pages. Keywords: PRD, fullstack, Spring Boot, Vue3, CRUD, DDL, API doc, antdv, JPA, MyBatis-Plus."
---

# Fullstack PRD Builder

## Overview

Generate a complete full-stack application from a PRD or feature specification. The skill produces:
PRD document (with embedded API docs), database DDL, Spring Boot backend code, and Vue3 frontend code.

**Tech stack**: Spring Boot 2.7+ / MySQL / Vue3 + Vite + Axios + ant-design-vue (antdv) @latest.
Backend persistence (MyBatis-Plus or JPA) and JDK version (8 or 17) are determined via user clarification (Step 4.0).
On JDK 17 the user may additionally opt into Spring AI.

## Dispatch Decision Tree

Before starting, determine what the user needs. Do not always run the full pipeline:

```
User request:
|
+-- "Generate PRD + API docs only"
|       -> Run Step 1 only, then stop.
|
+-- "Generate DDL only"
|       -> Step 1 (extract entities from PRD) + Step 2 (clarify DB) + Step 3, then stop.
|
+-- "Generate backend only"
|       -> Step 1 (extract APIs from PRD) + Step 4.0 (clarify tech) + Step 4 + Step 6, then stop.
|
+-- "Generate frontend only"
|       -> Step 1 (extract APIs from PRD) + Step 5 + Step 5.5 + Step 6, then stop.
|
+-- "Add new module to existing project"
|       -> Scan existing code first: find BaseEntity, Result, request.js, router/index.js.
|       -> Reuse existing infrastructure; generate only the new DTO/VO/Controller/Service/Mapper/views.
|       -> Append route to existing router; append API module to existing src/api/.
|
+-- "Build complete app from scratch"
        -> Run all Steps 1-6 in order.
```

## Workflow

### Step 1: Parse PRD and Generate Enhanced PRD (with API Docs)

Read the user's PRD or feature description. If the input is unstructured, first expand it into
structured form following [references/prd-template.md](references/prd-template.md). Show the
structured version to the user and confirm before proceeding.

Generate an enhanced PRD document that includes:

- Product overview, user stories, functional modules
- **API interface documentation** embedded directly in the PRD, containing for each endpoint:
  - Request method + URL path (e.g., `GET /api/user-manage/page`)
  - Request parameters (query/body/path) with types and descriptions
  - Response body structure with field names and types
  - Error codes
- Data model / entity relationships

The API paths defined in the PRD serve as the **source of truth** for Controller route mappings in Step 4.

Output the PRD as a markdown file in the project workspace.

### Step 2: Database Clarification (MANDATORY)

**Before generating any DDL script**, ask the user these two questions:

1. **Database name**: What database name should be used?
2. **Table prefix**: What prefix should all table names use? (e.g., `sys_`, `t_`, `tb_`)

Wait for the user's answer before proceeding to Step 3. Do not assume defaults.

### Step 3: Generate Database DDL

Using the confirmed database name and table prefix from Step 2, generate the DDL script.

- Every business table must include the 4 mandatory audit fields. See [references/database-standards.md](references/database-standards.md).
- Table names and column names use snake_case.
- Generate a single `schema.sql` file containing all `CREATE TABLE` statements plus indexes and comments.

### Step 4: Backend Tech Clarification + Generate Backend Code

#### Step 4.0: Clarify Backend Tech Stack (MANDATORY)

Before generating any backend code, ask the user these questions:

1. **JDK version**: JDK 8 or JDK 17?
2. **Persistence framework**: MyBatis-Plus or JPA (Spring Data JPA)?

If the user chooses JDK 17, also ask:

3. **Spring AI**: Do you want to include Spring AI for LLM integration? (yes/no)

Wait for the user's answers. These choices determine:

| Choice | Impact |
|--------|--------|
| JDK 8 | java.version=8; Spring Boot 2.7.18; no Spring AI; MySQL driver: mysql-connector-java |
| JDK 17 | java.version=17; Spring Boot 3.3.5; Spring AI available; use records; MySQL driver: mysql-connector-j |
| MyBatis-Plus | BaseMapper, @TableName, MetaObjectHandler, MybatisPlusConfig |
| JPA | JpaRepository, @Entity, @MappedSuperclass, @EntityListeners, AuditingEntityListener |
| Spring AI (JDK 17 only) | spring-ai-openai-spring-boot-starter in pom.xml; spring.ai config in application.yml |

#### Step 4.1: Copy Infrastructure Templates

Based on the choices above, copy the matching infrastructure files.

**Always copy (framework-agnostic)**:
- `assets/backend-template/GlobalExceptionHandler.java` -> `{basePackage}/exception/`
- `assets/backend-template/Result.java` -> `{basePackage}/common/`

**If MyBatis-Plus**:
- `assets/backend-template/mybatis-plus/BaseEntity.java` -> `{basePackage}/entity/`
- `assets/backend-template/mybatis-plus/MyMetaObjectHandler.java` -> `{basePackage}/config/`
- `assets/backend-template/mybatis-plus/pom.xml` -> project root
- `assets/backend-template/mybatis-plus/application.yml` -> `src/main/resources/`

**If JPA**:
- `assets/backend-template/jpa/BaseEntity.java` -> `{basePackage}/entity/` (use for JDK 8 / SB 2.7; javax.persistence imports)
- `assets/backend-template/jpa/BaseEntity-jakarta.java` -> `{basePackage}/entity/BaseEntity.java` (use for JDK 17 / SB 3.3; jakarta.persistence imports)
- `assets/backend-template/jpa/JpaAuditConfig.java` -> `{basePackage}/config/`
- `assets/backend-template/jpa/pom.xml` -> project root
- `assets/backend-template/jpa/application.yml` -> `src/main/resources/`

**Template placeholders to replace**:
- `{{basePackage}}` with the Java base package (e.g., `com.example.ordermanage`)
- `{{groupId}}` / `{{artifactId}}` in pom.xml
- `{{javaVersion}}` in pom.xml: set to `8` or `17` per Step 4.0
- `{{springBootVersion}}` in pom.xml: set to `2.7.18` (JDK 8) or `3.3.5` (JDK 17)
- `{{mysqlConnector}}` in pom.xml: set to `mysql-connector-java` (JDK 8) or `mysql-connector-j` (JDK 17)
- `{{dbName}}` in application.yml (from Step 2)

**Template conditionals** (Mustache-style `{{#springAi}}...{{/springAi}}`):
- If Spring AI is selected: keep the content between `{{#springAi}}` and `{{/springAi}}`, then remove the tags.
- If Spring AI is NOT selected: remove everything from `{{#springAi}}` through `{{/springAi}}`, including the tags.

Then generate business code following this layered structure (same regardless of persistence choice):

```
src/main/java/{basePackage}/
  controller/   # @RestController, receives DTO, returns VO
  service/      # Business logic, returns VO to controller
  service/impl/
  mapper/       # Only for MyBatis-Plus: extends BaseMapper<Entity>
  repository/   # Only for JPA: extends JpaRepository<Entity, Long>
  entity/       # extends BaseEntity
  dto/          # Request DTOs (UserSaveDTO, UserQueryDTO, etc.)
  vo/           # Response VOs (UserVO, PageResultVO, etc.)
  config/       # Persistence config + CORS config
  exception/    # GlobalExceptionHandler
```

**Mandatory rules** -- see [references/backend-standards.md](references/backend-standards.md) for full details:
- Controller receives **DTO**, never Entity.
- Service returns **VO**, never Entity.
- Include `GlobalExceptionHandler` catching `MethodArgumentNotValidException` and `RuntimeException`.
- Entity classes extend `BaseEntity` containing the 4 audit fields.
- Controller paths must match the API paths defined in the PRD's API documentation (Step 1).

### Step 5: Generate Frontend Code

First, copy the infrastructure files from `assets/frontend-template/`:
- `request.js` -> `src/utils/request.js`
- `vite.config.js` -> project root
- `.env.development` -> project root
- `router-index.js` -> `src/router/index.js` (fill in module routes from PRD)
- `App.vue` -> `src/App.vue`

Then generate per-module code using **ant-design-vue** components:

```
src/
  api/              # API modules per feature
  views/            # Page components (kebab-case directory names)
  router/           # Vue Router config
  utils/request.js  # Axios instance with interceptors
  components/       # Shared modal components for add/edit
  .env.development  # VITE_API_BASE = '/api'
  vite.config.js    # /api proxy to backend
```

**Mandatory rules** -- see [references/frontend-standards.md](references/frontend-standards.md) for full details:
- Wrap Axios in `request.js` with request interceptor (Token) and response interceptor (`res.data.data` unwrap + error handling).
- Default tables use ant-design-vue `a-table` with pagination and loading state.
- Add/Edit use a **single `a-modal` component**, controlled by `v-model:open`, passing `row` for edit backfill.
- Routes: `path` uses kebab-case (`/user-manage`), `name` uses PascalCase (`UserManage`).
- `.env.development` provides `VITE_API_BASE = '/api'`, `vite.config.js` proxies `/api` to backend.

### Step 5.5: Big-Data Table Clarification (OPTIONAL)

Before writing table components, ask the user:

"Will any table in this project display large amounts of data (1000+ rows, virtual scrolling needed)? If yes, I'll use vxe-table for those tables instead of the default a-table."

- If the user says **no or skips**: use `a-table` throughout (default).
- If the user says **yes**: use `vxe-grid` + `pagerConfig` for the specified big-data tables; keep `a-table` for all other tables.

### Step 6: Verification

Run the automated verification script:

```bash
python scripts/verify.py <output-dir> --prd <prd-file>
```

This checks:
- DDL: every table has CREATE_USER, CREATE_TIME, UPDATE_USER, UPDATE_TIME
- Backend: GlobalExceptionHandler catches MethodArgumentNotValidException + RuntimeException
- Backend: BaseEntity has all 4 audit fields
- Backend: Controller paths match PRD API paths (when --prd is provided)
- Frontend: request.js has both interceptors
- Frontend: .env.development and Vite proxy configured

Fix any FAIL results before declaring the generation complete.

## Coding Standards Reference

Load only the file(s) relevant to the current step:

- **Backend**: [references/backend-standards.md](references/backend-standards.md)
- **Frontend**: [references/frontend-standards.md](references/frontend-standards.md)
- **Database**: [references/database-standards.md](references/database-standards.md)
- **PRD Format**: [references/prd-template.md](references/prd-template.md)
