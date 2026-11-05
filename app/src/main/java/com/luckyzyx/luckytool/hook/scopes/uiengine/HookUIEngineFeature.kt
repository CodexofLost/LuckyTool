package com.luckyzyx.luckytool.hook.scopes.uiengine

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class HookUIEngineFeature(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val prefs = preferences(ModulePrefs)
        val launcherAnimTier = prefs.getString("customized_launcher_anim_level", "-1")
        val launcherHigh = prefs.getBoolean("force_enable_launcher_high_level", false)
        val isTierAOrAPlus = (launcherAnimTier in arrayOf("1", "2")) || (launcherAnimTier == "-1" && launcherHigh)

        if (!isTierAOrAPlus) return

        val hookClass = { clazz: Class<*>? ->
            clazz?.resolve()?.apply {
                firstMethodOrNull { name = "isDisableAodAllDayDisplayMode"; optional() }?.hook {
                    before { result = false }
                }
                firstMethodOrNull { name = "isSupportRamlessAod"; optional() }?.hook {
                    before { result = false }
                }
                firstMethodOrNull { name = "isSupportRamlessOrLight"; optional() }?.hook {
                    before { result = false }
                }
                firstMethodOrNull { name = "isSupportInspirationAod"; optional() }?.hook {
                    before { result = true }
                }
                firstMethodOrNull { name = "isSupportCurvedDisplay"; optional() }?.hook {
                    before { result = true }
                }
                firstMethodOrNull { name = "isSupportFlavorOneEdgeClockAod"; optional() }?.hook {
                    before { result = true }
                }
                firstMethodOrNull { name = "isSupportWCG"; optional() }?.hook {
                    before { result = true }
                }
            }
        }

        // 1. Direct class resolution
        runCatching {
            hookClass("com.oplus.aodimpl.utils.AodFeatureOptions".toClassOrNull())
        }

        // 2. Companion isOplusFeatureSupport hook
        runCatching {
            "com.oplus.aodimpl.utils.AodFeatureOptions\$Companion".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "isOplusFeatureSupport"; optional() }?.hook {
                    before {
                        val key = args[0] as? String ?: return@before
                        when (key) {
                            "oplus.software.disable_aod_all_day_mode" -> result = false
                            "oplus.software.display.video_mode_aod_support",
                            "oplus.software.display.wcg_2.0_support",
                            "oplus.uiengine.support_left_swipe",
                            "oplus.software.display.resolution_switch_support" -> result = true
                        }
                    }
                }
            }
        }

        // 3. Fallback via DexKit for regional ROM variations
        runCatching {
            val dynamicClass = dexKitBridge.findClass {
                matcher {
                    usingStrings("oplus.software.disable_aod_all_day_mode")
                }
            }.firstOrNull()?.name?.toClassOrNull()
            hookClass(dynamicClass)
        }
    }
}
