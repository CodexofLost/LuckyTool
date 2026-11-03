package com.luckyzyx.luckytool.ui.compose.scopes.related

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
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
        switch(
            key = "force_enable_launcher_high_level",
            title = c.getString(R.string.force_enable_launcher_high_level),
            summary = c.getString(R.string.force_enable_launcher_high_level_summary),
        )
        if (osCode >= 37) {
            switch(
                key = "force_enable_docker_background_blur",
                title = c.getString(R.string.force_enable_docker_background_blur),
            )
        }
    }
}
