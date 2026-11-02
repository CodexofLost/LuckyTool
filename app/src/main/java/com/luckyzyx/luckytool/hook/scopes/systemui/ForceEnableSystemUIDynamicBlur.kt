package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.View
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
 * 5. 桌面壁纸模糊底衬合成：
 *    - 在 AutoBlurDrawable.draw(Canvas) 执行前，为 scrimBehind 绘制 WallpaperBlurDrawable 作为动态模糊底衬。
 *    - 当在前台打开应用或抽屉时，应用图层不透明，前台动态模糊完整覆盖底衬；在桌面时，桌面图标与壁纸模糊完美融合。
 */
@Obfuscate
object ForceEnableSystemUIDynamicBlur : YukiBaseHooker() {

    override fun onHook() {
        // 1. Android Framework Window Blur 基础支持
        "android.view.CrossWindowBlurListeners".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "isCrossWindowBlurEnabled" }?.hook {
                before { result = true }
            }
        }

        "android.app.ActivityManager".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "isHighEndGfx" }?.hook {
                before { result = true }
            }
        }

        // 2. SystemUI BlurUtils 窗口模糊能力检查
        "com.android.systemui.statusbar.BlurUtils".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "supportsBlursOnWindows" }?.hook {
                before { result = true }
            }
            firstMethodOrNull { name = "supportsBlursOnWindowsBase" }?.hook {
                before { result = true }
            }
        }

        // 3. ColorOS 高斯模糊总闸（GaussBlurUtils）
        "com.oplus.systemui.blur.GaussBlurUtils".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "isGaussBlurDisabled" }?.hook {
                before { result = false }
            }
            firstMethodOrNull { name = "isQSGaussBlurDisabled" }?.hook {
                before { result = false }
            }
        }

        // 4. ColorOS Scrim 控制器等级判断（ScrimUtil）
        "com.oplusos.systemui.common.util.ScrimUtil".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "isLowGaussianLevel" }?.hook {
                before { result = false }
            }
            firstMethodOrNull { name = "isLightOsGaussianLevel" }?.hook {
                before { result = false }
            }
            firstMethodOrNull { name = "isLowGaussianLevelByRam" }?.hook {
                before { result = false }
            }
            firstMethodOrNull { name = "isWallpaperBlurDisable" }?.hook {
                before { result = false }
            }
            firstMethodOrNull { name = "isHighGaussianLevel" }?.hook {
                before { result = true }
            }
            firstMethodOrNull { name = "isBAnimLevel" }?.hook {
                before { result = false }
            }
            firstMethodOrNull { name = "isBPAnimLevel" }?.hook {
                before { result = false }
            }
            firstMethodOrNull { name = "isMiddleOrHighAnimLevel" }?.hook {
                before { result = true }
            }
            firstMethodOrNull { name = "getAnimLevel" }?.hook {
                before { result = 1 }
            }
        }

        // 5. ColorOS 平台能力等级（OplusPlatformLevelUtils）
        "com.oplus.util.OplusPlatformLevelUtils".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "getPlatformGaussianLevel" }?.hook {
                before { result = 3 }
            }
            firstMethodOrNull { name = "getPlatformAnimationLevel" }?.hook {
                before { result = 1 }
            }
            firstMethodOrNull { name = "isLowPlatform" }?.hook {
                before { result = false }
            }
        }

        // 6. ColorOS 质感与动态模糊状态管理器（PlatformBlurManagerImpl & MaterialBlurStateManager）
        "com.oplus.systemui.panelanimation.platformblur.PlatformBlurManagerImpl".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "useMotionBlur" }?.hook {
                before { result = true }
            }
            firstMethodOrNull { name = "handleTimeoutPauseBlur" }?.hook {
                before {
                    if (args.isNotEmpty()) {
                        args[0] = false
                    }
                }
            }
        }

        "com.oplus.systemui.panelanimation.platformblur.MaterialBlurStateManager".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "enableMaterialBlur" }?.hook {
                before { result = true }
            }
        }

        // 7. 系统设置中的质感模糊与窗口模糊开关拦截
        hookSettingGetInt("android.provider.Settings\$System", "system_material_blur_enable", 1)
        hookSettingGetInt("android.provider.Settings\$Global", "disable_window_blurs", 0)

        // 8. 消除解锁状态下下拉通知/快捷设置时的纯灰底色遮罩（ScrimControllerExImp & ScrimState）
        "com.oplus.systemui.statusbar.phone.ScrimControllerExImp".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull {
                name = "getNotificationScrimBgColor"
                parameterCount = 1
            }?.hook {
                before {
                    val scrimController = instance.asResolver().firstMethodOrNull { name = "getScrimController" }?.invoke<Any>()
                    val state = scrimController?.asResolver()?.firstMethodOrNull { name = "getState" }?.invoke<Any>()
                    val stateName = state?.toString()
                    if (stateName == "UNLOCKED" || stateName == null) {
                        result = 0
                    }
                }
            }
        }

        "com.android.systemui.statusbar.phone.ScrimState".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull {
                name = "getBehindTint"
                emptyParameters()
            }?.hook {
                after {
                    if (instance.toString() == "UNLOCKED") {
                        result = 0
                    }
                }
            }
        }

        // 9. 桌面壁纸模糊底衬合成：
        // 在桌面透明背景下，AutoBlurDrawable 捕获的 Surface 缺少壁纸图层（FLAG_SHOW_WALLPAPER）。
        // 在 AutoBlurDrawable.draw(Canvas) 执行前，为 scrimBehind 绘制 WallpaperBlurDrawable 作为动态模糊底衬。
        // 当打开应用或抽屉时，应用图层不透明，前台动态模糊完整覆盖底衬；在桌面时，桌面图标与壁纸模糊完美融合。
        "com.oplusos.systemui.common.blurability.drawable.AutoBlurDrawable".toClassOrNull()?.resolve()?.apply {
            var wallpaperBlurDrawable: Drawable? = null

            firstMethodOrNull {
                name = "draw"
                parameters(Canvas::class.java)
            }?.hook {
                before {
                    val canvas = args.getOrNull(0) as? Canvas ?: return@before
                    val autoBlur = instance as? Drawable ?: return@before
                    val viewBlurProxy = autoBlur.asResolver().firstMethodOrNull { name = "getViewBlurProxy" }?.invoke<Any>() ?: return@before
                    val hostView = viewBlurProxy.asResolver().firstMethodOrNull { name = "getView" }?.invoke<View>() ?: return@before

                    val entryName = runCatching { hostView.resources.getResourceEntryName(hostView.id) }.getOrNull()
                    if (entryName == "scrim_behind" || hostView.javaClass.name.contains("ScrimView")) {
                        val blurAmount = (viewBlurProxy.asResolver().firstMethodOrNull { name = "getBlurAmount" }?.invoke<Float>() as? Number)?.toFloat() ?: 0.0f
                        if (blurAmount > 0.001f) {
                            if (wallpaperBlurDrawable == null) {
                                val wbClass = "com.oplusos.systemui.common.blurability.wallpaperblur.WallpaperBlurDrawable".toClassOrNull()
                                if (wbClass != null) {
                                    runCatching {
                                        val ctor = wbClass.getConstructor(
                                            Context::class.java,
                                            Handler::class.java,
                                            Boolean::class.javaPrimitiveType
                                        )
                                        wallpaperBlurDrawable = ctor.newInstance(hostView.context, Handler(Looper.getMainLooper()), false) as? Drawable
                                    }
                                }
                            }
                            wallpaperBlurDrawable?.let { wb ->
                                wb.bounds = autoBlur.bounds
                                wb.alpha = (blurAmount * 255f).toInt().coerceIn(0, 255)
                                wb.draw(canvas)
                            }
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
