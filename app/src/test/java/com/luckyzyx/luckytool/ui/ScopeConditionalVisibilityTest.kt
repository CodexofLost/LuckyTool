package com.luckyzyx.luckytool.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.luckyzyx.luckytool.ui.compose.components.PrefScopeBuilder
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.PrefState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 冻结「条件条目随偏好写入自动重算」这一语义（不再依赖 Activity recreate）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScopeConditionalVisibilityTest {

    private lateinit var context: Context
    private lateinit var raw: SharedPreferences
    private lateinit var state: PrefState

    private val testSpec = ScopePageSpec(
        pageKey = "test_conditional_page",
        prefsName = "scope_conditional_test",
        packName = "android",
    ) {
        switch(key = "driver_pref", title = "Driver")
        if (state.getBoolean("driver_pref", false)) {
            switch(key = "conditional_row", title = "Conditional Row")
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        raw = context.getSharedPreferences("scope_conditional_test", Context.MODE_PRIVATE)
        raw.edit().clear().commit()
        state = PrefState.ofProvider { raw }
    }

    private fun buildPage(): Set<String> {
        val builder = PrefScopeBuilder(state)
        builder.context = context
        builder.beginBuild()
        testSpec.content(builder)
        builder.computeSegments()
        return builder.entries.mapNotNull { it.indexKey }.toSet()
    }

    @Test
    fun `conditional rows stay hidden while the driving pref holds the default`() {
        val keys = buildPage()
        assertTrue("驱动条目本身始终可见", "driver_pref" in keys)
        assertFalse("默认时条件条目不应出现", "conditional_row" in keys)
    }

    @Test
    fun `writing the driving pref recomputes the conditional rows`() {
        val before = state.revision.value
        assertTrue(state.set("driver_pref", true))
        assertTrue(
            "state.set 必须让 revision 前进（ScopeScreen 据此重组重跑构建 lambda）",
            state.revision.value > before,
        )

        val keys = buildPage()
        assertTrue("条件条目应在重跑后出现，无需 recreate", "conditional_row" in keys)
    }
}
