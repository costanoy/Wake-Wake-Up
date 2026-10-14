package com.wakewakeup.ui

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import com.wakewakeup.WakeWakeUpApplication
import com.wakewakeup.testing.advance
import java.time.Duration

/** A typical phone screen (Robolectric's default is a small 320×470 dp one). */
const val PHONE = "w411dp-h891dp-xxhdpi"

val context: WakeWakeUpApplication get() = ApplicationProvider.getApplicationContext()

fun str(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)

/**
 * Waits for [condition] the way a phone would get there: the main thread keeps running (ViewModel
 * coroutines resuming after a database call are posted to it) and Compose keeps recomposing.
 * Compose's own waitUntil only advances Compose's clock, so app work posted to the main
 * thread would never run while it waits.
 */
fun ComposeTestRule.waitFor(message: String = "condition", timeoutMs: Long = 15_000, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (true) {
        advance(Duration.ofMillis(16))
        waitForIdle()
        if (condition()) return
        if (System.currentTimeMillis() > deadline) {
            val screen = runCatching { onAllNodes(isRoot()).printToString(maxDepth = Int.MAX_VALUE) }.getOrDefault("?")
            throw AssertionError("Timed out waiting for $message. Screen:\n$screen")
        }
        Thread.sleep(5)
    }
}

fun ComposeTestRule.waitForText(text: String, substring: Boolean = false) =
    waitFor("text \"$text\"") { onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }

fun ComposeTestRule.waitForDescription(description: String) =
    waitFor("description \"$description\"") { onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty() }

fun ComposeTestRule.waitForNoText(text: String, substring: Boolean = false) =
    waitFor("no text \"$text\"") { onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isEmpty() }

fun hasStateDescription(value: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

fun SemanticsNodeInteraction.stateDescription(): String? =
    fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)
