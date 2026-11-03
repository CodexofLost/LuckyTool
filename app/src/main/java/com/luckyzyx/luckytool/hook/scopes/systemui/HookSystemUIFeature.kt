package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.globals.HookGlobalFeatureConfig
import com.luckyzyx.luckytool.hook.globals.HookGlobalFeatureProvider
import com.luckyzyx.luckytool.hook.globals.HookGlobalSystemProperties
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class HookSystemUIFeature(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    override fun onHook() {
        loadHooker(HookGlobalFeatureConfig)
        loadHooker(HookGlobalSystemProperties)
        loadHooker(HookGlobalFeatureProvider(dexKitBridge))

        val shadeBlurMode = preferences(ModulePrefs).getString("customized_shade_blur_mode", "0")
        val forceBlur = preferences(ModulePrefs).getBoolean("force_enable_systemui_blur_feature", false)
        if (forceBlur || shadeBlurMode != "0") {
            loadHooker(ForceEnableSystemUIDynamicBlur)
        }
    }
}
