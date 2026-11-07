package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.globals.HookGlobalFeatureConfig
import com.luckyzyx.luckytool.hook.globals.HookGlobalFeatureProvider
import com.luckyzyx.luckytool.hook.globals.HookGlobalSystemProperties
import com.luckyzyx.luckytool.utils.DexkitUtils
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookFluidCloudCapsule : YukiBaseHooker() {
    override fun onHook() {
        loadHooker(HookGlobalFeatureConfig)
        loadHooker(HookGlobalSystemProperties)

        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->
            loadHooker(HookGlobalFeatureProvider(dexKitBridge))
        }
    }
}
