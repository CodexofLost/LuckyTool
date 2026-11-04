package com.luckyzyx.luckytool.ui.compose.scopes.statusbar

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.RestartMenuUtils
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

        category(c.getString(R.string.category_blur_and_animation))
        list(
            key = "customized_shade_blur_mode",
            title = c.getString(R.string.customized_shade_blur_mode),
            entries = c.resources.getStringArray(R.array.customized_shade_blur_mode_entries),
            entryValues = arrayOf("0", "1", "2", "3"),
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
            summary = c.getString(R.string.need_restart_scope),
            notify = true,
        )

        switch(
            key = "enable_qs_tile_elastic_deformation",
            title = c.getString(R.string.enable_qs_tile_elastic_deformation),
            summary = arraySummaryLine(
                c.getString(R.string.enable_qs_tile_elastic_deformation_summary),
                c.getString(R.string.need_restart_scope),
            ),
            notify = true,
        )

        switch(
            key = "enable_media_player_blur",
            title = c.getString(R.string.enable_media_player_blur),
            summary = arraySummaryLine(
                c.getString(R.string.enable_media_player_blur_summary),
                c.getString(R.string.need_restart_scope),
            ),
            notify = true,
        )

        switch(
            key = "enable_bouncer_blur_scrim",
            title = c.getString(R.string.enable_bouncer_blur_scrim),
            summary = arraySummaryLine(
                c.getString(R.string.enable_bouncer_blur_scrim_summary),
                c.getString(R.string.need_restart_scope),
            ),
            notify = true,
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

        category(c.getString(R.string.enable_notification_background_blur_effect))
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

        category(c.getString(R.string.category_scope_management))
        click(
            title = c.getString(R.string.restart_systemui),
            summary = c.getString(R.string.restart_systemui_summary),
            onClick = {
                RestartMenuUtils.restartScope(c, arrayOf("com.android.systemui"))
            },
        )
        click(
            title = c.getString(R.string.reload_hooker_page),
            summary = c.getString(R.string.reload_hooker_page_summary),
            onClick = {
                RestartMenuUtils.restartHooker(c, arrayOf("com.android.systemui"))
            },
        )
    }
}
