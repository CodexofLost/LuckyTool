package com.luckyzyx.luckytool.hook.globals

import android.util.ArrayMap
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookGlobalFeatureConfig : YukiBaseHooker() {
    override fun onHook() {
        val list = ArrayMap<String, Boolean>().apply {
            val animLevelPref = preferences(ModulePrefs).getString("customized_anim_level", "-1")
            val launcherHighLevel = preferences(ModulePrefs).getBoolean("force_enable_launcher_high_level", false)
            val sysuiBlurFeature = preferences(ModulePrefs).getBoolean("force_enable_systemui_blur_feature", false)
            val shadeBlurMode = preferences(ModulePrefs).getString("customized_shade_blur_mode", "0")

            if (animLevelPref != "-1" || launcherHighLevel || sysuiBlurFeature || shadeBlurMode != "0") {
                put("oplus.software.view.spring_over_scroller", true)
                put("oplus.software.support_blockable_animation", true)
                put("oplus.software.adaptive_smooth_animation", true)
                put("oplus.software.support_quick_launchapp", true)
                put("oplus.software.support_fluid_entry", true)
                put("oplus.software.fluid_replace_ntf", true)
            }

            // Source SystemUI 强制启用高斯模糊
            if (sysuiBlurFeature || shadeBlurMode != "0") {
                put("oplus.software.display.osie_aisdr2hdr_support", false)
            }
        }
        loadHooker(HookFeatureConfigManager(list))
    }
}
