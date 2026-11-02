package com.luckyzyx.luckytool.hook.scopes.systemui

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
 * 1. 禁用 B/BP 级降级分支：
 *    - isBAnimLevel / isBPAnimLevel 必须返回 false。若为 true，PlatformBlurManagerImpl 会跳过
 *      Blur.resumeWindowBlur() 调用，且 ScrimControllerExImp 会将背景 Drawable 切换为静态
 *      WallpaperBlurDrawable，导致动态模糊失效并产生透明/纯灰底色轮转现象。
 * 2. 锁定高斯模糊与动画能力为旗舰最高级：
 *    - isHighGaussianLevel = true, getAnimLevel = 1, getPlatformGaussianLevel = 3, getPlatformAnimationLevel = 1。
 * 3. 动态模糊管线与超时防护：
 *    - 拦截 PlatformBlurManagerImpl.useMotionBlur() 固定为 true。
 *    - 拦截 PlatformBlurManagerImpl.handleTimeoutPauseBlur() 阻止超时自动暂停动态模糊。
 *    - 拦截 MaterialBlurStateManager.enableMaterialBlur() 固定为 true。
 * 4. 消除解锁状态下下拉通知/快捷设置时的纯灰底色遮罩：
 *    - 拦截 ScrimControllerExImp.getNotificationScrimBgColor() 与 ScrimState.getBehindTint()，
 *      在 UNLOCKED 状态下固定返回 0（透明），防止 isQsVisible=true 时强行注入 #1A1A1A 纯灰遮罩。
 * 5. 保持解锁状态下拉通知/快捷设置时桌面壁纸图层合成（FLAG_SHOW_WALLPAPER）：
 *    - 拦截 OplusNotificationShadeWindowControllerExImpl.shouldShowWallpaper，在下拉展开或面板可见时返回 true。
 *    - WindowManager/SurfaceFlinger 在状态栏窗口展开期间保持壁纸与桌面图标、应用、PiP 视频统一合成，实现 100% 实时动态模糊。
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

        // 2. SystemUI BlurUtils 窗口模糊能力检查 (Android 16 采用 supportsBlursOnWindowsBase)
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

        // 4. ColorOS Scrim 控制器等级判断（ScrimUtil）
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

        // 5. ColorOS 平台能力等级（OplusPlatformLevelUtils）
        runCatching {
            "com.oplus.util.OplusPlatformLevelUtils".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "getPlatformGaussianLevel"; optional() }?.hook {
                    before { result = 3 }
                }
                firstMethodOrNull { name = "getPlatformAnimationLevel"; optional() }?.hook {
                    before { result = 1 }
                }
                firstMethodOrNull { name = "isLowPlatform"; optional() }?.hook {
                    before { result = false }
                }
            }
        }

        // 6. ColorOS 质感与动态模糊状态管理器（PlatformBlurManagerImpl & MaterialBlurStateManager）
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
                // 拦截通知栏/快捷设置下拉过程中的强制 pauseBlur 调用
                firstMethodOrNull {
                    name = "pauseBlur"
                    parameterCount = 1
                    optional()
                }?.hook {
                    before {
                        val reason = args.getOrNull(0) as? String ?: ""
                        if (reason.contains("should not blur") || reason.contains("unsupportBlur")) {
                            result = null
                            return@before
                        }
                        val isExpanding = runCatching {
                            instance.asResolver().firstFieldOrNull { name = "isExpanding"; optional() }?.get<Boolean>()
                        }.getOrNull() == true

                        if (isExpanding) {
                            result = null
                        }
                    }
                }
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
                            runCatching {
                                instance.asResolver().firstMethodOrNull { name = "resumeBlur"; parameterCount = 1; optional() }
                                    ?.invoke<Unit>("luckytool_force_resume")
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

        // 7. 系统设置中的质感模糊与窗口模糊开关拦截
        hookSettingGetInt("android.provider.Settings\$System", "system_material_blur_enable", 1)
        hookSettingGetInt("android.provider.Settings\$Global", "disable_window_blurs", 0)

        // 8. 消除解锁状态下下拉通知/快捷设置时的纯灰底色遮罩（ScrimControllerExImp & ScrimState）
        runCatching {
            "com.oplus.systemui.statusbar.phone.ScrimControllerExImp".toClassOrNull()?.resolve()?.apply {
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

        // 9. 保持解锁状态下拉通知/快捷设置时桌面壁纸图层合成（FLAG_SHOW_WALLPAPER）：
        // 在桌面透明背景下，Launcher 仅绘制图标与 Dock，壁纸由 ImageWallpaper 独立图层承载。
        // ColorOS 的 OplusNotificationShadeWindowControllerExImpl.shouldShowWallpaper 在解锁状态下拉时默认返回 false，
        // 导致 NotificationShadeWindowControllerImpl 清除 FLAG_SHOW_WALLPAPER，SurfaceFlinger 无法捕获壁纸底衬。
        // 拦截 shouldShowWallpaper，当下拉展开或面板可见时返回 true，确保壁纸与桌面图标、应用、PiP 视频统一参与硬件动态高斯模糊。
        runCatching {
            "com.oplus.systemui.shade.OplusNotificationShadeWindowControllerExImpl".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "shouldShowWallpaper"
                    optional()
                }?.hook {
                    after {
                        val state = args.getOrNull(1)
                        val panelVisible = runCatching {
                            state?.asResolver()?.firstFieldOrNull { name = "panelVisible"; optional() }?.get<Boolean>()
                        }.getOrNull() == true
                        val shadeOrQsExpanded = runCatching {
                            state?.asResolver()?.firstFieldOrNull { name = "shadeOrQsExpanded"; optional() }?.get<Boolean>()
                        }.getOrNull() == true
                        val keyguardShowing = runCatching {
                            state?.asResolver()?.firstFieldOrNull { name = "keyguardShowing"; optional() }?.get<Boolean>()
                        }.getOrNull() == true

                        if (!keyguardShowing && (panelVisible || shadeOrQsExpanded)) {
                            result = true
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
