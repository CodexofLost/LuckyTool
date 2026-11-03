---
context_version: 1.0.8
synced_commit: "5c6dbb3ce"
synced_at: "2026-11-03T14:52:00Z"
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
| **Global Feature Interceptors** | `app/src/main/java/com/luckyzyx/luckytool/hook/globals/HookGlobalFeatureProvider.kt` | Intercepts ColorOS `AppFeatureProvider` queries (`DEVICE_LEVEL_HIGH`, `support_upgrade_anim_material_blur`, etc.). |
| **System Properties Injection** | `app/src/main/java/com/luckyzyx/luckytool/hook/globals/HookGlobalSystemProperties.kt` | Injects animation levels (`persist.sys.oplus.anim_level=2`, `upgrade_anim_level`), `ro.oplus.gaussianlevel`, SurfaceFlinger blur flags. |
| **Launcher Customization** | `app/src/main/java/com/luckyzyx/luckytool/hook/hookers/HookLauncher.kt` | Unlocks dynamic dock blur (`ForceEnableDockerBackgroundBlur`), folder live blur, recents card-stack physics. |
| **SystemUI & Status Bar** | `app/src/main/java/com/luckyzyx/luckytool/hook/hookers/HookSystemUI.kt` | Notification & QS shade dynamic live Gaussian blur (`ForceEnableSystemUIDynamicBlur`) and wallpaper blur mode. |
| **Compose UI & Settings** | `app/src/main/java/com/luckyzyx/luckytool/ui/compose/MainActivity.kt` | Focused 3-page tree: `android_related`, `statusbar_control_center`, `launcher`. |
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
# Local Build / Compilation
./gradlew assembleDebug

# Release Compilation
./gradlew assembleRelease

# Unit Test Execution
./gradlew test

# Stream LuckyTool Logs via pidcat
pidcat com.luckyzyx.luckytool
```
