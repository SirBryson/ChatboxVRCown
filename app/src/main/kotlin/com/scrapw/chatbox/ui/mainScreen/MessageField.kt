package com.scrapw.chatbox.ui.mainScreen

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.scrapw.chatbox.R
import com.scrapw.chatbox.data.SettingsStates
import com.scrapw.chatbox.speech.ContinuousSpeechRecognizer
import com.scrapw.chatbox.ui.ChatboxViewModel
import com.scrapw.chatbox.ui.common.HapticConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest


@Composable
fun MessageField(
    chatboxViewModel: ChatboxViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    val speechRecognizer = remember(chatboxViewModel) {
        ContinuousSpeechRecognizer(
            context = context,
            onPartialResult = { chatboxViewModel.onSpeechPartial(it) },
            onFinalResult = { chatboxViewModel.onSpeechFinal(it) },
            onListeningChanged = { isListening = it },
            onUnavailable = {
                Toast.makeText(
                    context,
                    context.getString(R.string.speech_recognition_unavailable),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            speechRecognizer.start()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.microphone_permission_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    DisposableEffect(speechRecognizer) {
        onDispose { speechRecognizer.destroy() }
    }

    Crossfade(
        targetState = chatboxViewModel.isAddressResolvable.value, label = "LockIpFieldCrossfade"
    ) { resolvable ->
        Row(
            modifier = modifier
                .height(72.dp)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            if (resolvable) {
                TextField(
                    value = chatboxViewModel.messageText.value,
                    onValueChange = {
                        chatboxViewModel.onMessageTextChange(it)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp)),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.write_a_message)) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            chatboxViewModel.ipAddressLocked = true
                            chatboxViewModel.sendMessage()
                        }
                    )
                )
            } else {
                TextField(
                    value = stringResource(R.string.invalid_ip_address),
                    onValueChange = {},
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp)),
                    textStyle = TextStyle.Default.copy(
                        fontStyle = FontStyle.Italic
                    ),
//                    colors = TextFieldDefaults.colors(
//                        disabledTextColor = MaterialTheme.colorScheme.error
//                    ),
                    enabled = false
                )
            }


            val view = LocalView.current
            val buttonHapticState = SettingsStates.buttonHapticState()


            val onSendClick = {
                chatboxViewModel.ipAddressLocked = true
                chatboxViewModel.sendMessage()
                if (buttonHapticState.value) {
                    view.performHapticFeedback(HapticConstants.send)
                }
            }

            val onSendLongClick = {
                chatboxViewModel.ipAddressLocked = true
                chatboxViewModel.stashMessage()
                if (buttonHapticState.value) {
                    view.performHapticFeedback(HapticConstants.send)
                }
            }


            val interactionSource = remember { MutableInteractionSource() }
            val viewConfiguration = LocalViewConfiguration.current

            Button(
                onClick = {
                    if (isListening) {
                        speechRecognizer.stop()
                    } else if (
                        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        speechRecognizer.start()
                    } else {
                        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.fillMaxHeight(),
                enabled = resolvable
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = stringResource(
                        if (isListening) R.string.stop_voice_input else R.string.start_voice_input
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }

            Button(
                onClick = {},
                modifier = Modifier.fillMaxHeight(),
                interactionSource = interactionSource,
                enabled = resolvable
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.Send,
                    contentDescription = stringResource(R.string.send),
                    modifier = Modifier.size(24.dp)
                )
            }

            LaunchedEffect(interactionSource) {
                var isLongClick = false

                interactionSource.interactions.collectLatest { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> {
                            isLongClick = false
                            delay(viewConfiguration.longPressTimeoutMillis)
                            isLongClick = true
                            onSendLongClick()
                        }

                        is PressInteraction.Release -> {
                            if (!isLongClick) {
                                onSendClick()
                            }
                        }
                    }
                }
            }

        }
    }
}
