package com.luckyzyx.luckytool.hook.scopes.aod

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class HookAODFeature(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val prefs = preferences(ModulePrefs)
        val launcherAnimTier = prefs.getString("customized_launcher_anim_level", "-1")
        val launcherHigh = prefs.getBoolean("force_enable_launcher_high_level", false)
        val isTierAOrAPlus = (launcherAnimTier in arrayOf("1", "2")) || (launcherAnimTier == "-1" && launcherHigh)

        if (!isTierAOrAPlus) return

        val hookTargetClass = { clazz: Class<*>? ->
            clazz?.resolve()?.apply {
                firstMethodOrNull { name = "h"; optional() }?.hook {
                    before { result = false }
                }
                firstMethodOrNull { name = "i"; optional() }?.hook {
                    before { result = false }
                }
                firstMethodOrNull { name = "j"; optional() }?.hook {
                    before { result = true }
                }
                firstMethodOrNull { name = "n"; optional() }?.hook {
                    before { result = false }
                }
                firstMethodOrNull { name = "w"; optional() }?.hook {
                    before { result = true }
                }
                firstMethodOrNull { name = "z"; optional() }?.hook {
                    before { result = true }
                }
            }
        }

        // 1. Direct class resolution for current ColorOS version
        runCatching {
            hookTargetClass("E2.a".toClassOrNull())
        }

        // 2. DexKit matcher for OTA/version resilience
        runCatching {
            val dynamicClass = dexKitBridge.findClass {
                matcher {
                    usingStrings("oplus.software.disable_aod_all_day_mode")
                }
            }.firstOrNull()?.name?.toClassOrNull()
            hookTargetClass(dynamicClass)
        }
    }
}
