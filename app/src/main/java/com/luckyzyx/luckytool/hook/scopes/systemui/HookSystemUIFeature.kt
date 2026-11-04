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

        val enableNotifBlur = preferences(ModulePrefs).getBoolean("enable_notification_background_blur_effect", false)
        if (enableNotifBlur) {
            loadHooker(NotificationBackgroundBlurAlpha)
        }
    }
}
