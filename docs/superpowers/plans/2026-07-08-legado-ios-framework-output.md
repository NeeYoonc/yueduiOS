# Legado iOS Framework Output Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce a Swift-consumable `LegadoShared` framework/XCFramework from the shared KMP module.

**Architecture:** Keep all current shared logic in `commonMain` and configure each iOS target to build a static framework named `LegadoShared`. Register the frameworks into a Kotlin Multiplatform `XCFramework` aggregator for Xcode integration.

**Tech Stack:** Kotlin Multiplatform, Kotlin/Native Apple targets, Gradle.

---

### Task 1: Framework Task Red-Green

**Files:**
- Modify: `shared/build.gradle.kts`

- [x] **Step 1: Write the failing check**

Run `:shared:tasks --all` and fail if `assembleLegadoShared...XCFramework` or `linkDebugFrameworkIosX64` is missing.

- [x] **Step 2: Verify red**

Run: `.\gradlew.bat --no-daemon -q :shared:tasks --all`

Expected: no `LegadoShared` framework/XCFramework tasks are present.

- [x] **Step 3: Configure framework output**

Create a `LegadoShared` XCFramework aggregator and add static frameworks for `iosX64`, `iosArm64`, and `iosSimulatorArm64`.

- [x] **Step 4: Verify green**

Run the same task-list check.

Expected: framework/XCFramework tasks are present.

### Task 2: Regression Verification

**Files:**
- No additional files.

- [x] **Step 1: Run shared tests**

Run: `.\gradlew.bat --no-daemon -q :shared:jvmTest`

- [x] **Step 2: Run Android shared tests**

Run: `.\gradlew.bat --no-daemon -q :app:testAppDebugUnitTest --tests "io.legado.app.shared.*"`

- [x] **Step 3: Run Android debug build**

Run: `.\gradlew.bat --no-daemon -q :app:assembleDebug`
