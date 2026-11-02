package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.View
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.ThemeUtils.isNightMode
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.safeOfNan
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object CustomNotificationBackgroundTransparency : YukiBaseHooker() {

    private var defaultNotifyPanelTintList: ColorStateList? = null
    private var defaultNotifyPanelElevation = -1f
    private var customAlpha = -1

    val NotificationBackgroundView =
        "com.android.systemui.statusbar.notification.row.NotificationBackgroundView"
    val OplusNotificationBackgroundView =
        "com.oplusos.systemui.statusbar.notification.row.OplusNotificationBackgroundView"

    override fun onHook() {
        if (getOSVersionCode < 25) return
        customAlpha = preferences(ModulePrefs).getInt("custom_notification_background_transparency", -1)
        dataChannel.wait<Int>("custom_notification_background_transparency") {
            customAlpha = it
        }

        //Source OplusNotificationBackgroundView / NotificationBackgroundView
        VariousClass(
            OplusNotificationBackgroundView,
            NotificationBackgroundView
        ).toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "drawRegionBlur"; parameterCount = 2 }?.hook {
                before {
                    if (customAlpha < 0) return@before
                    modifyNotifyPanelAlpha(instance(), lastArg().get<Drawable>())
                }
            }
            firstMethodOrNull { name = "drawCustom"; parameterCount = 2 }?.hook {
                before {
                    if (customAlpha < 0) return@before
                    modifyNotifyPanelAlpha(instance(), lastArg().get<Drawable>())
                }
            }
            firstMethodOrNull { name = "draw"; parameterCount = 2 }?.hook {
                before {
                    if (customAlpha < 0) return@before
                    modifyNotifyPanelAlpha(instance(), lastArg().get<Drawable>())
                }
            }
        }

        //Source ExpandableNotificationRow
        "com.android.systemui.statusbar.notification.row.ExpandableNotificationRow".toClassOrNull()
            ?.resolve()?.apply {
                firstMethodOrNull {
                    name = "updateBackgroundForGroupState"
                    emptyParameters()
                }?.hook {
                    before {
                        if (customAlpha < 0) return@before
                        firstFieldOrNull { name = "mShowGroupBackgroundWhenExpanded" }?.of(instance)
                            ?.set(true)
                    }
                }
            }
    }

    /**
     * 设置通知面板背景透明度
     * @param view 背景 View 实例
     * @param drawable 背景实例
     * @param isTint 是否着色 [view]
     */
    private fun modifyNotifyPanelAlpha(view: View?, drawable: Drawable?, isTint: Boolean = false) {
        if (view == null) return
        if (defaultNotifyPanelTintList == null) defaultNotifyPanelTintList = view.backgroundTintList
        if (defaultNotifyPanelElevation < 0f) defaultNotifyPanelElevation = view.elevation
        val currentColor = if (view.context.isNightMode) 0xFF404040.toInt() else 0xFFFAFAFA.toInt()
        when {

            isTint.not() && view.parent?.parent?.javaClass?.name?.contains("ChildrenContainer") == true -> {
                drawable?.alpha = 0
            }

            else -> {
                currentColor.colorAlphaOf(customAlpha / 10.0F).also {
                    if (isTint) view.backgroundTintList = ColorStateList.valueOf(it)
                    else drawable?.setTint(it)
                }
            }
        }
        view.elevation = if (customAlpha >= 0) 0f else defaultNotifyPanelElevation
    }

    /**
     * 调整颜色透明度
     * @param value 透明度
     * @return [Int] 调整后的颜色
     */
    private fun Int.colorAlphaOf(value: Float) =
        safeOfNan { (255.coerceAtMost(0.coerceAtLeast((value * 255).toInt())) shl 24) + (0x00ffffff and this) }

}