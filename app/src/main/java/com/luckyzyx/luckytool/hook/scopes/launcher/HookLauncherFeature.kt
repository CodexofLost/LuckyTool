package com.luckyzyx.luckytool.hook.scopes.launcher

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
        val osCode = getOSVersionCode
        loadHooker(HookFeatureOption)
        loadHooker(HookLauncherSettings(dexKitBridge))
        loadHooker(HookLauncherDynamicBlur)
        if (osCode >= 34) loadHooker(HookAppFeature)
    }

    @Obfuscate
    object HookLauncherDynamicBlur : YukiBaseHooker() {
        override fun onHook() {
            val enableBlur = preferences(ModulePrefs).getBoolean("force_enable_systemui_blur_feature", false) ||
                preferences(ModulePrefs).getBoolean("force_enable_launcher_high_level", false)
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
        }
    }

    @Obfuscate
    object HookAppFeature : YukiBaseHooker() {
        override fun onHook() {
            val disableAutoSwitch =
                preferences(ModulePrefs).getBoolean("disable_auto_switch_last_task", false)
            if (!disableAutoSwitch) return

            //Source AppFeatureUtils (all versions)
            //Legacy gate, new Launcher still consults it in StackPagedViewEx,
            //getToRecentsFocusPage, AppToOverviewAnimationProvider and
            //StandardInterruptHelper.canFocusToNextPage
            "com.android.common.util.AppFeatureUtils".toClass().resolve().apply {
                //Source OplusGridRecentsConfig isEnable
                firstMethodOrNull {
                    name = "isSupportAutoFocusToNextPageInOverviewState"
                    parameterCount = 1
                }?.hook {
                    intercept(false)
                }
                firstMethod {
                    name = "isSupportAutoFocusToNextPageInOverviewState"
                    emptyParameters()
                }.hook {
                    intercept(false)
                }
            }

            //Source RecentInterruptAnimUtilKt (new Launcher only)
            //computeNonInterruptFocusToNextPageTarget dropped the gate and returns
            //runningTaskIndex + 1, which StackRecentsViewDelegate.updateStackLayoutNextPage
            //uses to overwrite the hooked value; -1 falls back to getNextPage()
            "com.oplus.quickstep.utils.RecentInterruptAnimUtilKt".toClassOrNull()?.resolve()
                ?.apply {
                    firstMethodOrNull {
                        name = "computeNonInterruptFocusToNextPageTarget"
                        parameterCount = 1
                    }?.hook {
                        intercept(-1)
                    }
                }

            //Source TileCardFirstInterruptFocusPolicy (new Launcher only, card-first path)
            //resolveFocusPageFallback decides the card-first settle page without the gate;
            //-1 falls back to getNextPage()
            "com.oplus.quickstep.utils.tilecardfirst.policy.TileCardFirstInterruptFocusPolicy"
                .toClassOrNull()?.resolve()?.apply {
                    firstMethodOrNull {
                        name = "resolveFocusPageFallback"
                        parameterCount = 2
                    }?.hook {
                        intercept(-1)
                    }
                }
        }
    }

    @Obfuscate
    object HookFeatureOption : YukiBaseHooker() {
        override fun onHook() {
            val appUpdateDot = preferences(ModulePrefs).getBoolean("enable_display_app_update_dot", false)
            val disableDockerMax =
                preferences(ModulePrefs).getBoolean("remove_docker_max_number_limit", false)
            val allowWidget =
                preferences(ModulePrefs).getBoolean("remove_widgets_add_request_whitelist", false)

            //Source FeatureOption
            "com.android.common.config.FeatureOption".toClass().resolve().apply {
                firstMethod { name = "initFeature" }.hook {
                    after {
                        if (appUpdateDot) {
                            firstFieldOrNull { name = "isSupportAppUpdateDotSwitch" }?.set(true)
                        }
                    }
                }
                if (disableDockerMax) {
                    firstMethodOrNull { name = "isDockerMax5" }?.hook {
                        intercept(false)
                    }
                }
                if (allowWidget) {
                    firstMethodOrNull { name = "isSupportWhiteListControl" }?.hook {
                        intercept(true)
                    }
                }
            }
        }
    }

    @Obfuscate
    class HookLauncherSettings(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            val appUpdateDot = preferences(ModulePrefs).getBoolean("enable_display_app_update_dot", false)

            //Source LauncherSettingsUtils
            dexKitBridge.findClass {
                matcher {
                    usingStrings("content://com.android.launcher.settings", "LauncherSettingsUtils")
                }
            }.apply {
                checkDataList("find clazz LauncherSettingsUtils")

                single().name.toClass().resolve().apply {
                    if (appUpdateDot) {
                        firstMethodOrNull { name = "isSupportAppUpdateDot" }?.hook {
                            intercept(true)
                        }
                    }
                }
            }
        }
    }
}
