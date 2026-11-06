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
            val capsuleLiveAlerts = prefs.getBoolean("enable_fluid_cloud_capsule_live_alerts", false)

            if (springOverscroll) {
                put("oplus.software.view.spring_over_scroller", true)
            }

            if (parallelGestures || capsuleLiveAlerts) {
                put("oplus.software.support_fluid_entry", true)
                put("oplus.software.fluid_replace_ntf", true)
            }

            if (capsuleLiveAlerts) {
                put("oplus.software.notification_alert_support_fifo", true)
            }

            if (parallelGestures) {
                put("oplus.software.support_blockable_animation", true)
                put("oplus.software.support_quick_launchapp", true)
            }

            if (adaptiveSmooth) {
                put("oplus.software.adaptive_smooth_animation", true)
            }

            // Source SystemUI 强制启用高斯模糊
            val sysuiBlurFeature = prefs.getBoolean("force_enable_systemui_blur_feature", false)
            if (sysuiBlurFeature) {
                put("oplus.software.display.osie_aisdr2hdr_support", false)
            }

            val panoramicAod = prefs.getBoolean("enable_panoramic_aod", false)
            if (panoramicAod) {
                put("oplus.software.display.aod_ramless_support", false)
            }
        }
        loadHooker(HookFeatureConfigManager(list))
    }
}
