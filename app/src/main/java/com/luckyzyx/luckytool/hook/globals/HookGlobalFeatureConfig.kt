package com.luckyzyx.luckytool.hook.globals

import android.util.ArrayMap
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookGlobalFeatureConfig : YukiBaseHooker() {
    override fun onHook() {
        val list = ArrayMap<String, Boolean>().apply {
            // Source SystemUI 强制启用高斯模糊
            val sysuiBlurFeature = preferences(ModulePrefs).getBoolean("force_enable_systemui_blur_feature", false)
            val shadeBlurMode = preferences(ModulePrefs).getString("customized_shade_blur_mode", "0")
            if (sysuiBlurFeature || shadeBlurMode != "0") {
                put("oplus.software.display.osie_aisdr2hdr_support", false)
            }
        }
        loadHooker(HookFeatureConfigManager(list))
    }
}
