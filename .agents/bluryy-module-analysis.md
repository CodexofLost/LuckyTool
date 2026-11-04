# BlurryAnimations (v2.9) Module Analysis & Feature Mapping

> **Source Target:** `Bluryyv2.9.zip` (Magisk / KernelSU Overlay Module by Ayan)  
> **Target OS:** ColorOS 15 & ColorOS 16 / realme UI 6 / OxygenOS 15 & 16  
> **Extraction Reference:** `scratch/bluryy_extracted/`

---

## 1. Architectural Overview & Delivery Mechanism

Unlike Xposed/LSPosed hook modules which hook methods in memory, `BlurryAnimations v2.9` operates strictly at the root/ROM file system level through two mechanisms:

1. **Early Boot `system.prop` Injection:**  
   Loaded by Magisk/KernelSU's `resetprop` service before zygote and system services initialize.
2. **VFS Direct Bind-Mounting (`post-fs-data.sh`):**  
   ```bash
   mount --bind "$MODDIR/my_product/etc/extension/" /my_product/etc/extension/
   ```
   Overwrites ColorOS's partition feature configuration XML files to fool `AppFeatureProvider` and `FeatureOption` into reading high-end flagship flags.

---

## 2. System Property Mapping (`system.prop`)

| Property | Value | Component | Feature Mechanism & Purpose |
| :--- | :---: | :--- | :--- |
| `persist.sys.oplus.anim_level` | `2` | Framework | **Animation Level 2:** Triggers ColorOS 15/16's latest fluid parallel animation curves (replaces older Level 1 flagship curves). |
| `ro.oplus.animationlevel` | `2` | Framework / Launcher | Read by `PlatformLevelUtils.getAnimationLevelOS14()` / OS15/16. Unlocks fluid interruption physics and 120Hz gesture response. |
| `persist.sys.oplus.upgrade_anim_level` | `2` | Launcher / SystemUI | **Upgrade Anim Flag:** Signals the material blur and animation upgrade engine to execute the new animation pipeline. |
| `ro.oplus.gaussianlevel` | `2` | Framework / SurfaceFlinger | Sets Gaussian blur rendering tier. Tier `2` provides smooth hardware GPU-accelerated backdrop blur with optimal power consumption. |
| `persist.sys.oplus.systemui.dynamic_blur` | `true` | SystemUI | Activates real-time hardware dynamic blur on Notification Shade, Quick Settings tiles, and volume sliders. |
| `ro.surface_flinger.supports_background_blur` | `1` | SurfaceFlinger | Global Android capability flag instructing RenderEngine to allocate blur render passes. |
| `ro.surface_flinger.media_panel_bg_blur` | `1` | SurfaceFlinger / SystemUI | Explicitly renders dynamic backdrop blur behind the media playback control card in the Notification Shade. |
| `ro.sf.blurs_are_caro` | `1` | SurfaceFlinger | Enables Qualcomm / ColorOS proprietary optimized "Caro" blur filter algorithm. |
| `ro.sf.blurs_are_expensive` | `0` | SurfaceFlinger / WMS | **Critical:** Informs WindowManager that blurs do not cause GPU frame drops; prevents fallback to opaque/solid grey drawables. |
| `disable_window_blurs` | `1` | WindowManager | Prevents Android's accessibility/battery saver automatic blur suppression from taking down system blurs. |
| `ro.launcher.blur.appLaunch` | `0` | Launcher | Disables frozen/static blur snapshot during app launch, allowing fluid live scaling of app windows. |
| `vendor.display.enable_rounded_corner` | `1` | Display HAL / SF | Enforces hardware rounded corners for seamless window scaling and card transitions. |
| `persist.sys.wallpaperanimation.enable` | `true` | Live Wallpaper / Keyguard | Enables ColorOS 15/16 depth-of-field wallpaper expansion animation during unlock and shade pull. |
| `ro.oplus.ltw.ability_level` | `full` | Live Wallpaper | Unlocks full ability tier for Live Wallpaper (depth effect, continuous transitions). |
| `oplus_customize_settings_zoom_wallpaper_enable` | `1` | Framework / Settings | Enables wallpaper zoom-out physics when pulling down the notification panel or opening Recents. |

