package com.luckyzyx.luckytool.ui.compose.scopes.statusbar

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.arraySummaryLine

/**
 * 控制中心与通知栏模糊页：QS 与通知中心背景动态模糊 / 壁纸模糊模式开关与调节。
 */
object StatusBarControlCenterPage {

    val spec = ScopePageSpec(
        pageKey = "statusbar_control_center",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf("com.android.systemui"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }

        category(c.getString(R.string.customized_shade_blur_mode))
        list(
            key = "customized_shade_blur_mode",
            title = c.getString(R.string.customized_shade_blur_mode),
            entries = c.resources.getStringArray(R.array.customized_shade_blur_mode_entries),
            entryValues = arrayOf("0", "1", "2"),
            default = "0",
            summary = arraySummaryLine(
                c.getString(R.string.current_mode) + ": %s",
                c.getString(R.string.customized_shade_blur_mode_summary),
                c.getString(R.string.need_restart_scope),
            ),
            notify = true,
        )

        switch(
            key = "force_enable_systemui_blur_feature",
            title = c.getString(R.string.force_enable_systemui_blur_feature),
            summary = arraySummaryLine(
                c.getString(R.string.force_enable_systemui_blur_feature_summary),
                c.getString(R.string.need_restart_scope),
            ),
            notify = true,
        )

        switch(
            key = "enable_notification_background_blur_effect",
            title = c.getString(R.string.enable_notification_background_blur_effect),
            summary = arraySummaryLine(
                c.getString(R.string.force_enable_systemui_blur_feature_tips),
                c.getString(R.string.force_enable_systemui_blur_feature_tips_2)
            ),
            notify = true,
        )

        slider(
            key = "custom_notification_background_transparency",
            title = c.getString(R.string.custom_notification_background_transparency),
            valueRange = -1..100,
            default = -1,
            summary = c.getString(R.string.force_enable_systemui_blur_feature_tips),
            notify = true,
        )

        slider(
            key = "custom_control_center_background_transparency",
            title = c.getString(R.string.custom_control_center_background_transparency),
            valueRange = -1..100,
            default = -1,
            summary = c.getString(R.string.force_enable_systemui_blur_feature_tips),
            notify = true,
        )
    }
}
