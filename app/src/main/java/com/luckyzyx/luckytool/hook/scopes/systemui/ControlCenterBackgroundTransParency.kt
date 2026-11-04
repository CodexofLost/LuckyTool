package com.luckyzyx.luckytool.hook.scopes.systemui

import android.view.View
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.safeOfNull
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ControlCenterBackgroundTransParency : YukiBaseHooker() {
    override fun onHook() {
        var customAlpha =
            preferences(ModulePrefs).getInt("custom_control_center_background_transparency", -1)
        dataChannel.wait<Int>("custom_control_center_background_transparency") {
            customAlpha = it
        }

        //Source ScrimController
        // updateScrimColor(View view, int tint, float alpha)
        "com.android.systemui.statusbar.phone.ScrimController".toClass().resolve().apply {
            firstMethod { name = "updateScrimColor" }.hook {
                before {
                    if (customAlpha < 0) return@before
                    val ratio = customAlpha.coerceIn(0, 100) / 100.0F
                    val view = firstArg().get<View>() ?: return@before
                    val alpha = arg(2).get<Float>() ?: return@before

                    val mScrimBehind = safeOfNull { firstFieldOrNull { name = "mScrimBehind"; optional() }?.of(instance)?.get<View>() }
                    val mNotificationsScrim = safeOfNull { firstFieldOrNull { name = "mNotificationsScrim"; optional() }?.of(instance)?.get<View>() }

                    val isTarget = (view === mScrimBehind) || (view === mNotificationsScrim) || run {
                        val name = safeOfNull { view.resources.getResourceEntryName(view.id) }
                        name == "scrim_behind" || name == "scrim_notifications"
                    }

                    if (isTarget && alpha > 0f) {
                        arg(2).set(alpha * ratio)
                    }
                }
            }
        }
    }
}