package com.luckyzyx.luckytool.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageRegistry
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 冻结「ScopePageRegistry 可达性判据与自渲染标识」语义。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScopePageReachabilityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `a page with neither DSL rows nor the selfRendered flag is not visible`() {
        val empty = ScopePageSpec(
            pageKey = "test_empty_spec",
            prefsName = ModulePrefs,
            packName = "",
            scopes = arrayOf(),
            restartEnabled = false,
        ) { }
        assertFalse(
            "既无 DSL 行也没声明自渲染的页仍应按空索引隐藏",
            ScopePageRegistry.hasVisibleContent(context, empty),
        )
    }

    @Test
    fun `the selfRendered flag alone makes an empty page visible`() {
        val special = ScopePageSpec(
            pageKey = "test_self_rendered_spec",
            prefsName = ModulePrefs,
            packName = "",
            scopes = arrayOf(),
            restartEnabled = false,
            selfRendered = true,
        ) { }
        assertTrue(ScopePageRegistry.buildIndex(context, special).isEmpty())
        assertTrue(
            "声明自渲染的页不应被空索引判为不可见",
            ScopePageRegistry.hasVisibleContent(context, special),
        )
    }

    @Test
    fun `unregistered page keys stay reachable`() {
        assertTrue(
            "未注册目标不拦截（保持旧行为）",
            ScopePageRegistry.isPageReachable(context, "not_registered_page"),
        )
    }
}