---

## 3. App Feature XML Mapping (`my_product/etc/extension/com.oplus.app-features.xml`)

These features are queried by applications via `AppFeatureProvider`:

| Feature Name | Value | Target Scope | Feature Behavior |
| :--- | :---: | :--- | :--- |
| `com.oplus.blur.support_upgrade_anim_material_blur` | `boolean:true` | Launcher & SystemUI | **Core Material Blur Key:** Unlocks dynamic material blur for Dock, Folders, and Notification Shade without fallback to grey `LayerNormalDrawable`. |
| `com.android.launcher.support_dynamic_blur` | `boolean:true` | Launcher | Unlocks real-time hardware Gaussian blur in Launcher (Dock, Folders, and Drawer backdrop). |
| `com.android.launcher.DEVICE_LEVEL_HIGH` | *present* | Launcher | Enforces high device tier inside Launcher, unlocking card-stack Recents and fluid gesture transitions. |
| `com.android.systemui.qs_deform_enable` | `boolean:true` | SystemUI | Enables fluid pill-shape morphing and elastic deformation on Quick Settings tiles when dragged. |
| `com.oplus.systemui.panoramic_aod.enable` | `boolean:true` | SystemUI | Enables continuous panoramic transition from AOD $\to$ Lockscreen $\to$ Homescreen. |
| `com.oplus.mediacontroller.fluidConfig` | `{"statusbar_enable_default":1}` | MediaController | Enables Fluid Cloud capsule for ongoing media in the status bar. |

---

## 4. Framework OS Feature XML Mapping (`my_product/etc/extension/com.oplus.oplus-feature.xml`)

These features are queried by `FeatureOption` and `OplusFeatureManager`:

| Feature Name | Target Layer | Effect |
| :--- | :--- | :--- |
| `oplus.software.view.spring_over_scroller` | Android Framework View | Bouncy spring physics overscroll on all scrollable lists across the OS. |
| `oplus.software.support_blockable_animation` | Animation Core | **Interruptible Parallel Animations:** Allows gestures or taps mid-animation without jank or dropped frames. |
| `oplus.software.adaptive_smooth_animation` | WindowManager / Anim | Adaptive frame-rate smoothing curves for app opening/closing. |
| `oplus.software.support_quick_launchapp` | ActivityManager | Eliminates artificial launch delays when rapidly launching apps. |
| `oplus.software.support_fluid_entry` | SystemUI / Metis | Activates Fluid Cloud Dynamic Island capsule entrypoints. |
| `oplus.software.fluid_replace_ntf` | SystemUI / Metis | Replaces traditional banner notifications with Fluid Cloud dynamic island pills. |

---

## 5. Architectural Comparison: Magisk Bind-Mount vs. LuckyTool In-Memory Hooking

| Criterion | BlurryAnimations Magisk Module | LuckyTool Xposed Implementation |
| :--- | :--- | :--- |
| **Execution Path** | Modifies root VFS (`mount --bind` over `/my_product/etc/extension/`) | RAM-only bytecode interception via `HookGlobalFeatureProvider` & `HookGlobalSystemProperties` |
| **Bootloop / Bricking Risk** | **High:** Overwriting `/my_product/etc/extension/` wipes carrier XMLs (`1044_Jio`, `1221_Airtel`), requiring 70+ country XML duplicates to avoid cell network loss. | **Zero:** System partition and `/my_product` remain untouched on disk. |
| **Storage Footprint** | ~200MB (bundled Snapdragon sm8475 AI NLP models and duplicate country XMLs) | **< 5MB APK**, runs entirely in memory. |
| **Granularity & Control** | All-or-nothing: Requires phone reboot to toggle on/off. | Real-time toggles in Compose UI, reboot-free preference syncing. |
