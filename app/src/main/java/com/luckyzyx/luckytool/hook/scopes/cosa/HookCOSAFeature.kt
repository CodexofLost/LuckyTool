package com.luckyzyx.luckytool.hook.scopes.cosa

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class HookCOSAFeature(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val prefs = preferences(ModulePrefs)
        val launcherAnimTier = prefs.getString("customized_launcher_anim_level", "-1")
        val launcherHigh = prefs.getBoolean("force_enable_launcher_high_level", false)
        val isTierAOrAPlus = (launcherAnimTier in arrayOf("1", "2")) || (launcherAnimTier == "-1" && launcherHigh)

        if (!isTierAOrAPlus) return

        // 1. Hook OplusFeatureConfigManager in COSA process
        runCatching {
            "com.oplus.content.OplusFeatureConfigManager".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "hasFeature"; optional() }?.hook {
                    before {
                        val key = args[0] as? String ?: return@before
                        when (key) {
                            "oplus.software.display.game.memc_enable",
                            "oplus.software.display.game.support.extreme_insert",
                            "oplus.software.display.game.memc_increase_fps_mode",
                            "oplus.software.display.game.sr_enable",
                            "oplus.software.display.game.sr.fully_enable",
                            "oplus.software.display.game.hdr2.0_enable",
                            "oplus.software.display.game.hdr3.0_enable",
                            "oplus.software.display.game_color_plus_iris_sdr2hdr_support",
                            "oplus.software.support.gt.mode",
                            "oplus.software.game.geek.performance.panel.min.type",
                            "oplus.software.game_lightningstart_preload" -> result = true
                            "oplus.software.game.cosa_light_selfkill_enable" -> result = false
                        }
                    }
                }
            }
        }

        // 2. Hook Utils (nb.p.l) to prevent light-selfkilling
        runCatching {
            "nb.p".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "l"; optional() }?.hook {
                    before { result = false }
                }
            }
        }

        // 3. Fallback via DexKit for selfkill check in COSA
        runCatching {
            val dynamicClass = dexKitBridge.findClass {
                matcher {
                    usingStrings("oplus.software.game.cosa_light_selfkill_enable")
                }
            }.firstOrNull()?.name?.toClassOrNull()
            dynamicClass?.resolve()?.apply {
                firstMethodOrNull { name = "l"; optional() }?.hook {
                    before { result = false }
                }
            }
        }
    }
}
