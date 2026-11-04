package com.luckyzyx.luckytool.ui.compose.scopes.related

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.service.PowerService
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.CommandUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.RestartMenuUtils
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.topjohnwu.superuser.ShellUtils

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
            key = "customized_framework_anim_level",
            title = c.getString(R.string.customized_framework_anim_level),
            entries = c.resources.getStringArray(R.array.customized_framework_anim_level_entries),
            entryValues = arrayOf("-1", "1", "2", "3"),
            default = "-1",
            summary = arraySummaryLine(
                c.getString(R.string.current_mode) + ": %s",
                c.getString(R.string.customized_framework_anim_level_summary),
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
        switch(
            key = "sf_blurs_are_expensive_bypass",
            title = c.getString(R.string.sf_blurs_are_expensive_bypass),
            summary = arraySummaryLine(
                c.getString(R.string.sf_blurs_are_expensive_bypass_summary),
                c.getString(R.string.need_restart_system),
            ),
        )
        switch(
            key = "enable_spring_overscroll",
            title = c.getString(R.string.enable_spring_overscroll),
            summary = arraySummaryLine(
                c.getString(R.string.enable_spring_overscroll_summary),
                c.getString(R.string.need_restart_system),
            ),
        )
        switch(
            key = "enable_parallel_gesture_animation",
            title = c.getString(R.string.enable_parallel_gesture_animation),
            summary = arraySummaryLine(
                c.getString(R.string.enable_parallel_gesture_animation_summary),
                c.getString(R.string.need_restart_system),
            ),
        )
        switch(
            key = "enable_adaptive_smooth_animation",
            title = c.getString(R.string.enable_adaptive_smooth_animation),
            summary = arraySummaryLine(
                c.getString(R.string.enable_adaptive_smooth_animation_summary),
                c.getString(R.string.need_restart_system),
            ),
        )

        category(c.getString(R.string.category_scope_management))
        click(
            title = c.getString(R.string.soft_reboot_framework),
            summary = c.getString(R.string.soft_reboot_framework_summary),
            onClick = {
                ShellUtils.fastCmd(CommandUtils.killzygote)
            },
        )
        click(
            title = c.getString(R.string.reboot_device),
            summary = c.getString(R.string.reboot_device_summary),
            onClick = {
                PowerService.get(c) { controller ->
                    controller?.reboot(false, null, false)
                }
            },
        )
        click(
            title = c.getString(R.string.reload_hooker_page),
            summary = c.getString(R.string.reload_hooker_page_summary),
            onClick = {
                RestartMenuUtils.restartHooker(c, arrayOf("system"))
            },
        )
    }
}
