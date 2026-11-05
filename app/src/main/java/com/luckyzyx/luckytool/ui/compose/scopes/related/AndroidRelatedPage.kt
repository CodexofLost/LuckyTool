package com.luckyzyx.luckytool.ui.compose.scopes.related

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.service.PowerService
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.CommandUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.RestartMenuUtils
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.topjohnwu.superuser.ShellUtils
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog

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
        val sysuiBlurActive = state.getBoolean("force_enable_systemui_blur_feature", false)
        val gaussianSummaryExtra = if (sysuiBlurActive) {
            " (" + c.getString(R.string.gaussian_level_sysui_override_note) + ")"
        } else {
            ""
        }
        list(
            key = "customized_gaussian_blur_effect_level",
            title = c.getString(R.string.customized_gaussian_blur_effect_level),
            entries = c.resources.getStringArray(R.array.customized_gaussian_blur_effect_level_entries),
            entryValues = arrayOf("-1", "0", "1", "2", "3"),
            default = "-1",
            summary = arraySummaryLine(
                c.getString(R.string.current_mode) + ": %s$gaussianSummaryExtra",
                c.getString(R.string.need_restart_system),
            ),
        )

        custom(
            key = "customized_gaussian_blur_conflict_dialog",
            title = "",
        ) {
            val gaussianLevel by state.stringFlow("customized_gaussian_blur_effect_level", "-1")
                .collectAsStateWithLifecycle()
            val sysuiBlurActive by state.booleanFlow("force_enable_systemui_blur_feature", false)
                .collectAsStateWithLifecycle()
            var showConflictDialog by remember { mutableStateOf(false) }

            LaunchedEffect(gaussianLevel, sysuiBlurActive) {
                if (gaussianLevel == "0" && sysuiBlurActive) {
                    showConflictDialog = true
                }
            }

            if (showConflictDialog) {
                when (LocalUiMode.current) {
                    UiMode.Miuix -> OverlayDialog(
                        title = stringResource(R.string.gaussian_blur_disabled_conflict_title),
                        show = true,
                        onDismissRequest = {
                            showConflictDialog = false
                            state.set("customized_gaussian_blur_effect_level", "-1")
                        },
                    ) {
                        MiuixText(
                            text = stringResource(R.string.gaussian_blur_disabled_conflict_message),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MiuixTextButton(
                                text = stringResource(android.R.string.cancel),
                                onClick = {
                                    showConflictDialog = false
                                    state.set("customized_gaussian_blur_effect_level", "-1")
                                },
                                modifier = Modifier.weight(1f),
                            )
                            MiuixTextButton(
                                text = stringResource(android.R.string.ok),
                                onClick = {
                                    showConflictDialog = false
                                    state.set("force_enable_systemui_blur_feature", false)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColorsPrimary(),
                            )
                        }
                    }

                    UiMode.Material -> AlertDialog(
                        onDismissRequest = {
                            showConflictDialog = false
                            state.set("customized_gaussian_blur_effect_level", "-1")
                        },
                        title = { Text(stringResource(R.string.gaussian_blur_disabled_conflict_title)) },
                        text = { Text(stringResource(R.string.gaussian_blur_disabled_conflict_message)) },
                        dismissButton = {
                            TextButton(onClick = {
                                showConflictDialog = false
                                state.set("customized_gaussian_blur_effect_level", "-1")
                            }) {
                                Text(stringResource(android.R.string.cancel))
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showConflictDialog = false
                                state.set("force_enable_systemui_blur_feature", false)
                            }) {
                                Text(stringResource(android.R.string.ok))
                            }
                        },
                    )
                }
            }
        }
        switch(
            key = "sf_blurs_are_expensive_bypass",
            title = c.getString(R.string.sf_blurs_are_expensive_bypass),
            enabled = !sysuiBlurActive,
            summary = arraySummaryLine(
                if (sysuiBlurActive) {
                    c.getString(R.string.sf_blurs_managed_by_sysui_blur)
                } else {
                    c.getString(R.string.sf_blurs_are_expensive_bypass_summary)
                },
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
