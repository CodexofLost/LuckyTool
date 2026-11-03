package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceEnableDockerBackgroundBlur : YukiBaseHooker() {
    override fun onHook() {
        // 在 setDockerBackground() 调用栈内把模糊分支条件强制为 true，让宿主走原生模糊路径：
        // OplusHotseat: (isSupportNewBlur && hasLargeDisplayFeatures()) ? createBlurDrawable() : ExpandUtils.getHotseatNormalBgDrawable()
        val forcingBlur = ThreadLocal<Boolean>()

        //Source OplusHotseat
        "com.android.launcher3.OplusHotseat".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "setDockerBackground"; emptyParameters(); optional() }?.hook {
                before {
                    forcingBlur.set(true)
                }
                after {
                    forcingBlur.remove()
                }
            }
        }

        //Source OplusBlurProperties (class & companion)
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
            }

        //Source ScreenUtils - 仅在 setDockerBackground() 调用期间拦截 hasLargeDisplayFeatures 为 true
        // 从而避免非折叠屏设备全局布局或分屏逻辑被污染
        "com.android.common.util.ScreenUtils".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "hasLargeDisplayFeatures"; emptyParameters(); optional() }?.hook {
                before {
                    if (forcingBlur.get() == true) {
                        result = true
                    }
                }
            }
            firstMethodOrNull { name = "isSupportDockerExpandScreen"; emptyParameters(); optional() }?.hook {
                before {
                    result = true
                }
            }
        }

        //Source ExpandUtils (兜底防止回退到纯灰背景 hotseat_bg)
        "com.android.launcher3.hotseat.expand.ExpandUtils".toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "getHotseatNormalBgDrawable"; optional() }?.hook {
                before {
                    if (forcingBlur.get() == true) {
                        result = null
                    }
                }
            }
        }

        //Source AppFeatureUtils (disable light OS animation restrictions to unlock launcher blur)
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
        }
    }
}
