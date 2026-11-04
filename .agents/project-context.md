---
context_version: 1.2.4
synced_commit: "f0b9982fe"
synced_at: "2026-11-04T10:45:00Z"
---

# Project Context: LuckyTool

## 1. Quick Specs & Project Cockpit

| Property | Details / Specification |
| :--- | :--- |
| **Identifier** | `com.luckyzyx.luckytool` |
| **Runtime / Platform** | Kotlin 2.1 / JVM 21, Android SDK 30–37 (`compileSdk = 37`, `targetSdk = 37`, `minSdk = 30`) |
| **Toolchain & Prerequisites** | JDK 21, Gradle, Android NDK (target `arm64-v8a`), LSPosed / LibXposed runtime |
| **Environment & Config** | `gradle.properties`, `app/version.properties`, `local.properties` |
| **Core Stack** | Jetpack Compose, YukiHookAPI 1.5.0, LibXposed API 102.0.0, DexKit (`org.luckypray.dexkit`), Kotlin Serialization |
| **Core Focus Scope** | QS & Notification Shade dynamic blur, Launcher dock/folder dynamic blur, Flagship parallel/fluid animations |
| **Primary Commands** | `Build: fast-gradlew assembleDebug` \| `Release: fast-gradlew assembleRelease` \| `CI: gh workflow run build.yml` |

## 2. Component Ownership & Architecture Map

| Subsystem / Layer | God Node / Entrypoint (`repo-relative-path`) | Public Contract & Core Responsibility |
| :--- | :--- | :--- |
| **Hook Core Engine** | `app/src/main/java/com/luckyzyx/luckytool/hook/MainHook.kt` | `MainHook`: Entrypoint for LSPosed/LibXposed; filtered strictly to `system`, `com.android.systemui`, and `com.android.launcher`. |
| **Global Feature Interceptors** | `app/src/main/java/com/luckyzyx/luckytool/hook/globals/HookGlobalFeatureProvider.kt` | Intercepts decoupled ColorOS `AppFeatureProvider` queries (`DEVICE_LEVEL_HIGH`, `support_upgrade_anim_material_blur`, `panoramic_aod.enable`, etc.). |
| **System Properties Injection** | `app/src/main/java/com/luckyzyx/luckytool/hook/globals/HookGlobalSystemProperties.kt` | Injects decoupled animation levels (Framework Level 1–3, Launcher Tier A+/A/B+/B), `ro.oplus.gaussianlevel`, SurfaceFlinger blur flags, media panel blur. |
| **Launcher Customization** | `app/src/main/java/com/luckyzyx/luckytool/hook/hookers/HookLauncher.kt` | Unlocks dynamic dock blur (`ForceEnableDockerBackgroundBlur`), launcher material live blur, recents card-stack physics, wallpaper depth/zoom animations, and decoupled animation tiers (A+/A/B+/B). |
| **SystemUI & Status Bar** | `app/src/main/java/com/luckyzyx/luckytool/hook/scopes/systemui/HookSystemUIFeature.kt` | Notification & QS shade dynamic live blur (`ForceEnableSystemUIDynamicBlur` via `force_enable_systemui_blur_feature`), and panoramic AOD unlock (`enable_panoramic_aod`). |
| **Compose UI & Settings** | `app/src/main/java/com/luckyzyx/luckytool/ui/compose/MainActivity.kt` | Focused 3-page tree: `android_related`, `statusbar_control_center`, `launcher`, each with 1-tap in-page scope reload operations. |
| **Bluryy Module Blueprint** | `.agents/bluryy-module-analysis.md` | Full reverse-engineering spec of `BlurryAnimations v2.9` Magisk module (properties, XML features). |

### Critical Execution Traces
* **Hook Initialization:** `Zygote / App Launch` → `MainHook.onHook()` → `Package Matching` → `Specific Hookers (Launcher/SystemUI/Android)`
* **Preference Propagation:** `UI Switch (MainActivity)` → `ModulePrefs (ContentProvider/SP)` → `Target Process Hook Cache` → `Real-Time System Reflection`
* **Dynamic Symbol Discovery:** `DexKitBridge` → `Memory DEX Search (Strings/Types)` → `Resolved Method` → `YukiHook Hook Assignment`

## 3. Domain & Technical Specifications

