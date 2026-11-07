package com.luckyzyx.luckytool.ui.compose.scopes.statusbar

import android.content.ComponentName
import android.content.Intent
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.DeviceUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.RestartMenuUtils
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.sendPrefsValue
import com.luckyzyx.luckytool.utils.showToast
import com.topjohnwu.superuser.ShellUtils

/**
 * 控制中心与通知栏模糊页：QS 与通知中心背景动态模糊 / 壁纸模糊模式开关与调节。
 */
object StatusBarControlCenterPage {

    val spec = ScopePageSpec(
        pageKey = "statusbar_control_center",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf(
            "com.android.systemui",
            "com.oplus.notificationmanager",
            "com.oplus.pantanal.ums",
        ),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }

        category(c.getString(R.string.category_blur_and_animation))
        switch(
            key = "force_enable_systemui_blur_feature",
            title = c.getString(R.string.force_enable_systemui_blur_feature),
            summary = arraySummaryLine(
                c.getString(R.string.force_enable_systemui_blur_feature_summary),
                c.getString(R.string.need_restart_scope),
            ),
            notify = true,
            onChange = { newValue ->
                if (newValue) {
                    val gaussianLevel = state.getString("customized_gaussian_blur_effect_level", "-1")
                    if (gaussianLevel == "0") {
                        state.set("customized_gaussian_blur_effect_level", "-1")
                        c.showToast(c.getString(R.string.systemui_blur_requires_gaussian_message))
                    }
                }
            },
        )

        switch(
            key = "enable_panoramic_aod",
            title = c.getString(R.string.enable_panoramic_aod),
            summary = arraySummaryLine(
                c.getString(R.string.enable_panoramic_aod_summary),
                c.getString(R.string.need_restart_scope),
            ),
            notify = true,
        )

        category(c.getString(R.string.category_fluid_cloud_capsule))
        switch(
            key = "enable_fluid_cloud_capsule_live_alerts",
            title = c.getString(R.string.enable_fluid_cloud_capsule_live_alerts),
            summary = arraySummaryLine(
                c.getString(R.string.enable_fluid_cloud_capsule_live_alerts_summary),
                c.getString(R.string.need_restart_scope),
            ),
            notify = true,
            onChange = { newValue ->
                c.sendPrefsValue("system", "enable_fluid_cloud_capsule_live_alerts", newValue)
                if (DeviceUtils.getRootStatus()) {
                    if (newValue) {
                        ShellUtils.fastCmd(
                            "resetprop persist.sys.oplus.live_bridge.support 1",
                            "resetprop persist.sys.oplus.capsule.support 1",
                            "resetprop debug.oplus.capsule.enable 1",
                            "resetprop persist.sys.oplus.notification.alert 1",
                            "resetprop -n ro.oplus.support.notification.capsule 1",
                            "pm enable com.oplus.pantanal.ums",
                            "am startservice -a coloros.intent.action.ums.running.server -p com.oplus.pantanal.ums",
                        )
                    } else {
                        ShellUtils.fastCmd(
                            "resetprop persist.sys.oplus.live_bridge.support 0",
                            "resetprop persist.sys.oplus.capsule.support 0",
                            "resetprop debug.oplus.capsule.enable 0",
                            "resetprop persist.sys.oplus.notification.alert 0",
                            "resetprop -n ro.oplus.support.notification.capsule 0",
                        )
                    }
                }
            },
        )
        click(
            title = c.getString(R.string.open_live_alerts_settings),
            summary = c.getString(R.string.open_live_alerts_settings_summary),
            onClick = {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.oplus.notificationmanager",
                        "com.oplus.notificationmanager.fluidseedingsettings.activity.FluidSeedingSettingMainActivity",
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    c.startActivity(intent)
                } catch (_: Exception) {
                    ShellUtils.fastCmd("am start -n com.oplus.notificationmanager/.fluidseedingsettings.activity.FluidSeedingSettingMainActivity")
                }
            },
        )

        category(c.getString(R.string.category_scope_management))
        click(
            title = c.getString(R.string.restart_systemui),
            summary = c.getString(R.string.restart_systemui_summary),
            onClick = {
                RestartMenuUtils.restartScope(
                    c,
                    arrayOf("com.android.systemui", "com.oplus.notificationmanager", "com.oplus.pantanal.ums"),
                )
            },
        )
        click(
            title = c.getString(R.string.reload_hooker_page),
            summary = c.getString(R.string.reload_hooker_page_summary),
            onClick = {
                RestartMenuUtils.restartHooker(
                    c,
                    arrayOf("com.android.systemui", "com.oplus.notificationmanager", "com.oplus.pantanal.ums"),
                )
            },
        )
    }
}
