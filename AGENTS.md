# AGENTS.md

Guidelines for AI agents (Codex, ChatGPT, Copilot, etc.) working on this repository.

This file defines operational rules for automated or AI-assisted code changes.

If a generated change conflicts with the architecture defined in `ARCHITECTURE.md`,
**the architecture document takes precedence.**

---

# 1. General Rules

When modifying this repository:

* Prefer **small, safe refactors**
* Do **not mix structural refactors with functional changes**
* Do **not change API contracts unless explicitly requested**
* Do **not rename files unnecessarily**
* Avoid introducing new architectural patterns

If unsure, **follow the patterns used by existing features**.

---

# 2. Architecture Source of Truth

The architecture of this project is defined in:

```id="archdoc"
ARCHITECTURE.md
```

Agents must read this document before implementing any feature.

All code must follow the rules defined there.

---

# 3. Feature Structure

Features live in:

```id="feat"
src/main/kotlin/features/
```

Each feature should follow this structure:

```id="featstruct"
feature_name/
 ├── dto/
 ├── ports/
 ├── FeatureRoutes.kt
 ├── FeatureServiceImpl.kt
 ├── FeatureRepository.kt
 ├── Entity.kt
 └── Table.kt
```

Do not introduce additional layers unless strictly necessary.

---

# 4. Interfaces and Dependency Inversion

Use interfaces to decouple layers.

Rules:

* Routes depend on **service interfaces**
* Services depend on **repository ports**
* Dependency injection must use **interfaces**

Example:

```id="example1"
LinkingService -> LinkingServiceImpl
LinkSessionRepositoryPort -> LinkSessionRepository
```

Do not inject concrete implementations into routes.

---

# 5. DTO Conventions

DTOs must:

* live in `dto/`
* contain only serialized data
* not include business logic

Each DTO should live in **its own file**.

Example:

```id="dtofiles"
CreateLinkSessionResponse.kt
ConfirmLinkSessionRequest.kt
LinkSessionStatusResponse.kt
```

Mapping functions should be placed in a separate file:

```id="mappers"
LinkingDtoMappers.kt
```

---

# 6. Routes

Routes must remain **thin**.

Routes may:

* validate input
* extract authentication
* call services
* map responses

Routes must **not implement business logic**.

---

# 7. Services

Services contain **business logic**.

Rules:

* services must not depend on Ktor
* services must not access HTTP concepts
* services orchestrate repositories

Naming convention:

```id="serviceconv"
FeatureService
FeatureServiceImpl
```

---

# 8. Repositories

Repositories are responsible for persistence.

Rules:

* repositories implement repository ports
* repositories contain no business logic
* repositories perform DB mapping

Example:

```id="repoexample"
LinkSessionRepository : LinkSessionRepositoryPort
```

---

# 9. Dependency Injection

Dependency injection uses **Koin**.

Defined in:

```id="di"
core/di/KoinModules.kt
```

Always inject using interfaces.

Correct:

```id="diok"
single<LinkingService> { LinkingServiceImpl(get()) }
single<LinkSessionRepositoryPort> { LinkSessionRepository() }
```

Incorrect:

```id="dibad"
single { LinkingServiceImpl(get()) }
```

---

# 10. Database Changes

When modifying persistence:

* create a **Flyway migration**
* do not modify existing migrations
* migrations must be incremental

Example:

```id="mig"
V4__add_link_sessions.sql
```

---

# 11. Safe Refactoring Rules

When refactoring:

Allowed:

* moving files
* renaming classes
* extracting interfaces
* separating DTO files
* improving naming

Not allowed:

* changing endpoint contracts
* changing DB schema unintentionally
* changing authentication behavior

---

# 12. When Adding a New Feature

Follow the template:

```id="newfeature"
features/newfeature/

 ├── dto/
 │   ├── CreateThingRequest.kt
 │   └── CreateThingResponse.kt
 │
 ├── ports/
 │   ├── ThingService.kt
 │   └── ThingRepositoryPort.kt
 │
 ├── ThingRoutes.kt
 ├── ThingServiceImpl.kt
 ├── ThingRepository.kt
 ├── Thing.kt
 └── ThingTable.kt
```

---

# 13. Coding Principles

Follow clean code principles:

* small functions
* explicit naming
* single responsibility
* avoid duplication
* avoid hidden side effects

---

# 14. If the Agent Is Unsure

When uncertain:

1. inspect existing features
2. follow the most common pattern
3. avoid introducing new patterns

Consistency is more important than novelty.