* **Target Operating Systems:** ColorOS 12 through ColorOS 17 / realme UI 3.0 through realme UI 7.0 / OxygenOS 12+.
* **Inter-Process Preference Sync:** Leverages YukiHookAPI's WorldReadable/ContentProvider module preferences for live, reboot-free setting toggles.
* **Obfuscation Resilience:** ColorOS internals undergo rapid obfuscation changes across point updates. All sensitive system hooks leverage DexKit bytecode signatures rather than fixed method names.

## 4. Architectural Invariants & Guardrails

* **Zero System Partition Writes:** All modifications run entirely in RAM via Xposed/LSPosed method hooks and bytecode manipulation.
* **Process Boundary Separation:** UI state in `com.luckyzyx.luckytool` must never access system services directly without checking user permissions; hooks inside `com.android.systemui` or `android` run in foreign security contexts.
* **Fail-Safe Exception Isolation:** Every hook must be guarded by try-catch or YukiHook logging to avoid unhandled exceptions causing SystemUI or SystemServer crash loops.

### Known Landmines & Anti-Hallucination Guardrails
* `TRAP: Setting disable_window_blurs = 1 in system properties` → `RISK: Globally disables window blurs in Android WindowManager across OS` → `FIX: Set disable_window_blurs = 0 or omit entirely.`
* `TRAP: Bundling launcherHigh into all feature toggles or gating all launcher hooks under single boolean` → `RISK: Cross-feature interference, overriding user tier choices, un-applied switches` → `FIX: Fully decouple sub-hookers and check dedicated preference keys.`
* `TRAP: Inverting ColorOS Launcher getAnimationLevelOS14 mapping or using legacy 0/1/2 indices` → `RISK: validAnimLevel in PlatformLevelUtils expects 1 (A+), 2 (A), 3 (B+), 4 (B). Other values fall back or break animation curves` → `FIX: Return user configured tier directly (1–4) without inversion.`
* `TRAP: Adding fake AppFeature keys like NEW_RECENTS_VIBRATION or support_spring_animation_app_start` → `RISK: ColorOS launcher does not check them (spring launch is hardcoded to true on ColorOS; recents layout uses CommonSettingsValueProxy)` → `FIX: Inspect disassembled APK bytecode before introducing new feature keys.`
* `TRAP: Hardcoding getPlatformAnimationLevel = 1 in SystemUI` → `RISK: Demotes SystemUI animations to LEVEL_LOW (1) in ColorOS` → `FIX: Return user configured framework animation level (default 3 = LEVEL_HIGH).`
* `TRAP: Direct method reflection by hardcoded name on ColorOS classes` → `RISK: MethodNotFoundException and feature silent failure on minor OTA updates` → `FIX: Always use DexKitBridge string pattern or parameter type matching.`
* `TRAP: Blind directory bind mounts over /my_product or /my_stock` → `RISK: Masks carrier, memory, and telephony XMLs causing bootloops` → `FIX: Intercept feature providers via HookAppFeatureProvider in RAM.`
* `TRAP: Reading SharedPreferences synchronously from foreign process hooks` → `RISK: SecurityException / Permission denial on Android 12+` → `FIX: Use YukiHookAPI preferences(ModulePrefs) bridge.`

## 5. End-to-End Data & State Flow Pipelines

* **Setting Toggle Flow:** `User toggles option in Compose UI → Stored in ModulePrefs → Target App process notified → Hook cache updated → Next framework call uses modified value.`
* **DexKit Search Flow:** `Target process starts → Hook checks cache → If uncached, DexKit scans loaded DEX classes in RAM → Method reference found & cached → YukiBaseHooker attaches.`

## 6. UI/UX Design System & Tokens

* **Framework:** Jetpack Compose with dual design system support (Material 3 Dynamic Theming + Miuix Components).
* **Color Schemes:** Adapts to ColorOS system accent color / Monet palette dynamically.
* **Layout Geometry:** Rounded card surfaces (`16.dp`–`24.dp`), segmented preference toggles, fluid transition animations.

## 7. Build, Verification & Diagnostic Recipes

```bash
# Local Build / Compilation (Always fast-gradlew, never ./gradlew)
fast-gradlew assembleDebug

# Release Compilation
fast-gradlew assembleRelease

# Unit Test Execution
fast-gradlew test

# Stream LuckyTool Logs via pidcat
pidcat com.luckyzyx.luckytool
```
