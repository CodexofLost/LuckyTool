package com.luckyzyx.luckytool.hook.globals

import android.util.ArrayMap
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookGlobalSystemProperties : YukiBaseHooker() {
    override fun onHook() {
        val list = ArrayMap<String, Any>().apply {
            // Source Android OplusPlatformLevelUtils GaussianLevel
            when (preferences(ModulePrefs).getString("customized_gaussian_blur_effect_level", "0")) {
                "0" -> put("ro.oplus.gaussianlevel", 0)
                "1" -> put("ro.oplus.gaussianlevel", 1)
                "2" -> put("ro.oplus.gaussianlevel", 2)
                "3" -> put("ro.oplus.gaussianlevel", 3)
            }

            // Source Animation & Blur System Properties
            val animLevelPref = preferences(ModulePrefs).getString("customized_anim_level", "-1")
            val launcherHighLevel = preferences(ModulePrefs).getBoolean("force_enable_launcher_high_level", false) ||
                    preferences(ModulePrefs).getBoolean("force_enable_docker_background_blur", false)
            val sysuiBlurFeature = preferences(ModulePrefs).getBoolean("force_enable_systemui_blur_feature", false)
            val shadeBlurMode = preferences(ModulePrefs).getString("customized_shade_blur_mode", "0")

            val effectiveAnimLevel = if (animLevelPref != "-1") {
                animLevelPref.toIntOrNull() ?: 2
            } else if (launcherHighLevel || sysuiBlurFeature || shadeBlurMode != "0") {
                2
            } else {
                -1
            }

            if (effectiveAnimLevel != -1) {
                put("persist.sys.oplus.anim_level", effectiveAnimLevel)
                put("ro.oplus.animationlevel", effectiveAnimLevel)
                put("persist.sys.oplus.upgrade_anim_level", effectiveAnimLevel)
                put("ro.launcher.blur.appLaunch", 0)
                put("vendor.display.enable_rounded_corner", 1)
                put("persist.sys.wallpaperanimation.enable", "true")
                put("ro.oplus.ltw.ability_level", "full")
                put("oplus_customize_settings_zoom_wallpaper_enable", 1)
            }

            val sysuiGaussBlurPref = preferences(ModulePrefs).getString("customized_systemui_gaussblur_level", "-1")
            if (sysuiGaussBlurPref != "-1") {
                put("debug.systemui.gaussblur_level", sysuiGaussBlurPref.toIntOrNull() ?: 1)
            } else if (sysuiBlurFeature || shadeBlurMode != "0") {
                put("debug.systemui.gaussblur_level", 1)
            }

            val sysuiDynamicBlurPref = preferences(ModulePrefs).getString("customized_systemui_dynamic_blur", "-1")
            val isBlurActive = sysuiDynamicBlurPref == "1" || sysuiBlurFeature || shadeBlurMode != "0" || launcherHighLevel

            if (isBlurActive) {
                put("ro.surface_flinger.supports_background_blur", true)
                put("ro.surface_flinger.media_panel_bg_blur", 1)
                put("ro.sf.blurs_are_caro", 1)
                put("ro.sf.blurs_are_expensive", 0)
                put("disable_window_blurs", 1)
                put("persist.sys.sf.disable_blurs", false)
                put("persist.sysui.disableBlur", false)
                if (sysuiDynamicBlurPref != "0") {
                    put("persist.sys.oplus.systemui.dynamic_blur", "true")
                }
            } else if (sysuiDynamicBlurPref == "0") {
                put("persist.sys.oplus.systemui.dynamic_blur", "false")
            }
        }
        loadHooker(HookSystemProperties(list))
    }
}
