package dev.xboyrex.novaplayer

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.xboyrex.novaplayer.core.model.Video
import dev.xboyrex.novaplayer.core.ui.base.DataState
import dev.xboyrex.novaplayer.core.ui.theme.NovaPlayerTheme
import dev.xboyrex.novaplayer.feature.more.screens.more.MoreAction
import dev.xboyrex.novaplayer.feature.more.screens.more.MoreScreenContent
import dev.xboyrex.novaplayer.feature.more.screens.more.MoreUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoreScreenHistoryTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyAndDisabledHistoryShowTheirMessagesAndKeepHistoryAccessible() {
        val state =
            mutableStateOf(value = MoreUiState(history = DataState.Success(value = emptyList())))
        val actions = mutableListOf<MoreAction>()
        composeRule.setContent {
            NovaPlayerTheme {
                MoreScreenContent(
                    state = state.value,
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithText(text = "No watch history").assertExists()
        composeRule.onNodeWithContentDescription(label = "History").performClick()
        composeRule.runOnIdle {
            state.value = state.value.copy(
                history = DataState.Success(value = listOf(Video.sample)),
                preferences = state.value.preferences.copy(isHistoryPaused = true),
            )
        }
        composeRule.onNodeWithText(text = "Watch history is turned off").assertExists()
        composeRule.onNodeWithText(text = "No watch history").assertDoesNotExist()
        composeRule.onNodeWithText(Video.sample.displayName).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(label = "History").performClick()
        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    MoreAction.OpenHistory,
                    MoreAction.OpenHistory,
                ),
                actions,
            )
            state.value = state.value.copy(
                history = DataState.Success(value = emptyList()),
                preferences = state.value.preferences.copy(isHistoryPaused = false),
            )
        }
        composeRule.onNodeWithText(text = "No watch history").assertExists()
        composeRule.onNodeWithText(text = "Watch history is turned off").assertDoesNotExist()
    }
}
