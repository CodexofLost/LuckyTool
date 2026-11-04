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

            if (animLevelPref != "-1") {
                val lvl = animLevelPref.toIntOrNull() ?: 1
                put("persist.sys.oplus.anim_level", lvl)
                put("ro.oplus.animationlevel", if (lvl <= 2) 1 else lvl)
            } else if (launcherHighLevel || sysuiBlurFeature || shadeBlurMode != "0") {
                put("persist.sys.oplus.anim_level", 1)
                put("ro.oplus.animationlevel", 1)
            }

            val sysuiGaussBlurPref = preferences(ModulePrefs).getString("customized_systemui_gaussblur_level", "-1")
            if (sysuiGaussBlurPref != "-1") {
                put("debug.systemui.gaussblur_level", sysuiGaussBlurPref.toIntOrNull() ?: 1)
            } else if (sysuiBlurFeature || shadeBlurMode != "0") {
                put("debug.systemui.gaussblur_level", 1)
            }

            val sysuiDynamicBlurPref = preferences(ModulePrefs).getString("customized_systemui_dynamic_blur", "-1")
            if (sysuiDynamicBlurPref == "1") {
                put("persist.sys.oplus.systemui.dynamic_blur", "true")
                put("ro.surface_flinger.supports_background_blur", true)
                put("persist.sys.sf.disable_blurs", false)
                put("persist.sysui.disableBlur", false)
            } else if (sysuiDynamicBlurPref == "0") {
                put("persist.sys.oplus.systemui.dynamic_blur", "false")
            } else if (sysuiBlurFeature || shadeBlurMode == "1") {
                put("ro.surface_flinger.supports_background_blur", true)
                put("persist.sys.sf.disable_blurs", false)
                put("ro.oplus.gaussianlevel", 3)
                put("persist.sys.oplus.systemui.dynamic_blur", "true")
                put("persist.sysui.disableBlur", false)
            }
        }
        loadHooker(HookSystemProperties(list))
    }
}
