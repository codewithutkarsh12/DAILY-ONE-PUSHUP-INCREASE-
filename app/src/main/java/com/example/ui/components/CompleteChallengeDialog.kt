package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Confirmation dialog shown when the user taps to log or complete today's push-ups.
 *
 * Rules enforced:
 * - A challenge is complete ONLY when the user confirms that at least [targetReps] have been achieved.
 * - If a lower rep count is entered, it cannot be treated as a completed target; the user is shown a clear
 *   validation message and may optionally save the lower count as partial progress toward today's target.
 */
@Composable
fun CompleteChallengeDialog(
    targetReps: Int,
    currentPartialReps: Int?,
    onDismiss: () -> Unit,
    onConfirmComplete: (actualReps: Int?) -> Unit,
    onSavePartialProgress: (partialReps: Int) -> Unit
) {
    var actualRepsText by rememberSaveable {
        mutableStateOf(
            if (currentPartialReps != null && currentPartialReps > 0) currentPartialReps.toString() else ""
        )
    }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }

    val parsedInput = actualRepsText.toIntOrNull()
    val isPartialCount = parsedInput != null && parsedInput in 1 until targetReps

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.confirm_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Today's target is $targetReps push-ups. Confirm completion below or enter the exact repetitions performed.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = actualRepsText,
                    onValueChange = { input ->
                        actualRepsText = input.filter { it.isDigit() }
                        validationError = null
                    },
                    label = { Text("Actual reps performed (optional)") },
                    placeholder = { Text("Default: $targetReps") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = validationError != null,
                    supportingText = {
                        if (validationError != null) {
                            Text(
                                text = validationError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (isPartialCount) {
                            Text(
                                text = "$parsedInput reps is below today's target ($targetReps). You can save it as partial progress, or reach $targetReps to complete the challenge.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text("Leave blank to confirm $targetReps push-ups completed.")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("actual_reps_input")
                )

                if (isPartialCount) {
                    OutlinedButton(
                        onClick = { onSavePartialProgress(parsedInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_partial_reps_button")
                    ) {
                        Text("Save Partial Progress ($parsedInput / $targetReps reps)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (actualRepsText.isBlank()) {
                        onConfirmComplete(null)
                    } else {
                        val parsed = actualRepsText.toIntOrNull()
                        when {
                            parsed == null -> {
                                validationError = "Enter a valid number."
                            }
                            parsed < targetReps -> {
                                validationError = "Target not met: at least $targetReps push-ups are required to complete today's challenge."
                            }
                            parsed > 10000 -> {
                                validationError = "Please enter a realistic rep count (≤ 10,000)."
                            }
                            else -> {
                                onConfirmComplete(parsed)
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("confirm_done_button")
            ) {
                Text(stringResource(R.string.confirm_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_done_button")
            ) {
                Text(stringResource(R.string.confirm_dialog_cancel))
            }
        }
    )
}
