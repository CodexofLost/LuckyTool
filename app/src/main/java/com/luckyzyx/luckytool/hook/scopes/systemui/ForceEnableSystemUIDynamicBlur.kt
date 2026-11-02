package com.luckyzyx.luckytool.hook.scopes.systemui

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
