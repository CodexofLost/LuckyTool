package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.hookAll
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.lsparanoid.Obfuscate

/**
 * 强制启用 SystemUI Tier-3 旗舰级动态背景高斯模糊。
 *
 * 核心逻辑与修复说明：
 * 1. 消除 B/BP 级降级分支阻断：
 *    - isBAnimLevel / isBPAnimLevel 返回 false。
 *    - 绕过 PlatformBlurManagerImpl 对 isBAnimLevel 的跳过判断，直接调用 Blur.resumeWindowBlur(context)
 *      与 Blur.setBlurFrequency(context, 120)，主动唤醒 com.oplus.blur 进程生成硬件模糊流。
 * 2. 锁定高斯模糊与动画能力为旗舰最高级：
 *    - isHighGaussianLevel = true, getAnimLevel = 1, getPlatformGaussianLevel = 3, getPlatformAnimationLevel = 1。
 * 3. 动态模糊管线与超时防护：
 *    - 拦截 PlatformBlurManagerImpl.useMotionBlur() 固定为 true。
 *    - 拦截 PlatformBlurManagerImpl.handleTimeoutPauseBlur() 阻止超时自动暂停动态模糊。
 *    - 拦截 MaterialBlurStateManager.enableMaterialBlur() 固定为 true。
 *    - 拦截 pauseBlur("unsupportBlur") 与 pauseBlur("should not blur")，使用 intercept() 阻止暂停。
 * 4. 强制 scrim_behind 使用 AutoBlurDrawable (ContinuousBlurDrawable)：
 *    - 拦截 ScrimControllerExImp.refreshBehindDrawable()，杜绝降级回退为 WallpaperBlurDrawable。
 *    - 拦截 ScrimControllerExImp.adjudgeScrimBehindBlurType()，锁定 BlurTypePlatformStatic。
 * 5. 消除解锁状态下下拉通知/快捷设置时的纯灰底色遮罩：
 *    - 拦截 ScrimControllerExImp.getNotificationScrimBgColor()、ScrimState.getBehindTint()、
 *      以及 ScrimViewExImp.updateColorWithTint()，确保底色遮罩固定为 0（透明）。
 */
@Obfuscate
object ForceEnableSystemUIDynamicBlur : YukiBaseHooker() {

