package com.luckyzyx.luckytool.hook.globals

import android.util.ArrayMap
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookGlobalFeatureConfig : YukiBaseHooker() {
    override fun onHook() {
        val list = ArrayMap<String, Boolean>().apply {
            val prefs = preferences(ModulePrefs)

            val springOverscroll = prefs.getBoolean("enable_spring_overscroll", false)
            val parallelGestures = prefs.getBoolean("enable_parallel_gesture_animation", false)
            val adaptiveSmooth = prefs.getBoolean("enable_adaptive_smooth_animation", false)

            if (springOverscroll) {
                put("oplus.software.view.spring_over_scroller", true)
            }

            if (parallelGestures) {
                put("oplus.software.support_blockable_animation", true)
                put("oplus.software.support_quick_launchapp", true)
                put("oplus.software.support_fluid_entry", true)
                put("oplus.software.fluid_replace_ntf", true)
            }

            if (adaptiveSmooth) {
                put("oplus.software.adaptive_smooth_animation", true)
            }

            // Source SystemUI 强制启用高斯模糊
            val sysuiBlurFeature = prefs.getBoolean("force_enable_systemui_blur_feature", false)
            if (sysuiBlurFeature) {
                put("oplus.software.display.osie_aisdr2hdr_support", false)
            }

            // Flagship Tier A/A+ Display & AOD Features
            val launcherAnimTier = prefs.getString("customized_launcher_anim_level", "-1")
            val launcherHigh = prefs.getBoolean("force_enable_launcher_high_level", false)
            val isTierAOrAPlus = (launcherAnimTier in arrayOf("1", "2")) || (launcherAnimTier == "-1" && launcherHigh)

            if (isTierAOrAPlus) {
                // UI Engine & AOD Capabilities
                put("oplus.software.disable_aod_all_day_mode", false)
                put("oplus.software.display.video_mode_aod_support", true)
                put("oplus.software.display.wcg_2.0_support", true)
                put("oplus.uiengine.support_left_swipe", true)
                put("oplus.software.display.resolution_switch_support", true)
            }

            val panoramicAod = prefs.getBoolean("enable_panoramic_aod", false)
            if (panoramicAod || isTierAOrAPlus) {
                put("oplus.software.display.aod_ramless_support", false)
            }
        }
        loadHooker(HookFeatureConfigManager(list))
    }
}
