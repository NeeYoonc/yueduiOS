# Shared URL Normalization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Normalize relative URLs produced by shared parsers so iOS receives absolute toc, cover, chapter, and next-page URLs.

**Architecture:** Add a small KMP `SharedUrlResolver` with manual HTTP/HTTPS resolution for absolute, protocol-relative, root-relative, path-relative, query, fragment, and `..` segments. Keep parser output raw; apply normalization in service layers using `SharedHttpResponse.finalUrl` as the base URL.

**Tech Stack:** Kotlin Multiplatform `:shared`, Kotlin common tests, existing shared service tests, Android unit tests.

---

### Task 1: URL Resolver

**Files:**
- Create: `shared/src/commonMain/kotlin/io/legado/shared/service/SharedUrlResolver.kt`
- Test: `shared/src/commonTest/kotlin/io/legado/shared/service/SharedUrlResolverTest.kt`

- [ ] **Step 1: Write failing resolver test**

Assert that absolute URLs are unchanged, `//cdn.test/a.jpg` inherits scheme, `/catalog` uses the base origin, `chapter/1.html` uses the base directory, `../cover.jpg` removes one segment, and query/fragment-only targets attach to the base path.

- [ ] **Step 2: Run test to verify failure**

Run:
```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'; .\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.SharedUrlResolverTest"
```

Expected: FAIL because `SharedUrlResolver` does not exist.

- [ ] **Step 3: Implement resolver**

Add `SharedUrlResolver.resolve(baseUrl, target)` and `resolveAll(baseUrl, targets)`. Return blank targets unchanged. If the base URL cannot be parsed as HTTP/HTTPS, return the target unchanged.

### Task 2: Service Integration

**Files:**
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/BookInfoService.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/BookTocService.kt`
- Modify: `shared/src/commonMain/kotlin/io/legado/shared/service/BookContentService.kt`
- Tests: existing service tests

- [ ] **Step 1: Update failing service tests**

Use relative parser output for `tocUrl`, `coverUrl`, chapter URL, and `nextContentUrls`; assert service results contain absolute URLs resolved from `response.finalUrl`.

- [ ] **Step 2: Run affected tests to verify failure**

Run:
```powershell
$env:GRADLE_USER_HOME='D:\games\AI\.gradle-legado-cache'; .\gradlew.bat --no-daemon :shared:jvmTest --tests "io.legado.shared.service.BookInfoServiceTest" --tests "io.legado.shared.service.BookTocServiceTest" --tests "io.legado.shared.service.BookContentServiceTest"
```

Expected: FAIL because services still return raw relative URLs.

- [ ] **Step 3: Normalize in services**

Apply resolver only after parser output. Use `response.finalUrl` as base.

### Task 3: Flow Tests And Final Verification

- [ ] Update shared and Android reading-flow tests to use relative detail, toc, and content URLs.
- [ ] Run `:shared:jvmTest`.
- [ ] Run `:app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`.
- [ ] Run `:app:assembleDebug`.
- [ ] Check whether the directory is a Git repo; if not, report commit skipped.
