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

            // Source Android OplusPlatformLevelUtils GaussianLevel
            when (prefs.getString("customized_gaussian_blur_effect_level", "-1")) {
                "0" -> put("ro.oplus.gaussianlevel", 0)
                "1" -> put("ro.oplus.gaussianlevel", 1)
                "2" -> put("ro.oplus.gaussianlevel", 2)
                "3" -> put("ro.oplus.gaussianlevel", 3)
            }

            // Framework Animation Level (ro.oplus.animationlevel: 1=Low, 2=Middle, 3=High)
            val frameworkAnimLevel = prefs.getString("customized_framework_anim_level", "-1")
            val legacyAnimLevel = prefs.getString("customized_anim_level", "-1")
            val effectiveFrameworkLevel = if (frameworkAnimLevel != "-1") {
                frameworkAnimLevel.toIntOrNull()
            } else if (legacyAnimLevel != "-1") {
                legacyAnimLevel.toIntOrNull()
            } else {
                null
            }
            if (effectiveFrameworkLevel != null) {
                put("ro.oplus.animationlevel", effectiveFrameworkLevel)
            }

            // Launcher Animation Tier (persist.sys.oplus.anim_level: 1=A+, 2=A, 3=B+, 4=B)
            val launcherAnimTier = prefs.getString("customized_launcher_anim_level", "-1")
            val launcherHighLevel = prefs.getBoolean("force_enable_launcher_high_level", false)
            val effectiveLauncherTier = if (launcherAnimTier != "-1") {
                launcherAnimTier.toIntOrNull()
            } else if (launcherHighLevel) {
                2
            } else if (legacyAnimLevel != "-1") {
                legacyAnimLevel.toIntOrNull()
            } else {
                null
            }
            if (effectiveLauncherTier != null) {
                put("persist.sys.oplus.anim_level", effectiveLauncherTier)
                put("persist.sys.oplus.upgrade_anim_level", effectiveLauncherTier)
                put("ro.launcher.blur.appLaunch", 0)
            }

            // Media Player Blur Background
            if (prefs.getBoolean("enable_media_player_blur", false)) {
                put("ro.surface_flinger.media_panel_bg_blur", 1)
            }

            // Live Wallpaper Depth Zooming
            if (prefs.getBoolean("enable_wallpaper_zoom_animation", false)) {
                put("persist.sys.wallpaperanimation.enable", "true")
                put("ro.oplus.ltw.ability_level", "full")
                put("oplus_customize_settings_zoom_wallpaper_enable", 1)
            }

            // SurfaceFlinger GPU Blur Penalty Bypass & SystemUI Blur Flags
            val sysuiBlurFeature = prefs.getBoolean("force_enable_systemui_blur_feature", false)
            val shadeBlurMode = prefs.getString("customized_shade_blur_mode", "0")
            val dockerBlur = prefs.getBoolean("force_enable_docker_background_blur", false)
            val sfBlurBypass = prefs.getBoolean("sf_blurs_are_expensive_bypass", false)
            val sysuiDynamicBlurPref = prefs.getString("customized_systemui_dynamic_blur", "-1")

            val shouldUnblockSfBlurs = sfBlurBypass || sysuiBlurFeature || shadeBlurMode != "0" || dockerBlur || sysuiDynamicBlurPref == "1"
            if (shouldUnblockSfBlurs) {
                put("ro.surface_flinger.supports_background_blur", true)
                put("ro.sf.blurs_are_caro", 1)
                put("ro.sf.blurs_are_expensive", 0)
                put("disable_window_blurs", 1)
                put("persist.sys.sf.disable_blurs", false)
                put("persist.sysui.disableBlur", false)
                put("vendor.display.enable_rounded_corner", 1)
            }

            val sysuiGaussBlurPref = prefs.getString("customized_systemui_gaussblur_level", "-1")
            if (sysuiGaussBlurPref != "-1") {
                put("debug.systemui.gaussblur_level", sysuiGaussBlurPref.toIntOrNull() ?: 1)
            } else if (sysuiBlurFeature || shadeBlurMode != "0") {
                put("debug.systemui.gaussblur_level", 1)
            }

            if (sysuiDynamicBlurPref == "1" || sysuiBlurFeature || shadeBlurMode != "0") {
                put("persist.sys.oplus.systemui.dynamic_blur", "true")
            } else if (sysuiDynamicBlurPref == "0") {
                put("persist.sys.oplus.systemui.dynamic_blur", "false")
            }
        }
        loadHooker(HookSystemProperties(list))
    }
}
