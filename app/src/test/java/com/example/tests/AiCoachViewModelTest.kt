package com.example.tests

import com.example.ai.AiCoachViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCoachViewModelTest {

    @Test
    fun `initial state has welcome message and idle status`() {
        val viewModel = AiCoachViewModel()
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.isStreaming)
        assertNull(state.errorMessage)
        assertEquals(1, state.conversation.size)
        assertFalse(state.conversation.first().isUser)
        assertTrue(state.conversation.first().text.contains("Gemini 3.8 Flash"))
    }

    @Test
    fun `clearConversation resets conversation to clean welcome state`() {
        val viewModel = AiCoachViewModel()
        viewModel.clearConversation()

        val state = viewModel.uiState.value
        assertEquals(1, state.conversation.size)
        assertFalse(state.conversation.first().isUser)
        assertNull(state.errorMessage)
        assertEquals("", state.streamingChunk)
    }

    @Test
    fun `dismissError clears error message`() {
        val viewModel = AiCoachViewModel()
        viewModel.dismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
