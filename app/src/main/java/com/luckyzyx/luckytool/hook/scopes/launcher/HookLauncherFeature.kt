package com.luckyzyx.luckytool.hook.scopes.launcher

import android.graphics.drawable.Drawable
import android.view.View
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

class HookLauncherFeature(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        loadHooker(HookLauncherDynamicBlur)
    }

    @Obfuscate
    object HookLauncherDynamicBlur : YukiBaseHooker() {
        override fun onHook() {
            val enableBlur = preferences(ModulePrefs).getString("customized_anim_level", "-1") == "1" ||
                preferences(ModulePrefs).getBoolean("force_enable_systemui_blur_feature", false) ||
                preferences(ModulePrefs).getBoolean("force_enable_launcher_high_level", false) ||
                preferences(ModulePrefs).getBoolean("force_enable_docker_background_blur", false) ||
                preferences(ModulePrefs).getBoolean("enable_docker_background", false)
            if (!enableBlur) return

            // 1. 强制 Launcher 平台能力等级判断为最高级旗舰：
            // 避免 isBlurUnavailable 判断为 true 时回退为 popup_no_blur_background 灰色背景
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
                    firstMethodOrNull { name = "getAnimationLevelOS14"; optional() }?.hook {
                        before { result = 1 }
                    }
                    firstMethodOrNull { name = "getGaussianLevel"; optional() }?.hook {
                        before { result = 3 }
                    }
                    firstMethodOrNull { name = "isHighLevelAnimation"; optional() }?.hook {
                        before { result = true }
                    }
                    firstMethodOrNull { name = "isLowLevelAnimation"; optional() }?.hook {
                        before { result = false }
                    }
                    firstMethodOrNull { name = "isMiddleLevelAnimation"; optional() }?.hook {
                        before { result = false }
                    }
                    firstMethodOrNull { name = "isStaticBlurScene"; optional() }?.hook {
                        before { result = false }
                    }
                }
            }

            // 2. 彻底屏蔽 Launcher 的 OplusStaticBlurView 纯灰底色及静态壁纸缓存：
            // 下拉通知栏或负一屏时，保证 Launcher 桌面与图标完整保留，由 SystemUI 动态高斯模糊硬件抓取
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

            // 3. 拦截 Launcher 的 addStaticBlurView，阻止静态遮罩视图挂载
            runCatching {
                "com.android.launcher.Launcher".toClassOrNull()?.resolve()?.apply {
                    firstMethodOrNull { name = "addStaticBlurView"; optional() }?.hook {
                        before {
                            intercept()
                        }
                    }
                }
            }

            // 4. 解锁 Launcher 动画等级与能力开关 (旗舰 Tier-1 等级，驱动模糊与解锁物理动效)
            runCatching {
                "com.android.common.util.LauncherAnimConfig".toClassOrNull()?.resolve()?.apply {
                    firstMethodOrNull { name = "isLevelAOrAPlus"; optional() }?.hook {
                        before { result = true }
                    }
                    firstMethodOrNull { name = "isLevelAPlus"; optional() }?.hook {
                        before { result = true }
                    }
                    firstMethodOrNull { name = "isLevelBPlusOrHigher"; optional() }?.hook {
                        before { result = true }
                    }
                    firstMethodOrNull { name = "isLevelBOrBPlus"; optional() }?.hook {
                        before { result = false }
                    }
                    firstMethodOrNull { name = "isAdaptiveAnimation"; optional() }?.hook {
                        before { result = true }
                    }
                }
            }

            // 5. 解锁 OplusBlurProperties，强制全场景材料模糊可用 (文件夹图标、抽屉背景、Dock等)
            runCatching {
                "com.android.launcher3.uioverrides.states.blurdrawable.OplusBlurProperties".toClassOrNull()
                    ?.resolve()?.apply {
                        firstMethodOrNull { name = "isSupportNewBlur"; optional() }?.hook {
                            before { result = true }
                        }
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
                "com.android.launcher3.uioverrides.states.blurdrawable.OplusBlurProperties\$Companion".toClassOrNull()
                    ?.resolve()?.apply {
                        firstMethodOrNull { name = "isSupportNewBlur"; optional() }?.hook {
                            before { result = true }
                        }
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

            // 6. 解锁桌面文件夹模糊：OplusPreviewBackground 与 FolderIcon 强制启用动态模糊背景
            // 杜绝回退到 LayerNormalDrawable 纯灰底色，阻止回收销毁模糊对象
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

            // 6.1 LayerBlurDrawable 消除默认灰色底图 (mDefaultDrawable) 遮盖模糊
            runCatching {
                "com.android.launcher3.uioverrides.states.blurdrawable.LayerBlurDrawable".toClassOrNull()
                    ?.resolve()?.apply {
                        firstMethodOrNull { name = "setBlurAlpha"; parameterCount = 1; optional() }?.hook {
                            after {
                                runCatching {
                                    val inst = instance ?: return@after
                                    inst.javaClass.getDeclaredField("mDefaultAlpha").apply { isAccessible = true }.set(inst, 0.0f)
                                    inst.javaClass.getDeclaredField("mBlurAlpha").apply { isAccessible = true }.set(inst, 1.0f)
                                    val defDrawable = inst.javaClass.getDeclaredField("mDefaultDrawable").apply { isAccessible = true }.get(inst) as? Drawable
                                    defDrawable?.alpha = 0
                                    val wholeAlpha = (inst.javaClass.getDeclaredField("mWholeAlpha").apply { isAccessible = true }.get(inst) as? Float) ?: 1.0f
                                    val blurDrawable = inst.javaClass.getDeclaredField("mBlurDrawable").apply { isAccessible = true }.get(inst) as? Drawable
                                    blurDrawable?.alpha = (255 * wholeAlpha).toInt()
                                }
                            }
                        }
                        firstMethodOrNull { name = "updateLayerDrawableAlpha"; emptyParameters(); optional() }?.hook {
                            after {
                                runCatching {
                                    val inst = instance ?: return@after
                                    inst.javaClass.getDeclaredField("mDefaultAlpha").apply { isAccessible = true }.set(inst, 0.0f)
                                    val defDrawable = inst.javaClass.getDeclaredField("mDefaultDrawable").apply { isAccessible = true }.get(inst) as? Drawable
                                    defDrawable?.alpha = 0
                                    val wholeAlpha = (inst.javaClass.getDeclaredField("mWholeAlpha").apply { isAccessible = true }.get(inst) as? Float) ?: 1.0f
                                    val blurDrawable = inst.javaClass.getDeclaredField("mBlurDrawable").apply { isAccessible = true }.get(inst) as? Drawable
                                    blurDrawable?.alpha = (255 * wholeAlpha).toInt()
                                }
                            }
                        }
                    }
            }

            // 7. 解锁打开文件夹动效：禁用低端简易动画，启用完整高斯壁纸模糊展开
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

            // 8. 解锁 AppFeatureUtils 动画与模糊特性
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
                    firstMethodOrNull { name = "isSupportDynamicBlur"; optional() }?.hook {
                        before { result = true }
                    }
                    firstMethodOrNull { name = "isMixColorsDisable"; optional() }?.hook {
                        before { result = false }
                    }
                    firstMethodOrNull { name = "getSupportDockerBg"; optional() }?.hook {
                        before { result = true }
                    }
                    firstMethodOrNull { name = "isLightOSAnimation"; optional() }?.hook {
                        before { result = false }
                    }
                }
            }
        }
    }
}
