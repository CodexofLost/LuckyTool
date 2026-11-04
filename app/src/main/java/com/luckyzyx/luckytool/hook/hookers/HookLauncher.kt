package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.globals.HookGlobalFeatureConfig
import com.luckyzyx.luckytool.hook.globals.HookGlobalFeatureProvider
import com.luckyzyx.luckytool.hook.globals.HookGlobalSystemProperties
import com.luckyzyx.luckytool.hook.scopes.launcher.ForceEnableDockerBackgroundBlur
import com.luckyzyx.luckytool.hook.scopes.launcher.HookLauncherFeature
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookLauncher : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        loadHooker(HookGlobalFeatureConfig)
        loadHooker(HookGlobalSystemProperties)

        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->
            loadHooker(HookGlobalFeatureProvider(dexKitBridge))
            loadHooker(HookLauncherFeature(dexKitBridge))
        }

        // 强制启用 Docker 背景模糊及高级动画材料模糊
        if (preferences(ModulePrefs).getString("customized_anim_level", "-1") == "1" ||
            preferences(ModulePrefs).getBoolean("force_enable_docker_background_blur", false) ||
            preferences(ModulePrefs).getBoolean("enable_docker_background", false) ||
            preferences(ModulePrefs).getBoolean("force_enable_systemui_blur_feature", false) ||
            preferences(ModulePrefs).getBoolean("force_enable_launcher_high_level", false)
        ) {
            if (osCode >= 34) loadHooker(ForceEnableDockerBackgroundBlur)
        }
    }
}
