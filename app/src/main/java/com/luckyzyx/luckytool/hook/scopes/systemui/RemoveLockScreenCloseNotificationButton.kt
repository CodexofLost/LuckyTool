package com.luckyzyx.luckytool.hook.scopes.systemui

import android.view.View
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveLockScreenCloseNotificationButton : YukiBaseHooker() {
    override fun onHook() {
        // Source NotificationPanelViewExt / OplusNotificationCloseButtonImp (C12 - C14)
        VariousClass(
            "com.oplusos.systemui.notification.extend.NotificationPanelViewExt",
            "com.oplus.systemui.notification.extend.OplusNotificationCloseButtonImp"
        ).toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "setNotificationCloseButton" }?.hook {
                intercept()
            }
        }

        // Source ClearAllController / OplusClearAllButton (C15 - C16)
        "com.oplus.systemui.notification.clearall.OplusClearAllButton".toClassOrNull()
            ?.resolve()?.apply {
                firstMethodOrNull { name = "getTargetVisible" }?.hook {
                    intercept(false)
                }
                firstMethodOrNull { name = "setAlpha" }?.hook {
                    before {
                        (instance as? View)?.isVisible = false
                    }
                }
            }

        "com.oplus.systemui.notification.clearall.ClearAllController".toClassOrNull()
            ?.resolve()?.apply {
                firstMethodOrNull { name = "bindViews" }?.hook {
                    after {
                        firstMethodOrNull { name = "getClearAllButton" }?.of(instance)
                            ?.invoke<View>()?.isVisible = false
                    }
                }
            }
    }
}
