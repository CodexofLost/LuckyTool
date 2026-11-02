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
object RemoveLockScreenBottomSOSButton : YukiBaseHooker() {
    override fun onHook() {
        //Source OplusEmergencyButtonControllerExImpl / OplusEmergencyButtonExImpl
        VariousClass(
            "com.oplus.systemui.keyguard.OplusEmergencyButtonControllerExImpl",
            "com.oplus.systemui.keyguard.OplusEmergencyButtonControllExImpl", //C13 legacy typo
            "com.oplus.keyguard.OplusEmergencyButtonControllerExImpl", // C16
            "com.oplus.keyguard.OplusEmergencyButtonExImpl", //C14 C15 C16
            "com.android.keyguard.OplusEmergencyButtonControllerEx"
        ).toClassOrNull()?.resolve()?.apply {
            firstMethodOrNull { name = "disableShowEmergencyButton" }?.hook {
                intercept(true)
            }
            firstMethodOrNull { name = "shouldUpdateEmergencyCallButton" }?.hook {
                before {
                    firstFieldOrNull { name = "mEmergencyButton" }?.of(instance)?.get<View>()
                        ?.isVisible = false
                    result = true
                }
            }
            firstMethodOrNull { name = "updateEmergencyCallButton" }?.hook {
                before {
                    if (args.isNotEmpty()) {
                        (firstArg() as? View)?.isVisible = false
                    }
                    if (args.size > 1) {
                        lastArg().set(false)
                    }
                }
            }
        }
    }
}
