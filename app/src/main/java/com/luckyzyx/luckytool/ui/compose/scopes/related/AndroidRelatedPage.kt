package com.luckyzyx.luckytool.ui.compose.scopes.related

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.arraySummaryLine

/**
 * Android 系统动画与高斯模糊页：全局动画等级、高斯模糊等级、SystemUI 高斯模糊与实时表面模糊开关。
 */
object AndroidRelatedPage {

    val spec = ScopePageSpec(
        pageKey = "android_related",
        prefsName = ModulePrefs,
        packName = "system",
        displayPackName = "android",
        scopes = arrayOf("system"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }

        category(c.getString(R.string.category_blur_and_animation))
        list(
            key = "customized_anim_level",
            title = c.getString(R.string.customized_anim_level),
            entries = c.resources.getStringArray(R.array.customized_anim_level_entries),
            entryValues = arrayOf("-1", "1", "2", "3", "4"),
            default = "-1",
            summary = arraySummaryLine(
                c.getString(R.string.current_mode) + ": %s",
                c.getString(R.string.customized_anim_level_summary),
                c.getString(R.string.need_restart_system),
            ),
        )
        list(
            key = "customized_gaussian_blur_effect_level",
            title = c.getString(R.string.customized_gaussian_blur_effect_level),
            entries = c.resources.getStringArray(R.array.customized_gaussian_blur_effect_level_entries),
            entryValues = arrayOf("-1", "0", "1", "2", "3"),
            default = "-1",
            summary = arraySummaryLine(
                c.getString(R.string.current_mode) + ": %s",
                c.getString(R.string.need_restart_system),
            ),
        )
        list(
            key = "customized_systemui_gaussblur_level",
            title = c.getString(R.string.customized_systemui_gaussblur_level),
            entries = c.resources.getStringArray(R.array.customized_systemui_gaussblur_level_entries),
            entryValues = arrayOf("-1", "1", "2", "3"),
            default = "-1",
            summary = arraySummaryLine(
                c.getString(R.string.current_mode) + ": %s",
                c.getString(R.string.customized_systemui_gaussblur_level_summary),
                c.getString(R.string.need_restart_system),
            ),
        )
        list(
            key = "customized_systemui_dynamic_blur",
            title = c.getString(R.string.customized_systemui_dynamic_blur),
            entries = c.resources.getStringArray(R.array.customized_systemui_dynamic_blur_entries),
            entryValues = arrayOf("-1", "1", "0"),
            default = "-1",
            summary = arraySummaryLine(
                c.getString(R.string.current_mode) + ": %s",
                c.getString(R.string.customized_systemui_dynamic_blur_summary),
                c.getString(R.string.need_restart_system),
            ),
        )
    }
}
