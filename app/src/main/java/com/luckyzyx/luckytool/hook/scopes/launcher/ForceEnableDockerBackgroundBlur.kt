package com.luckyzyx.luckytool.hook.scopes.launcher

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceEnableDockerBackgroundBlur : YukiBaseHooker() {
    override fun onHook() {
        // 1. OplusHotseat: 阻止回收销毁模糊材质属性，防止关闭文件夹或返回桌面时背景销毁
        "com.android.launcher3.OplusHotseat".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "recycleBlurBackground"; optional() }?.hook {
                before {
                    intercept()
                }
            }
        }

        // 2. ExpandUtils: 兜底拦截普通 Dock 背景，防止任何途径返回 hotseat_bg 灰色底图
        "com.android.launcher3.hotseat.expand.ExpandUtils".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "getHotseatNormalBgDrawable"; optional() }?.hook {
                before {
                    result = null
                }
            }
        }

        // 3. LayerBlurDrawable: 彻底消除默认灰色底图 (mDefaultDrawable) 遮挡模糊层的问题
        // 当状态切换或文件夹打开时 setBlurAlpha(0.0f) 会将 mDefaultAlpha 设为 1.0f，导致纯灰底色完全遮盖模糊
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

        "com.android.launcher3.uioverrides.states.blurdrawable.LayerBlurDrawable\$Companion".toClassOrNull()
            ?.resolve()?.apply {
                firstMethodOrNull { name = "createDefaultDrawable"; optional() }?.hook {
                    after {
                        (result as? GradientDrawable)?.apply {
                            setColor(Color.TRANSPARENT)
                            alpha = 0
                        }
                    }
                }
            }

        // 4. OplusBlurProperties: 强制全局材质模糊全开 (桌面文件夹、Dock、抽屉背景等)
        "com.android.launcher3.uioverrides.states.blurdrawable.OplusBlurProperties".toClassOrNull()
            ?.resolve()?.apply {
                firstMethodOrNull { name = "isSupportNewBlur"; optional() }?.hook {
                    before {
                        result = true
                    }
                }
                firstMethodOrNull { name = "getSysMaterialBlurSwitchEnable"; optional() }?.hook {
                    before {
                        result = true
                    }
                }
                firstMethodOrNull { name = "isMaterialBlurDrawSupported"; optional() }?.hook {
                    before {
                        result = true
                    }
                }
                firstMethodOrNull { name = "isMaterialBlurJumpSupported"; optional() }?.hook {
                    before {
                        result = true
                    }
                }
            }

        "com.android.launcher3.uioverrides.states.blurdrawable.OplusBlurProperties\$Companion".toClassOrNull()
            ?.resolve()?.apply {
                firstMethodOrNull { name = "isSupportNewBlur"; optional() }?.hook {
                    before {
                        result = true
                    }
                }
                firstMethodOrNull { name = "getSysMaterialBlurSwitchEnable"; optional() }?.hook {
                    before {
                        result = true
                    }
                }
                firstMethodOrNull { name = "isMaterialBlurDrawSupported"; optional() }?.hook {
                    before {
                        result = true
                    }
                }
                firstMethodOrNull { name = "isMaterialBlurJumpSupported"; optional() }?.hook {
                    before {
                        result = true
                    }
                }
                firstMethodOrNull { name = "pauseBlurService"; optional() }?.hook {
                    before {
                        val pause = args.getOrNull(1) as? Boolean
                        if (pause == true) {
                            intercept()
                        }
                    }
                }
            }

        // 5. FolderIcon & OplusPreviewBackground: 文件夹图标与背景永久保持模糊，阻止回收销毁
        "com.android.launcher3.folder.FolderIcon".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "isSupportNewBlurLocal"; optional() }?.hook {
                before {
                    result = true
                }
            }
        }

        "com.android.launcher3.folder.OplusPreviewBackground".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "isBgSupportNewBlur"; optional() }?.hook {
                before {
                    result = true
                }
            }
            firstMethodOrNull { name = "isSupportNewBlurLocal"; optional() }?.hook {
                before {
                    result = true
                }
            }
            firstMethodOrNull { name = "recycleBlurBackground"; optional() }?.hook {
                before {
                    intercept()
                }
            }
        }

        "com.android.launcher3.folder.PreviewBackground".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "isSupportNewBlurLocal"; optional() }?.hook {
                before {
                    result = true
                }
            }
            firstMethodOrNull { name = "recycleBlurBackground"; optional() }?.hook {
                before {
                    intercept()
                }
            }
        }

        // 6. PlatformLevelUtils: 锁定最高平台等级 (避免 isBlurUnavailable 判断为 true 时回退灰色背景)
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
            firstMethodOrNull { name = "isHighLevelAnimation"; optional() }?.hook {
                before { result = true }
            }
            firstMethodOrNull { name = "isLowLevelAnimation"; optional() }?.hook {
                before { result = false }
            }
            firstMethodOrNull { name = "getAnimationLevelOS14"; optional() }?.hook {
                before { result = 1 }
            }
            firstMethodOrNull { name = "getGaussianLevel"; optional() }?.hook {
                before { result = 3 }
            }
        }

        // 7. AppFeatureUtils: 解除 LightOS 动画限制并解锁模糊特性
        "com.android.common.util.AppFeatureUtils".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "isLightOSAnimation"; emptyParameters(); optional() }?.hook {
                before {
                    result = false
                }
            }
            firstMethodOrNull { name = "getAllowLauncherBlur"; optional() }?.hook {
                before {
                    result = true
                }
            }
            firstMethodOrNull { name = "isSupportDynamicBlur"; optional() }?.hook {
                before {
                    result = true
                }
            }
            firstMethodOrNull { name = "isUpgradeMaterialBlur"; optional() }?.hook {
                before {
                    result = true
                }
            }
            firstMethodOrNull { name = "isLowGaussianBlur"; optional() }?.hook {
                before {
                    result = false
                }
            }
            firstMethodOrNull { name = "isAllAppsDisableBlur"; optional() }?.hook {
                before {
                    result = false
                }
            }
            firstMethodOrNull { name = "isRecentsDisableBlur"; optional() }?.hook {
                before {
                    result = false
                }
            }
            firstMethodOrNull { name = "isDisableWidgetBlurBackground"; optional() }?.hook {
                before {
                    result = false
                }
            }
        }
    }
}

