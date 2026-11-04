package com.luckyzyx.luckytool.ui.compose.scopes.related

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.RestartMenuUtils
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 桌面动画与模糊页：Docker 背景高斯模糊、文件夹动态高斯模糊与旗舰动画等级开关。
 */
object LauncherRelatedPage {

    val spec = ScopePageSpec(
        pageKey = "launcher",
        prefsName = ModulePrefs,
        packName = "com.android.launcher",
        scopes = arrayOf("com.android.launcher", "com.oppo.launcher"),
        restartEnabled = true,
        isVisible = { checkPackName("com.android.launcher") },
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        val osCode = getOSVersionCode

        category(c.getString(R.string.category_blur_and_animation))
        list(
            key = "customized_launcher_anim_level",
            title = c.getString(R.string.customized_launcher_anim_level),
            entries = c.resources.getStringArray(R.array.customized_launcher_anim_level_entries),
            entryValues = arrayOf("-1", "1", "2", "3", "4"),
            default = "-1",
            summary = arraySummaryLine(
                c.getString(R.string.current_mode) + ": %s",
                c.getString(R.string.customized_launcher_anim_level_summary),
                c.getString(R.string.need_restart_scope),
            ),
        )

        if (osCode >= 34) {
            switch(
                key = "force_enable_docker_background_blur",
                title = c.getString(R.string.force_enable_docker_background_blur),
                summary = c.getString(R.string.need_restart_scope),
            )
        }

        switch(
            key = "enable_folder_material_blur",
            title = c.getString(R.string.enable_folder_material_blur),
            summary = arraySummaryLine(
                c.getString(R.string.enable_folder_material_blur_summary),
                c.getString(R.string.need_restart_scope),
            ),
        )

        switch(
            key = "enable_drawer_dynamic_blur",
            title = c.getString(R.string.enable_drawer_dynamic_blur),
            summary = arraySummaryLine(
                c.getString(R.string.enable_drawer_dynamic_blur_summary),
                c.getString(R.string.need_restart_scope),
            ),
        )

        switch(
            key = "enable_wallpaper_zoom_animation",
            title = c.getString(R.string.enable_wallpaper_zoom_animation),
            summary = arraySummaryLine(
                c.getString(R.string.enable_wallpaper_zoom_animation_summary),
                c.getString(R.string.need_restart_scope),
            ),
        )

        switch(
            key = "force_enable_recents_stack_layout",
            title = c.getString(R.string.force_enable_recents_stack_layout),
            summary = arraySummaryLine(
                c.getString(R.string.force_enable_recents_stack_layout_summary),
                c.getString(R.string.need_restart_scope),
            ),
        )

        switch(
            key = "disable_launcher_low_mem_limits",
            title = c.getString(R.string.disable_launcher_low_mem_limits),
            summary = arraySummaryLine(
                c.getString(R.string.disable_launcher_low_mem_limits_summary),
                c.getString(R.string.need_restart_scope),
            ),
        )

        category(c.getString(R.string.category_scope_management))
        click(
            title = c.getString(R.string.restart_launcher),
            summary = c.getString(R.string.restart_launcher_summary),
            onClick = {
                RestartMenuUtils.restartScope(c, arrayOf("com.android.launcher", "com.oppo.launcher"))
            },
        )
        click(
            title = c.getString(R.string.reload_hooker_page),
            summary = c.getString(R.string.reload_hooker_page_summary),
            onClick = {
                RestartMenuUtils.restartHooker(c, arrayOf("com.android.launcher", "com.oppo.launcher"))
            },
        )
    }
}
