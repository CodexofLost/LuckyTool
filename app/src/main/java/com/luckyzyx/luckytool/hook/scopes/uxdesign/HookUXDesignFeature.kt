package com.luckyzyx.luckytool.hook.scopes.uxdesign

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class HookUXDesignFeature(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val prefs = preferences(ModulePrefs)
        val launcherAnimTier = prefs.getString("customized_launcher_anim_level", "-1")
        val launcherHigh = prefs.getBoolean("force_enable_launcher_high_level", false)
        val folderBlur = prefs.getBoolean("enable_folder_material_blur", false)

        val tier: Int = (if (launcherAnimTier != "-1") {
            launcherAnimTier.toIntOrNull()
        } else if (launcherHigh) {
            2
        } else {
            null
        }) ?: return

        val isTierAOrAPlus = tier <= 2

        // 1. Hook com.oplus.uxdesign.common.PlatformLevelUtils directly
        val hookClass = { clazz: Class<*>? ->
            clazz?.resolve()?.apply {
                firstMethodOrNull { name = "a"; optional() }?.hook {
                    before { result = tier }
                }
                firstMethodOrNull { name = "b"; optional() }?.hook {
                    before { result = tier }
                }
                firstMethodOrNull { name = "c"; optional() }?.hook {
                    before {
                        val checkTier = args[0] as? Int ?: return@before
                        result = (checkTier == tier)
                    }
                }
                firstMethodOrNull { name = "d"; optional() }?.hook {
                    before { result = true }
                }
                firstMethodOrNull { name = "e"; optional() }?.hook {
                    before {
                        if (isTierAOrAPlus || folderBlur) {
                            result = true
                        }
                    }
                }
            }
        }

        runCatching {
            hookClass("com.oplus.uxdesign.common.PlatformLevelUtils".toClassOrNull())
        }

        // 2. Fallback via DexKit for vendor/regional variant ROMs
        runCatching {
            val dynamicClass = dexKitBridge.findClass {
                matcher {
                    usingStrings("persist.sys.oplus.anim_level")
                }
            }.firstOrNull()?.name?.toClassOrNull()
            hookClass(dynamicClass)
        }
    }
}
