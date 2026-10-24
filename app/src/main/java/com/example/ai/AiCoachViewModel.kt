package com.example.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AiCoachViewModel"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val TIMEOUT_MILLIS = 60_000L

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class AiCoachUiState(
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingChunk: String = "",
    val conversation: List<ChatMessage> = emptyList(),
    val errorMessage: String? = null,
    val lastPrompt: String? = null
)

class AiCoachViewModel : ViewModel() {

    // Lazy initialization of Gemini 3.8 Flash model via Firebase AI Logic
    private val model by lazy {
        Firebase.ai.generativeModel(modelName = MODEL_NAME)
    }

    private val _uiState = MutableStateFlow(AiCoachUiState())
    val uiState: StateFlow<AiCoachUiState> = _uiState.asStateFlow()

    init {
        // Welcome message with initial guidance
        _uiState.update { current ->
            current.copy(
                conversation = listOf(
                    ChatMessage(
                        isUser = false,
                        text = "Hello! I am your RepRise AI Coach, powered by Gemini 3.8 Flash. I can help you break down today's push-up target into manageable sets, improve your biomechanics & form, avoid wrist or shoulder strain, and provide personalized recovery advice. How can I help you train today?"
                    )
                )
            )
        }
    }

    /**
     * Sends a question or prompt to Gemini 3.8 Flash using streaming to minimize perceived latency.
     */
    fun sendPrompt(userPrompt: String, contextInfo: String = "") {
        val trimmed = userPrompt.trim()
        if (trimmed.isBlank() || _uiState.value.isLoading) return

        val userMessage = ChatMessage(isUser = true, text = trimmed)
        val fullPrompt = if (contextInfo.isNotBlank()) {
            "User context: $contextInfo\n\nUser request: $trimmed\n\nPlease give concise, encouraging, athletic-science-grounded advice for a daily push-up streak practitioner."
        } else {
            "User request: $trimmed\n\nPlease give concise, encouraging, athletic-science-grounded advice for a daily push-up streak practitioner."
        }

        _uiState.update { current ->
            current.copy(
                isLoading = true,
                isStreaming = true,
                streamingChunk = "",
                errorMessage = null,
                lastPrompt = trimmed,
                conversation = current.conversation + userMessage
            )
        }

        viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                withTimeout(TIMEOUT_MILLIS) {
                    model.generateContentStream(fullPrompt).collect { chunk ->
                        val text = chunk.text.orEmpty()
                        accumulated.append(text)
                        _uiState.update { current ->
                            current.copy(streamingChunk = accumulated.toString())
                        }
                    }
                }

                val finalOutput = accumulated.toString().trim()
                if (finalOutput.isNotEmpty()) {
                    val coachMessage = ChatMessage(isUser = false, text = finalOutput)
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            isStreaming = false,
                            streamingChunk = "",
                            conversation = current.conversation + coachMessage
                        )
                    }
                } else {
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            isStreaming = false,
                            errorMessage = "Gemini returned an empty response. Please try again."
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini generation timed out", e)
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        isStreaming = false,
                        errorMessage = "Request timed out after 60 seconds. Please check your network and try again."
                    )
                }
            } catch (e: CancellationException) {
                // ALWAYS re-throw CancellationException to maintain coroutine cooperative cancellation
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini generation failed", e)
                val displayMsg = e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Gemini API connection error. Please verify network access."
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        isStreaming = false,
                        errorMessage = displayMsg
                    )
                }
            }
        }
    }

    /**
     * Generates a tailored pacing breakdown for today's target (e.g. 15 reps) based on energy level.
     */
    fun generatePacingPlan(
        todayTarget: Int,
        activeStreak: Int,
        fatigueLevel: String
    ) {
        val prompt = "Today my daily target is $todayTarget push-ups. My active streak is $activeStreak days. My current energy level is: $fatigueLevel. Please provide a clear, practical set & rest breakdown (e.g. Set 1, Set 2, rest intervals in seconds) and 2 key form cues to help me finish strong today."
        sendPrompt(
            userPrompt = prompt,
            contextInfo = "Today's Target: $todayTarget reps, Active Streak: $activeStreak days, Energy: $fatigueLevel"
        )
    }

    fun retryLastPrompt() {
        val last = _uiState.value.lastPrompt ?: return
        sendPrompt(last)
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearConversation() {
        _uiState.update { current ->
            current.copy(
                conversation = listOf(
                    ChatMessage(
                        isUser = false,
                        text = "Conversation cleared. How can I assist your push-up training today?"
                    )
                ),
                errorMessage = null,
                streamingChunk = ""
            )
        }
    }
}
