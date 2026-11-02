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
 * 在中低端设备上，ColorOS 将动画级别提升至 Tier-3 时会将通知面板底色置为透明以期待动态模糊；
 * 但因内部多道检测（BlurUtils、GaussBlurUtils、ScrimUtil、CrossWindowBlurListeners 等）
 * 均判定不支持模糊，导致模糊计算被跳过（"[applyBlur] Skip for supportsBlursOnWindows() is false"），
 * 最终出现通知面板全透明现象。
 *
 * 此 Hooker 统一拦截这些内部校验方法，确保动态高斯模糊渲染管线正常运转。
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
                before { result = true }
            }
            firstMethodOrNull { name = "isBPAnimLevel" }?.hook {
                before { result = true }
            }
            firstMethodOrNull { name = "isMiddleOrHighAnimLevel" }?.hook {
                before { result = true }
            }
        }

        // 5. ColorOS 平台能力等级（OplusPlatformLevelUtils）
        "com.oplus.util.OplusPlatformLevelUtils".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "getPlatformGaussianLevel" }?.hook {
                before { result = 3 }
            }
            firstMethodOrNull { name = "getPlatformAnimationLevel" }?.hook {
                before { result = 3 }
            }
            firstMethodOrNull { name = "isLowPlatform" }?.hook {
                before { result = false }
            }
        }

        // 6. 系统设置中的质感模糊与窗口模糊开关拦截
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
