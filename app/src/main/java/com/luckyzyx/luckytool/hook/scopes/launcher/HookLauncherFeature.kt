package com.luckyzyx.luckytool.hook.scopes.launcher

import android.view.View
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class HookLauncherFeature(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        loadHooker(HookLauncherAnimationTier)
        loadHooker(HookLauncherDynamicBlurShield)
        loadHooker(HookLauncherFolderBlur)
        loadHooker(HookLauncherMotion)
    }

    /**
     * 1. 桌面动画等级与能力开关 (Tier A+ / Tier A / Tier B+ / Tier B)
     */
    @Obfuscate
    object HookLauncherAnimationTier : YukiBaseHooker() {
        override fun onHook() {
            val prefs = preferences(ModulePrefs)
            val launcherAnimTier = prefs.getString("customized_launcher_anim_level", "-1")
            val launcherHigh = prefs.getBoolean("force_enable_launcher_high_level", false)

            val tier: Int = (if (launcherAnimTier != "-1") {
                launcherAnimTier.toIntOrNull()
            } else if (launcherHigh) {
                1
            } else {
                null
            }) ?: return

            // tier: 1=A+, 2=A, 3=B+, 4=B
            val isLevelAOrAPlus = tier <= 2
            val isLevelAPlus = tier == 1
            val isLevelBPlusOrHigher = tier <= 3
            val isLevelBOrBPlus = tier >= 3

            runCatching {
                "com.android.common.util.PlatformLevelUtils".toClassOrNull()?.resolve()?.apply {
                    firstMethodOrNull { name = "getAnimationLevelOS14"; optional() }?.hook {
                        before { result = if (isLevelAOrAPlus) 2 else if (isLevelBPlusOrHigher) 1 else 0 }
                    }
                    firstMethodOrNull { name = "isHighLevelAnimation"; optional() }?.hook {
                        before { result = isLevelAOrAPlus }
                    }
                    firstMethodOrNull { name = "isMiddleLevelAnimation"; optional() }?.hook {
                        before { result = (tier == 3) }
                    }
                    firstMethodOrNull { name = "isLowLevelAnimation"; optional() }?.hook {
                        before { result = (tier >= 4) }
                    }
                }
            }

            runCatching {
                "com.android.common.util.LauncherAnimConfig".toClassOrNull()?.resolve()?.apply {
                    firstMethodOrNull { name = "isLevelAOrAPlus"; optional() }?.hook {
                        before { result = isLevelAOrAPlus }
                    }
                    firstMethodOrNull { name = "isLevelAPlus"; optional() }?.hook {
                        before { result = isLevelAPlus }
                    }
                    firstMethodOrNull { name = "isLevelBPlusOrHigher"; optional() }?.hook {
                        before { result = isLevelBPlusOrHigher }
                    }
                    firstMethodOrNull { name = "isLevelBOrBPlus"; optional() }?.hook {
                        before { result = isLevelBOrBPlus }
                    }
                    firstMethodOrNull { name = "isAdaptiveAnimation"; optional() }?.hook {
                        before { result = true }
                    }
                }
            }
        }
    }

    /**
     * 2. 桌面静态遮罩防护：当下拉通知栏或控制中心时，屏蔽 OplusStaticBlurView 灰底，交由 SystemUI 动态高斯模糊硬件抓取
     */
    @Obfuscate
    object HookLauncherDynamicBlurShield : YukiBaseHooker() {
        override fun onHook() {
            val prefs = preferences(ModulePrefs)
            val sysuiDynamic = prefs.getString("customized_systemui_dynamic_blur", "-1") == "1"
            val shadeBlurMode = prefs.getString("customized_shade_blur_mode", "0") != "0"
            val sysuiBlur = prefs.getBoolean("force_enable_systemui_blur_feature", false)

            if (!sysuiDynamic && !shadeBlurMode && !sysuiBlur) return

            runCatching {
                "com.android.common.util.PlatformLevelUtils".toClassOrNull()?.resolve()?.apply {
                    firstMethodOrNull { name = "isBlurUnavailable"; optional() }?.hook {
                        before { result = false }
                    }
                    firstMethodOrNull { name = "isDynamicBlurAvailable"; optional() }?.hook {
                        before { result = true }
                    }
                    firstMethodOrNull { name = "isDynamicBlurUnavailable"; optional() }?.hook {
                        before { result = false }
                    }
                    firstMethodOrNull { name = "isStaticBlurScene"; optional() }?.hook {
                        before { result = false }
                    }
                }
            }

            runCatching {
                "com.android.launcher.views.OplusStaticBlurView".toClassOrNull()?.resolve()?.apply {
                    firstMethodOrNull { name = "init"; optional() }?.hook {
                        after {
                            runCatching {
                                instance<View>().background = null
                                instance<View>().visibility = View.GONE
                            }
                        }
                    }
                    firstMethodOrNull { name = "setBackground"; optional() }?.hook {
                        before {
                            args[0] = null
                        }
                    }
                    firstMethodOrNull { name = "onBlurBitmapChanged"; optional() }?.hook {
                        before {
                            intercept()
                        }
                    }
                    firstMethodOrNull { name = "setAlpha"; optional() }?.hook {
                        before {
                            intercept()
                        }
                    }
                }
            }

            runCatching {
                "com.android.launcher.Launcher".toClassOrNull()?.resolve()?.apply {
                    firstMethodOrNull { name = "addStaticBlurView"; optional() }?.hook {
                        before {
                            intercept()
                        }
                    }
                }
            }
        }
    }

    /**
     * 3. 桌面文件夹与材质动态模糊 (OplusBlurProperties / FolderIcon / FolderAnimUtil)
     */
    @Obfuscate
    object HookLauncherFolderBlur : YukiBaseHooker() {
        override fun onHook() {
            val prefs = preferences(ModulePrefs)
            val folderBlur = prefs.getBoolean("enable_folder_material_blur", false)
            val dockerBlur = prefs.getBoolean("force_enable_docker_background_blur", false)

            if (!folderBlur && !dockerBlur) return

            // 基础模糊能力：如果开启了文件夹或 Docker 模糊，OplusBlurProperties.isSupportNewBlur 必须返回 true
            runCatching {
                "com.android.launcher3.uioverrides.states.blurdrawable.OplusBlurProperties".toClassOrNull()
                    ?.resolve()?.apply {
                        firstMethodOrNull { name = "isSupportNewBlur"; optional() }?.hook {
                            before { result = true }
                        }
                        if (folderBlur) {
                            firstMethodOrNull { name = "getSysMaterialBlurSwitchEnable"; optional() }?.hook {
                                before { result = true }
                            }
                            firstMethodOrNull { name = "isMaterialBlurDrawSupported"; optional() }?.hook {
                                before { result = true }
                            }
                            firstMethodOrNull { name = "isMaterialBlurJumpSupported"; optional() }?.hook {
                                before { result = true }
                            }
                            firstMethodOrNull { name = "isSupportDrawerBlur"; optional() }?.hook {
                                before { result = true }
                            }
                            firstMethodOrNull { name = "isSupportDrawerBtnBlur"; optional() }?.hook {
                                before { result = true }
                            }
                        }
                    }
                "com.android.launcher3.uioverrides.states.blurdrawable.OplusBlurProperties\$Companion".toClassOrNull()
                    ?.resolve()?.apply {
                        firstMethodOrNull { name = "isSupportNewBlur"; optional() }?.hook {
                            before { result = true }
                        }
                        if (folderBlur) {
                            firstMethodOrNull { name = "getSysMaterialBlurSwitchEnable"; optional() }?.hook {
                                before { result = true }
                            }
                            firstMethodOrNull { name = "isMaterialBlurDrawSupported"; optional() }?.hook {
                                before { result = true }
                            }
                            firstMethodOrNull { name = "isMaterialBlurJumpSupported"; optional() }?.hook {
                                before { result = true }
                            }
                            firstMethodOrNull { name = "isSupportDrawerBlur"; optional() }?.hook {
                                before { result = true }
                            }
                            firstMethodOrNull { name = "isSupportDrawerBtnBlur"; optional() }?.hook {
                                before { result = true }
                            }
                        }
                    }
            }

            if (!folderBlur) return

            // 文件夹图标与背景动态模糊
            runCatching {
                "com.android.launcher3.folder.OplusPreviewBackground".toClassOrNull()?.resolve()?.apply {
                    firstMethodOrNull { name = "isBgSupportNewBlur"; optional() }?.hook {
                        before { result = true }
                    }
                    firstMethodOrNull { name = "isSupportNewBlurLocal"; optional() }?.hook {
                        before { result = true }
                    }
                    firstMethodOrNull { name = "recycleBlurBackground"; optional() }?.hook {
                        before { intercept() }
                    }
                }
                "com.android.launcher3.folder.PreviewBackground".toClassOrNull()?.resolve()?.apply {
                    firstMethodOrNull { name = "isSupportNewBlurLocal"; optional() }?.hook {
                        before { result = true }
                    }
                    firstMethodOrNull { name = "recycleBlurBackground"; optional() }?.hook {
                        before { intercept() }
                    }
                }
                "com.android.launcher3.folder.FolderIcon".toClassOrNull()?.resolve()?.apply {
                    firstMethodOrNull { name = "isSupportNewBlurLocal"; optional() }?.hook {
                        before { result = true }
                    }
                }
            }

            // 文件夹动效（关闭简易/低端动画）
            runCatching {
                "com.android.launcher3.anim.light.FolderAnimUtil".toClassOrNull()?.resolve()?.apply {
                    firstMethodOrNull { name = "isLightFolderAnim"; optional() }?.hook {
                        before { result = false }
                    }
                    firstMethodOrNull { name = "isLightOrSuperLightFolderAnim"; optional() }?.hook {
                        before { result = false }
                    }
                    firstMethodOrNull { name = "isSuperLightFolderAnim"; optional() }?.hook {
                        before { result = false }
                    }
                }
            }

            runCatching {
                "com.android.common.util.AppFeatureUtils".toClassOrNull()?.resolve()?.apply {
                    firstMethodOrNull { name = "isLightFolderAnimation"; optional() }?.hook {
                        before { result = false }
                    }
                    firstMethodOrNull { name = "isFolderBlurDisabled"; optional() }?.hook {
                        before { result = false }
                    }
                    firstMethodOrNull { name = "isUpgradeMaterialBlur"; optional() }?.hook {
                        before { result = true }
                    }
                }
            }
        }
    }

    /**
     * 4. 桌面动效特性与内存限制解除
     */
    @Obfuscate
    object HookLauncherMotion : YukiBaseHooker() {
        override fun onHook() {
            val prefs = preferences(ModulePrefs)
            val disableLowMem = prefs.getBoolean("disable_launcher_low_mem_limits", false)
            val dockerBlur = prefs.getBoolean("force_enable_docker_background_blur", false)
            val folderBlur = prefs.getBoolean("enable_folder_material_blur", false)
            val launcherHigh = prefs.getBoolean("force_enable_launcher_high_level", false)

            runCatching {
                "com.android.common.util.AppFeatureUtils".toClassOrNull()?.resolve()?.apply {
                    if (disableLowMem || launcherHigh) {
                        firstMethodOrNull { name = "isLightOSAnimation"; optional() }?.hook {
                            before { result = false }
                        }
                        firstMethodOrNull { name = "isMixColorsDisable"; optional() }?.hook {
                            before { result = false }
                        }
                    }
                    if (dockerBlur || folderBlur || launcherHigh) {
                        firstMethodOrNull { name = "isSupportDynamicBlur"; optional() }?.hook {
                            before { result = true }
                        }
                    }
                }
            }
        }
    }
}
