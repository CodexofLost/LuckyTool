package com.luckyzyx.luckytool.hook.scopes.launcher

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceEnableDockerBackgroundBlur : YukiBaseHooker() {
    override fun onHook() {
        // 1. OplusHotseat: 强制调用原生 createBlurDrawable() 设置 Dock 模糊背景，杜绝回退到普通纯灰底图
        "com.android.launcher3.OplusHotseat".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "setDockerBackground"; emptyParameters(); optional() }?.hook {
                replaceUnit {
                    runCatching {
                        val inst = instance ?: return@replaceUnit
                        val saw = runCatching {
                            inst.javaClass.getMethod("getShortcutsAndWidgets").invoke(inst) as? ViewGroup
                        }.getOrNull() ?: runCatching {
                            var clazz: Class<*>? = inst.javaClass
                            var field: java.lang.reflect.Field? = null
                            while (clazz != null && field == null) {
                                field = runCatching { clazz!!.getDeclaredField("mShortcutsAndWidgets").apply { isAccessible = true } }.getOrNull()
                                clazz = clazz.superclass
                            }
                            field?.get(inst) as? ViewGroup
                        }.getOrNull() ?: return@replaceUnit

                        if (saw.childCount == 0) {
                            saw.setBackgroundResource(0)
                            return@replaceUnit
                        }
                        val createBlurMethod = inst.javaClass.getDeclaredMethod("createBlurDrawable").apply { isAccessible = true }
                        val blurDrawable = createBlurMethod.invoke(inst) as? Drawable
                        if (blurDrawable != null) {
                            saw.background = blurDrawable
                        }
                        val bgVisible = runCatching {
                            val bgVisibleField = inst.javaClass.getDeclaredField("mBackgroundVisible").apply { isAccessible = true }
                            bgVisibleField.get(inst) as? Boolean
                        }.getOrNull() ?: true
                        runCatching {
                            val setBgAlphaMethod = inst.javaClass.getMethod("setBackgroundAlpha", Float::class.javaPrimitiveType)
                            setBgAlphaMethod.invoke(inst, if (bgVisible) 1.0f else 0.0f)
                        }
                    }
                }
            }

            firstMethodOrNull { name = "updateDockerBgIfNecessary"; optional() }?.hook {
                replaceUnit {
                    runCatching {
                        val inst = instance ?: return@replaceUnit
                        val saw = runCatching {
                            inst.javaClass.getMethod("getShortcutsAndWidgets").invoke(inst) as? ViewGroup
                        }.getOrNull() ?: return@replaceUnit
                        if (saw.background == null && saw.childCount > 0) {
                            val setDockerBg = inst.javaClass.getMethod("setDockerBackground")
                            setDockerBg.invoke(inst)
                        }
                    }
                }
            }

            firstMethodOrNull { name = "recycleBlurBackground"; optional() }?.hook {
                before {
                    intercept()
                }
            }

            firstMethodOrNull { name = "onDraw"; optional() }?.hook {
                before {
                    runCatching {
                        val inst = instance ?: return@before
                        val saw = runCatching {
                            inst.javaClass.getMethod("getShortcutsAndWidgets").invoke(inst) as? ViewGroup
                        }.getOrNull() ?: return@before
                        if (saw.background == null && saw.childCount > 0) {
                            val setDockerBg = inst.javaClass.getMethod("setDockerBackground")
                            setDockerBg.invoke(inst)
                        }
                    }
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
            firstMethodOrNull { name = "getSupportDockerBg"; emptyParameters(); optional() }?.hook {
                before {
                    result = true
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

        // 8. ScreenUtils: 支持 Docker 扩展屏特性与大屏特性
        "com.android.common.util.ScreenUtils".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "isSupportDockerExpandScreen"; emptyParameters(); optional() }?.hook {
                before {
                    result = true
                }
            }
            firstMethodOrNull { name = "isSupportDockerExpandScreenByActivity"; optional() }?.hook {
                before {
                    result = true
                }
            }
            firstMethodOrNull { name = "hasLargeDisplayFeatures"; optional() }?.hook {
                before {
                    result = true
                }
            }
        }

        // 9. Realme 品牌 Docker 背景开关支持与假玻璃拦截
        "com.android.launcher.custom.LiquidGlassImpl".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "drawDockerBack"; optional() }?.hook {
                before {
                    intercept()
                }
            }
            firstMethodOrNull { name = "isSupportDockerBg"; optional() }?.hook {
                before {
                    result = true
                }
            }
        }
        "com.android.launcher.custom.OplusToolUtils".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "drawDockerBack"; optional() }?.hook {
                before {
                    intercept()
                }
            }
            firstMethodOrNull { name = "isSupportDockerBg"; optional() }?.hook {
                before {
                    result = true
                }
            }
        }
    }
}

