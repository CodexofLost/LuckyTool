package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
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

        val panoramicAod = preferences(ModulePrefs).getBoolean("enable_panoramic_aod", false)
        if (panoramicAod) {
            // 1. Hook SmoothTransitionController in com.android.systemui
            val hookSmoothController = { clazz: Class<*>? ->
                clazz?.resolve()?.apply {
                    firstMethodOrNull { name = "getDisplayPanelFeatureHidl"; optional() }?.hook {
                        before {
                            result = intArrayOf(15) // Bits: 1 (powerOn), 2 (smooth), 4 (panoramic), 8 (allDay)
                        }
                    }
                    firstMethodOrNull { name = "init"; optional() }?.hook {
                        after {
                            instance?.let { target ->
                                runCatching { target.javaClass.getDeclaredField("isSupportPanoramic").apply { isAccessible = true }.set(target, true) }
                                runCatching { target.javaClass.getDeclaredField("isSupportPanoramicByPanelFeature").apply { isAccessible = true }.set(target, true) }
                                runCatching { target.javaClass.getDeclaredField("isSupportPanoramicAllDay").apply { isAccessible = true }.set(target, true) }
                                runCatching { target.javaClass.getDeclaredField("isSupportPanoramicAllDayByPanelFeature").apply { isAccessible = true }.set(target, true) }
                                runCatching { target.javaClass.getDeclaredField("isSupportSmoothTransition").apply { isAccessible = true }.set(target, true) }
                                runCatching {
                                    target.javaClass.getDeclaredMethod("setPanoramicStatusForApplication", Boolean::class.javaPrimitiveType).apply { isAccessible = true }.invoke(target, true)
                                }
                                runCatching {
                                    target.javaClass.getDeclaredMethod("setPanoramicSupportAllDayForApplication", Boolean::class.javaPrimitiveType).apply { isAccessible = true }.invoke(target, true)
                                }
                            }
                        }
                    }
                }
            }

            runCatching {
                hookSmoothController("com.oplus.systemui.aod.display.SmoothTransitionController".toClassOrNull())
            }

            runCatching {
                val dynamicSmooth = dexKitBridge.findClass {
                    matcher {
                        usingStrings("oplus_keyguard_panoramic_aod_support")
                    }
                }.firstOrNull()?.name?.toClassOrNull()
                hookSmoothController(dynamicSmooth)
            }

            // 2. Hook AodFeatureOption in com.android.systemui
            val hookAodFeatureOption = { clazz: Class<*>? ->
                clazz?.resolve()?.apply {
                    firstMethodOrNull { name = "isSupportRamLessAod"; optional() }?.hook {
                        before { result = false }
                    }
                    firstMethodOrNull { name = "isSupportAod"; optional() }?.hook {
                        before { result = true }
                    }
                    firstMethodOrNull { name = "isSupportLTPO1HzAOD"; optional() }?.hook {
                        before { result = true }
                    }
                }
            }

            runCatching {
                hookAodFeatureOption("com.oplusos.systemui.common.feature.AodFeatureOption".toClassOrNull())
            }

            runCatching {
                val dynamicAod = dexKitBridge.findClass {
                    matcher {
                        usingStrings("oplus.software.display.aod_ramless_support")
                    }
                }.firstOrNull()?.name?.toClassOrNull()
                hookAodFeatureOption(dynamicAod)
            }

            // 3. Hook KeyguardFeatureOption in com.android.systemui
            val hookKeyguardFeatureOption = { clazz: Class<*>? ->
                clazz?.resolve()?.apply {
                    firstMethodOrNull { name = "isLihgtOsLowLevel"; optional() }?.hook {
                        before { result = false }
                    }
                }
            }

            runCatching {
                hookKeyguardFeatureOption("com.oplusos.systemui.common.feature.KeyguardFeatureOption".toClassOrNull())
            }

            runCatching {
                val dynamicKeyguard = dexKitBridge.findClass {
                    matcher {
                        usingStrings("com.android.systemui.kg_light_os_level")
                    }
                }.firstOrNull()?.name?.toClassOrNull()
                hookKeyguardFeatureOption(dynamicKeyguard)
            }
        }
    }
}
