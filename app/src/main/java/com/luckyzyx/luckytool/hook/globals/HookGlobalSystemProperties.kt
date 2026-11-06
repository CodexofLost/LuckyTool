package com.luckyzyx.luckytool.hook.globals

import android.util.ArrayMap
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookGlobalSystemProperties : YukiBaseHooker() {
    override fun onHook() {
        val list = ArrayMap<String, Any>().apply {
            val prefs = preferences(ModulePrefs)

            val sysuiBlurFeature = prefs.getBoolean("force_enable_systemui_blur_feature", false)
            val adaptiveSmooth = prefs.getBoolean("enable_adaptive_smooth_animation", false)

            // Source Android OplusPlatformLevelUtils GaussianLevel (-1 = untouched stock ROM default)
            val userGaussianLevel = prefs.getString("customized_gaussian_blur_effect_level", "-1")
            val effectiveGaussianLevel = when {
                userGaussianLevel == "0" -> 0 // Explicit user disable strictly respected
                userGaussianLevel != "-1" -> userGaussianLevel.toIntOrNull() ?: 3
                sysuiBlurFeature -> 3 // Baseline requirement when SystemUI dynamic blur is enabled
                else -> null // -1 and blur OFF -> untouched
            }
            if (effectiveGaussianLevel != null) {
                put("ro.oplus.gaussianlevel", effectiveGaussianLevel)
            }

            // Framework Animation Level (ro.oplus.animationlevel: 1=Low, 2=Middle, 3=High)
            val frameworkAnimLevel = prefs.getString("customized_framework_anim_level", "-1")
            val effectiveFrameworkLevel = when {
                frameworkAnimLevel != "-1" -> frameworkAnimLevel.toIntOrNull() ?: 3
                sysuiBlurFeature -> 3
                else -> null
            }
            if (effectiveFrameworkLevel != null) {
                put("ro.oplus.animationlevel", effectiveFrameworkLevel)
            }

            // Launcher Animation Tier (persist.sys.oplus.anim_level: 1=A+, 2=A, 3=B+, 4=B)
            val launcherAnimTier = prefs.getString("customized_launcher_anim_level", "-1")
            val launcherHighLevel = prefs.getBoolean("force_enable_launcher_high_level", false)
            val effectiveLauncherTier = when {
                launcherAnimTier != "-1" -> launcherAnimTier.toIntOrNull()
                launcherHighLevel || sysuiBlurFeature || adaptiveSmooth -> 1
                else -> null
            }
            if (effectiveLauncherTier != null) {
                put("persist.sys.oplus.anim_level", effectiveLauncherTier)
                put("persist.sys.oplus.upgrade_anim_level", effectiveLauncherTier)
                put("ro.launcher.blur.appLaunch", 0)
            }


            // Live Wallpaper Depth Zooming
            if (prefs.getBoolean("enable_wallpaper_zoom_animation", false)) {
                put("persist.sys.wallpaperanimation.enable", "true")
                put("ro.oplus.ltw.ability_level", "full")
                put("oplus_customize_settings_zoom_wallpaper_enable", 1)
            }

            // SurfaceFlinger GPU Blur Penalty Bypass & SystemUI Blur Flags
            val sfBlurBypass = prefs.getBoolean("sf_blurs_are_expensive_bypass", false)
            val sysuiDynamicBlurPref = prefs.getString("customized_systemui_dynamic_blur", "-1")

            val shouldUnblockSfBlurs = sfBlurBypass || sysuiBlurFeature || sysuiDynamicBlurPref == "1"
            if (shouldUnblockSfBlurs) {
                put("ro.surface_flinger.supports_background_blur", true)
                put("ro.sf.blurs_are_caro", 1)
                put("ro.sf.blurs_are_expensive", 0)
                put("disable_window_blurs", 0)
                put("persist.sys.sf.disable_blurs", false)
                put("persist.sysui.disableBlur", false)
                put("vendor.display.enable_rounded_corner", 1)
            }

            val sysuiGaussBlurPref = prefs.getString("customized_systemui_gaussblur_level", "-1")
            if (sysuiGaussBlurPref != "-1") {
                put("debug.systemui.gaussblur_level", sysuiGaussBlurPref.toIntOrNull() ?: 1)
            } else if (sysuiBlurFeature) {
                put("debug.systemui.gaussblur_level", 1)
            }

            if (sysuiDynamicBlurPref == "1" || sysuiBlurFeature) {
                put("persist.sys.oplus.systemui.dynamic_blur", "true")
            } else if (sysuiDynamicBlurPref == "0") {
                put("persist.sys.oplus.systemui.dynamic_blur", "false")
            }

            // Fluid Cloud & Live Alerts Capsule Unblock
            val enableCapsuleLiveAlerts = prefs.getBoolean("enable_fluid_cloud_capsule_live_alerts", false)
            if (enableCapsuleLiveAlerts) {
                put("persist.sys.oplus.live_bridge.support", 1)
                put("persist.sys.oplus.capsule.support", 1)
                put("debug.oplus.capsule.enable", 1)
                put("persist.sys.oplus.notification.alert", 1)
                put("ro.oplus.support.notification.capsule", 1)
            }
        }
        loadHooker(HookSystemProperties(list))
    }
}
