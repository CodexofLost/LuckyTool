package com.luckyzyx.luckytool.hook

import android.annotation.SuppressLint
import com.highcapable.yukihookapi.annotation.xposed.YukiHookLibXposedEntry
import com.highcapable.yukihookapi.hook.factory.configure
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.YukiHookXposedModule
import com.luckyzyx.luckytool.hook.hookers.HookAndroid
import com.luckyzyx.luckytool.hook.hookers.HookLauncher
import com.luckyzyx.luckytool.hook.hookers.HookSystemUI
import com.luckyzyx.luckytool.hook.hookers.HookUXDesign
import com.luckyzyx.luckytool.hook.hookers.HookWallpapers
import org.lsposed.lsparanoid.Obfuscate
import java.io.File

@YukiHookLibXposedEntry(
    entryClassName = "Entry",
    minApiVersion = 102,
    targetApiVersion = 102,
    hotReload = YukiHookLibXposedEntry.HotReload.AUTO,
    scope = [
        "android",
        "system",
        "com.android.systemui",
        "com.android.launcher",
        "com.oppo.launcher",
        "com.oplus.wallpapers",
        "com.oplus.uxdesign",
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
        const val TAG = "RUI Tool"

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

        // 壁纸与个性化 (Live Wallpaper Blur, Motion & Style Filters)
        loadApp("com.oplus.wallpapers", HookWallpapers)

        // 个性化与材质模糊设置 (Personalization & System Material Blur Settings)
        loadApp("com.oplus.uxdesign", HookUXDesign)
    }
}