    override fun onHook() {
        // 1. Android Framework Window Blur 基础支持
        runCatching {
            "android.view.CrossWindowBlurListeners".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "isCrossWindowBlurEnabled"; optional() }?.hook {
                    before { result = true }
                }
            }
        }

        runCatching {
            "android.app.ActivityManager".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "isHighEndGfx"; optional() }?.hook {
                    before { result = true }
                }
            }
        }

        // 2. SystemUI BlurUtils 窗口模糊能力检查
        runCatching {
            "com.android.systemui.statusbar.BlurUtils".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "supportsBlursOnWindowsBase"; optional() }?.hook {
                    before { result = true }
                }
                firstMethodOrNull { name = "supportsBlursOnWindows"; optional() }?.hook {
                    before { result = true }
                }
            }
        }

        runCatching {
            "com.oplusos.systemui.common.blurability.motion.BlurUtils\$Companion".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "supportsBlursOnWindows"; optional() }?.hook {
                    before { result = true }
                }
            }
        }

        // 3. ColorOS 高斯模糊总闸（GaussBlurUtils）
        runCatching {
            "com.oplus.systemui.blur.GaussBlurUtils".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "isGaussBlurDisabled"; optional() }?.hook {
                    before { result = false }
                }
                firstMethodOrNull { name = "isQSGaussBlurDisabled"; optional() }?.hook {
                    before { result = false }
                }
            }
        }

        // 4. ColorOS 特性配置静态字段修正（NotificationFeatureOption & KeyguardFeatureOption）
        runCatching {
            "com.oplusos.systemui.common.feature.NotificationFeatureOption".toClassOrNull()?.resolve()?.apply {
                firstFieldOrNull { name = "isGaussBlurDisabled"; optional() }?.set(false)
                firstFieldOrNull { name = "isPanViewBlurDisabled"; optional() }?.set(false)
                firstFieldOrNull { name = "isWallpaperBlurDisabled"; optional() }?.set(false)
                firstMethodOrNull { name = "loadAppFeature"; optional() }?.hook {
                    after {
                        firstFieldOrNull { name = "isGaussBlurDisabled"; optional() }?.set(false)
                        firstFieldOrNull { name = "isPanViewBlurDisabled"; optional() }?.set(false)
                        firstFieldOrNull { name = "isWallpaperBlurDisabled"; optional() }?.set(false)
                    }
                }
            }
        }

        runCatching {
            "com.oplusos.systemui.common.feature.KeyguardFeatureOption".toClassOrNull()?.resolve()?.apply {
                firstFieldOrNull { name = "isDisableBouncerBlurScrim"; optional() }?.set(false)
                firstFieldOrNull { name = "isEnableBouncerWallpaperBlur"; optional() }?.set(false)
                firstMethodOrNull { name = "loadAppFeature"; optional() }?.hook {
                    after {
                        firstFieldOrNull { name = "isDisableBouncerBlurScrim"; optional() }?.set(false)
                        firstFieldOrNull { name = "isEnableBouncerWallpaperBlur"; optional() }?.set(false)
                    }
                }
            }
        }

        // 5. ColorOS Scrim 控制器等级判断（ScrimUtil）
        runCatching {
            "com.oplusos.systemui.common.util.ScrimUtil".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "isLowGaussianLevel"; optional() }?.hook {
                    before { result = false }
                }
                firstMethodOrNull { name = "isLightOsGaussianLevel"; optional() }?.hook {
                    before { result = false }
                }
                firstMethodOrNull { name = "isLowGaussianLevelByRam"; optional() }?.hook {
                    before { result = false }
                }
                firstMethodOrNull { name = "isWallpaperBlurDisable"; optional() }?.hook {
                    before { result = false }
                }
                firstMethodOrNull { name = "isHighGaussianLevel"; optional() }?.hook {
                    before { result = true }
                }
                firstMethodOrNull { name = "isBAnimLevel"; optional() }?.hook {
                    before { result = false }
                }
                firstMethodOrNull { name = "isBPAnimLevel"; optional() }?.hook {
                    before { result = false }
                }
                firstMethodOrNull { name = "isMiddleOrHighAnimLevel"; optional() }?.hook {
                    before { result = true }
                }
                firstMethodOrNull { name = "getAnimLevel"; optional() }?.hook {
                    before { result = 1 }
                }
            }
        }

        // 6. ColorOS 平台能力等级（OplusPlatformLevelUtils）
        runCatching {
            "com.oplus.util.OplusPlatformLevelUtils".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "getPlatformGaussianLevel"; optional() }?.hook {
                    before { result = 3 }
                }
                firstMethodOrNull { name = "getPlatformAnimationLevel"; optional() }?.hook {
                    before { result = 1 }
                }
                firstMethodOrNull { name = "getPlatformLevel"; optional() }?.hook {
                    before { result = 3 }
                }
                firstMethodOrNull { name = "isLowPlatform"; optional() }?.hook {
                    before { result = false }
                }
            }
        }

        // 7. ColorOS 质感与动态模糊状态管理器（PlatformBlurManagerImpl & MaterialBlurStateManager）
        runCatching {
            "com.oplus.systemui.panelanimation.platformblur.PlatformBlurManagerImpl".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "useMotionBlur"; optional() }?.hook {
                    before { result = true }
                }
                firstMethodOrNull { name = "forceStopMotionBlur"; optional() }?.hook {
                    before { result = false }
                }
                firstMethodOrNull { name = "handleTimeoutPauseBlur"; optional() }?.hook {
                    before {
                        if (args.isNotEmpty()) {
                            args[0] = false
                        }
                    }
                }
                // 强制调用 Blur.resumeWindowBlur(context)，彻底穿透 isBAnimLevel 限制
                firstMethodOrNull {
                    name = "resumeBlur"
                    parameterCount = 1
                    optional()
                }?.hook {
                    before {
                        val ctx = instance.asResolver().firstFieldOrNull { name = "context"; optional() }?.get<Context>()
                        if (ctx != null) {
                            runCatching {
                                "com.oplus.posteffect.Blur".toClassOrNull()?.resolve()
                                    ?.firstMethodOrNull { name = "resumeWindowBlur"; optional() }
                                    ?.invoke<Unit>(null, ctx)
                            }
                        }
                    }
                }
                // 强制调用 Blur.setBlurFrequency(context, blurFrequency)，设置为 120Hz 高频动态模糊
                firstMethodOrNull {
                    name = "setPlatformBlurMode"
                    parameterCount = 1
                    optional()
                }?.hook {
                    before {
                        val ctx = instance.asResolver().firstFieldOrNull { name = "context"; optional() }?.get<Context>()
                        if (ctx != null) {
                            runCatching {
                                "com.oplus.posteffect.Blur".toClassOrNull()?.resolve()
                                    ?.firstMethodOrNull { name = "setBlurFrequency"; optional() }
                                    ?.invoke<Unit>(null, ctx.applicationContext, 120)
                            }
                        }
                    }
                }
                // 拦截通知栏/快捷设置下拉过程中的强制 pauseBlur 调用
                firstMethodOrNull {
                    name = "pauseBlur"
                    parameterCount = 1
                    optional()
                }?.hook {
                    before {
                        val reason = args.getOrNull(0) as? String ?: ""
                        if (reason.contains("should not blur") || reason.contains("unsupportBlur")) {
                            intercept()
                            return@before
                        }
                        val isExpanding = runCatching {
                            instance.asResolver().firstFieldOrNull { name = "isExpanding"; optional() }?.get<Boolean>()
                        }.getOrNull() == true

                        if (isExpanding) {
                            intercept()
                        }
                    }
                }
                // 面板展开时持续维持 resumeBlur 唤醒状态
                firstMethodOrNull {
                    name = "updatePlatformBlurState"
                    parameterCount = 1
                    optional()
                }?.hook {
                    after {
                        val scrimController = runCatching {
                            instance.asResolver().firstMethodOrNull { name = "getScrimController"; optional() }?.invoke<Any>()
                                ?.asResolver()?.firstMethodOrNull { name = "get"; optional() }?.invoke<Any>()
                        }.getOrNull()
                        val panelFraction = runCatching {
                            scrimController?.asResolver()?.firstMethodOrNull { name = "getPanelExpansionFraction"; optional() }?.invoke<Float>()
                        }.getOrNull() ?: 0f
                        val qsExpansion = runCatching {
                            scrimController?.asResolver()?.firstMethodOrNull { name = "getQsExpansion"; optional() }?.invoke<Float>()
                        }.getOrNull() ?: 0f
                        if (panelFraction > 0.001f || qsExpansion > 0.001f) {
                            val ctx = instance.asResolver().firstFieldOrNull { name = "context"; optional() }?.get<Context>()
                            if (ctx != null) {
                                runCatching {
                                    "com.oplus.posteffect.Blur".toClassOrNull()?.resolve()
                                        ?.firstMethodOrNull { name = "resumeWindowBlur"; optional() }
                                        ?.invoke<Unit>(null, ctx)
                                }
                            }
                        }
                    }
                }
            }
        }

        runCatching {
            "com.oplus.systemui.panelanimation.platformblur.MaterialBlurStateManager".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "enableMaterialBlur"; optional() }?.hook {
                    before { result = true }
                }
            }
        }

        // 8. 系统设置中的质感模糊与窗口模糊开关拦截
        hookSettingGetInt("android.provider.Settings\$System", "system_material_blur_enable", 1)
        hookSettingGetInt("android.provider.Settings\$Global", "disable_window_blurs", 0)

        // 9. 强制 scrim_behind 使用 AutoBlurDrawable 与 BlurTypePlatformStatic
        runCatching {
            "com.oplus.systemui.statusbar.phone.ScrimControllerExImp".toClassOrNull()?.resolve()?.apply {
                // 确保 scrimBehind 使用 AutoBlurDrawable，避免持有旧的 WallpaperBlurDrawable
                firstMethodOrNull { name = "refreshBehindDrawable"; optional() }?.hook {
                    before {
                        runCatching {
                            val scrimController = instance.asResolver().firstMethodOrNull { name = "getScrimController"; optional() }?.invoke<Any>()
                            val scrimBehind = scrimController?.asResolver()?.firstMethodOrNull { name = "getScrimBehind"; optional() }?.invoke<View>() ?: return@runCatching
                            val drawable = (scrimBehind as? ImageView)?.drawable ?: scrimBehind.asResolver().firstMethodOrNull { name = "getDrawable"; optional() }?.invoke<Drawable>()
                            if (drawable != null && drawable.javaClass.name.contains("WallpaperBlurDrawable")) {
                                (scrimBehind as? ImageView)?.setImageDrawable(null)
                                scrimBehind.asResolver().firstMethodOrNull { name = "setDrawable"; optional() }?.invoke<Unit>(null)
                            }
                        }
                    }
                }
                // 确保 scrimBehind 锁定为 BlurTypePlatformStatic
                firstMethodOrNull { name = "adjudgeScrimBehindBlurType"; optional() }?.hook {
                    after {
                        runCatching {
                            val scrimController = instance.asResolver().firstMethodOrNull { name = "getScrimController"; optional() }?.invoke<Any>()
                            val scrimBehind = scrimController?.asResolver()?.firstMethodOrNull { name = "getScrimBehind"; optional() }?.invoke<Any>()
                            val ext = scrimBehind?.asResolver()?.firstMethodOrNull { name = "getExt"; optional() }?.invoke<Any>()
                            val viewBlurProxy = ext?.asResolver()?.firstMethodOrNull { name = "getViewBlurProxy"; optional() }?.invoke<Any>()
                            val platformStaticType = "com.oplusos.systemui.common.blurability.ViewBlurProxy\$BlurType\$BlurTypePlatformStatic".toClassOrNull()
                                ?.resolve()?.firstFieldOrNull { name = "INSTANCE"; optional() }?.get<Any>()
                            if (viewBlurProxy != null && platformStaticType != null) {
                                viewBlurProxy.asResolver().firstMethodOrNull { name = "setBlurType"; optional() }
                                    ?.invoke<Unit>(platformStaticType)
                            }
                        }
                    }
                }
                firstMethodOrNull {
                    name = "getNotificationScrimBgColor"
                    parameterCount = 1
                    optional()
                }?.hook {
                    before {
                        result = 0
                    }
                }
            }
        }

        // 10. 消除解锁状态下下拉通知/快捷设置时的纯灰底色遮罩
        runCatching {
            "com.android.systemui.statusbar.phone.ScrimState".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "getBehindTint"
                    emptyParameters()
                    optional()
                }?.hook {
                    after {
                        if (instance.toString() == "UNLOCKED") {
                            result = 0
                        }
                    }
                }
            }
        }

        runCatching {
            "com.oplus.systemui.scrim.ScrimViewExImp".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "updateColorWithTint"; optional() }?.hook {
                    before {
                        runCatching {
                            val scrimView = instance.asResolver().firstMethodOrNull { name = "getScrimView"; optional() }?.invoke<Any>()
                            scrimView?.asResolver()?.firstFieldOrNull { name = "mTintColor"; optional() }?.set(0)
                        }
                    }
                }
            }
        }

        YLog.info("ForceEnableSystemUIDynamicBlur: hooks initialized successfully", tag = "LuckyTool")
    }

    /**
     * 将进程内对指定 key 的 Settings 读取固定为给定值
     */
    private fun hookSettingGetInt(className: String, key: String, value: Int) {
        runCatching {
            className.toClassOrNull()?.resolve()?.apply {
                method { name = "getInt" }.hookAll {
                    before { if (args.size > 1 && args[1] == key) result = value }
                }
                method { name = "getIntForUser" }.hookAll {
                    before { if (args.size > 1 && args[1] == key) result = value }
                }
            }
        }
    }
}
