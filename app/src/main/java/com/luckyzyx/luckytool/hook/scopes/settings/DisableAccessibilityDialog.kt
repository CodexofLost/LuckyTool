package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DisableAccessibilityDialog : YukiBaseHooker() {
    override fun onHook() {
        //Source SpecialPermRiskConfirmHelper
        "com.oplus.settings.applications.specialaccess.SpecialPermRiskConfirmHelper".toClassOrNull()
            ?.resolve()?.apply {
                firstMethodOrNull {
                    name = "show"
                    returnType = Boolean::class
                }?.intercept {
                    val runnale = lastArg() as? Runnable
                    runnale?.run()
                    true
                }
            }
    }
}
