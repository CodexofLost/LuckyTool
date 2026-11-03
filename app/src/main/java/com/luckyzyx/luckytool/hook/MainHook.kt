package com.luckyzyx.luckytool.hook

import android.annotation.SuppressLint
import com.highcapable.yukihookapi.annotation.xposed.YukiHookLibXposedEntry
import com.highcapable.yukihookapi.hook.factory.configure
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.YukiHookXposedModule
import com.luckyzyx.luckytool.hook.hookers.HookAndroid
import com.luckyzyx.luckytool.hook.hookers.HookLauncher
import com.luckyzyx.luckytool.hook.hookers.HookSystemUI
import org.lsposed.lsparanoid.Obfuscate
import java.io.File

@YukiHookLibXposedEntry(
    entryClassName = "Entry",
    minApiVersion = 102,
    targetApiVersion = 102,
    hotReload = YukiHookLibXposedEntry.HotReload.AUTO,
    scope = [
        "system",
        "com.android.systemui",
        "com.android.launcher",
        "com.oppo.launcher",
    ],
)
@Obfuscate
class MainHook : YukiHookXposedModule {

    override fun onInit() {
        configure {
            logging {
                tag = MainHook.TAG
            }
        }
    }

    companion object {
        const val TAG = "LuckyTool"

        @SuppressLint("SdCardPath")
        fun isMasterEnabled(): Boolean {
            return try {
                File("/sdcard/disable_lt").exists()
            } catch (_: Throwable) {
                false
            }
        }
    }

    override fun onHook() = encase {
        if (isMasterEnabled()) return@encase

        // 系统框架 (Global Blur & Animation Tiers)
        loadSystem(HookAndroid)

        // 系统界面 (QS & Notification Shade Dynamic Blur)
        loadApp("com.android.systemui", HookSystemUI)

        // 桌面 (Dock & Folder Dynamic Blur, Flagship Animations)
        loadApp("com.oppo.launcher", HookLauncher)
        loadApp("com.android.launcher", HookLauncher)
    }
}
