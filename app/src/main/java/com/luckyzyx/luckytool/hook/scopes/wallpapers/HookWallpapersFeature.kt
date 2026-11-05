package com.luckyzyx.luckytool.hook.scopes.wallpapers

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.firstMethodOrNull
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class HookWallpapersFeature(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val prefs = preferences(ModulePrefs)
        val launcherAnimTier = prefs.getString("customized_launcher_anim_level", "-1")
        val launcherHigh = prefs.getBoolean("force_enable_launcher_high_level", false)

        val tier: Int = (if (launcherAnimTier != "-1") {
            launcherAnimTier.toIntOrNull()
        } else if (launcherHigh) {
            2
        } else {
            null
        }) ?: return

        val isTierAOrAPlus = tier <= 2

        // 1. Hook Blur Support Utility (s.a)
        runCatching {
            "com.oplus.wallpapers.rely.utils.s".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "a"; optional() }?.hook {
                    before {
                        if (isTierAOrAPlus) result = true
                    }
                }
            }
        }

        // 2. Hook Device Ability Utility (w)
        runCatching {
            "com.oplus.wallpapers.rely.utils.w".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "b"; optional() }?.hook {
                    before { result = tier }
                }
                firstMethodOrNull { name = "B"; optional() }?.hook {
                    before {
                        if (isTierAOrAPlus) result = true
                    }
                }
                firstMethodOrNull { name = "C"; optional() }?.hook {
                    before {
                        if (isTierAOrAPlus) result = true
                    }
                }
                firstMethodOrNull { name = "f"; optional() }?.hook {
                    before {
                        if (isTierAOrAPlus) result = true
                    }
                }
                firstMethodOrNull { name = "y"; optional() }?.hook {
                    before {
                        if (isTierAOrAPlus) result = false
                    }
                }
                firstMethodOrNull { name = "z"; optional() }?.hook {
                    before {
                        if (isTierAOrAPlus) result = false
                    }
                }
            }
        }
    }
}
