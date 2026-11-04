package com.luckyzyx.luckytool.hook.scopes.launcher

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceEnableDockerBackgroundBlur : YukiBaseHooker() {
    override fun onHook() {
        "com.android.launcher3.OplusHotseat".toClassOrNull()?.resolve()?.apply {
            // 1. 拦截 OplusHotseat.setDockerBackground，原生直接调用 createBlurDrawable() 设置模糊背景，
            // 杜绝依赖 ScreenUtils.hasLargeDisplayFeatures() 导致平板模式白图标，同时严格遵守原生 mBackgroundVisible 开关
            firstMethodOrNull { name = "setDockerBackground"; emptyParameters(); optional() }?.hook {
                replaceUnit {
                    val inst = instance ?: return@replaceUnit
                    val saw = runCatching {
                        inst.javaClass.getMethod("getShortcutsAndWidgets").invoke(inst) as? ViewGroup
                    }.getOrNull() ?: return@replaceUnit

                    if (saw.childCount == 0) {
                        saw.setBackgroundResource(0)
                        return@replaceUnit
                    }

                    // 调用原生 createBlurDrawable() 获取真实 LayerBlurDrawable 动态高斯模糊
                    val blurDrawable = runCatching {
                        val createBlurMethod = inst.javaClass.getDeclaredMethod("createBlurDrawable").apply { isAccessible = true }
                        createBlurMethod.invoke(inst) as? Drawable
                    }.getOrNull()

                    if (blurDrawable != null) {
                        saw.background = blurDrawable
                    } else {
                        val normalBg = runCatching {
                            val expandUtilsClass = "com.android.launcher3.hotseat.expand.ExpandUtils".toClassOrNull()
                            val m = expandUtilsClass?.getDeclaredMethod("getHotseatNormalBgDrawable", Context::class.java)
                            m?.invoke(null, (inst as View).context) as? Drawable
                        }.getOrNull()
                        if (normalBg != null) {
                            saw.background = normalBg
                        }
                    }

                    // 尊重用户在桌面设置中的开关状态 (mBackgroundVisible: true 显示, false 完全透明隐藏)
                    val bgVisible = runCatching {
                        val f = inst.javaClass.getDeclaredField("mBackgroundVisible").apply { isAccessible = true }
                        f.get(inst) as? Boolean
                    }.getOrNull() ?: true

                    runCatching {
                        val setBgAlpha = inst.javaClass.getMethod("setBackgroundAlpha", Float::class.javaPrimitiveType)
                        setBgAlpha.invoke(inst, if (bgVisible) 1.0f else 0.0f)
                    }
                }
            }

            // 2. 阻止回收销毁模糊材质属性，防止关闭文件夹或返回桌面时背景销毁
            firstMethodOrNull { name = "recycleBlurBackground"; optional() }?.hook {
                before {
                    intercept()
                }
            }
        }
    }
}
