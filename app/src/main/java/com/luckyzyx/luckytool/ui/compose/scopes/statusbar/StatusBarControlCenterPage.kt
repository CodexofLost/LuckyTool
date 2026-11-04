package com.luckyzyx.luckytool.ui.compose.scopes.statusbar

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.RestartMenuUtils
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.showToast

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
