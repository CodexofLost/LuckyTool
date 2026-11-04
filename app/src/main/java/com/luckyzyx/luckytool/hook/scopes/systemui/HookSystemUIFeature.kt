package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.globals.HookGlobalFeatureProvider
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class HookSystemUIFeature(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    override fun onHook() {
        loadHooker(HookGlobalFeatureProvider(dexKitBridge))

        val forceBlur = preferences(ModulePrefs).getBoolean("force_enable_systemui_blur_feature", false)
        if (forceBlur) {
            loadHooker(ForceEnableSystemUIDynamicBlur)
        }

        loadHooker(NotificationBackgroundBlurAlpha)
        loadHooker(ControlCenterBackgroundTransParency)
    }
}
