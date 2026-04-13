package com.questkeep.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.questkeep.R
import com.questkeep.auth.AuthViewModel
import com.questkeep.auth.AuthState
import com.questkeep.ui.theme.*
import com.questkeep.utils.DiceParser
import com.questkeep.viewmodel.DiceRollerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceRollerModal(
    campaignId: String?,
    onDismiss: () -> Unit,
    /** GM userId — used as targetPlayerId for private rolls so the GM sees them too */
    gmId: String? = null,
    viewModel: DiceRollerViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val expression by viewModel.expression.collectAsState()
    val lastResult by viewModel.lastResult.collectAsState()
    val error by viewModel.error.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val focusManager = LocalFocusManager.current
    val authState by authViewModel.authState.collectAsState()
    val senderName = (authState as? AuthState.Authenticated)?.user?.nickname ?: "Player"
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Rolling animation states
    var isRolling by remember { mutableStateOf(false) }
    var rollingFace by remember { mutableStateOf("🎲") }
    var showResult by remember { mutableStateOf(false) }
    val resultScale by animateFloatAsState(
        targetValue = if (showResult) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "result_scale"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.dice_roller),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            // Quick dice buttons
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DiceParser.STANDARD_DICE) { sides ->
                    OutlinedButton(
                        onClick = {
                            viewModel.quickRoll(sides)
                            triggerRollAnimation(scope, { isRolling = it }, { rollingFace = it }, { showResult = it })
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPurpleLight),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("d$sides", fontSize = 13.sp)
                    }
                }
            }

            // Expression input
            OutlinedTextField(
                value = expression,
                onValueChange = { viewModel.setExpression(it) },
                label = { Text(stringResource(R.string.dice_expression_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    viewModel.roll()
                    triggerRollAnimation(scope, { isRolling = it }, { rollingFace = it }, { showResult = it })
                }),
                isError = error != null,
                supportingText = {
                    if (error != null) Text(error!!, color = AccentRed)
                    else Text(stringResource(R.string.dice_expression_example), color = TextDisabled)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = PrimaryPurpleLight,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // Result display — shows cycling face while rolling, then bounces in the result
            if (isRolling || showResult) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, RoundedCornerShape(12.dp))
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRolling) {
                        Text(
                            text = rollingFace,
                            fontSize = 64.sp,
                            color = TextPrimary
                        )
                    } else {
                        lastResult?.let { result ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.scale(resultScale)
                            ) {
                                Text(
                                    text = result.total.toString(),
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryPurpleLight
                                )
                                Text(
                                    text = result.expression + if (result.rolls.size > 1)
                                        " [${result.rolls.joinToString(", ")}]" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Roll buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.rollAndPost(campaignId, senderName, isPublic = true)
                        triggerRollAnimation(scope, { isRolling = it }, { rollingFace = it }, { showResult = it })
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text(stringResource(R.string.roll_public), fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = {
                        focusManager.clearFocus()
                        // Pass gmId so GM can also see the private roll result
                        viewModel.rollAndPost(campaignId, senderName, isPublic = false, privateTargetId = gmId)
                        triggerRollAnimation(scope, { isRolling = it }, { rollingFace = it }, { showResult = it })
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentOrange)
                ) {
                    Text(stringResource(R.string.roll_private), fontWeight = FontWeight.SemiBold)
                }
            }

            // Presets
            if (presets.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets) { preset ->
                        InputChip(
                            selected = false,
                            onClick = { viewModel.setExpression(preset.expression) },
                            label = { Text(preset.label) }
                        )
                    }
                }
            }
        }
    }
}

fun triggerRollAnimation(
    scope: CoroutineScope,
    setIsRolling: (Boolean) -> Unit,
    setRollingFace: (String) -> Unit,
    setShowResult: (Boolean) -> Unit
) {
    scope.launch(Dispatchers.Main) {
        val faces = listOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")
        setIsRolling(true)
        setShowResult(false)
        var i = 0
        val endTime = System.currentTimeMillis() + 600
        while (System.currentTimeMillis() < endTime) {
            setRollingFace(faces[i % faces.size])
            i++
            delay(70)
        }
        setRollingFace("🎲")
        setIsRolling(false)
        setShowResult(true)
    }
}
