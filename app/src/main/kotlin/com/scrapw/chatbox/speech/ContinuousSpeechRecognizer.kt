package com.scrapw.chatbox.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Keeps Android's SpeechRecognizer alive across utterances and exposes only the
 * text for the current utterance. A completed utterance starts a fresh session,
 * so previous sentences can never leak into the next VRChat update.
 */
class ContinuousSpeechRecognizer(
    context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onListeningChanged: (Boolean) -> Unit,
    private val onUnavailable: () -> Unit,
    private val transformResult: (String) -> String = { it }
) : RecognitionListener {

    private val handler = Handler(Looper.getMainLooper())
    private val recognizer: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context).also {
                it.setRecognitionListener(this)
            }
        } else {
            null
        }

    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.US.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3_000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2_000L)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            putExtra(
                RecognizerIntent.EXTRA_ENABLE_FORMATTING,
                RecognizerIntent.FORMATTING_OPTIMIZE_QUALITY
            )
            putExtra(RecognizerIntent.EXTRA_HIDE_PARTIAL_TRAILING_PUNCTUATION, false)
        }
    }

    private var shouldListen = false
    private var sessionRunning = false
    private var destroyed = false

    fun start() {
        if (recognizer == null) {
            onUnavailable()
            return
        }
        shouldListen = true
        onListeningChanged(true)
        startSession(0)
    }

    fun stop() {
        shouldListen = false
        handler.removeCallbacksAndMessages(null)
        if (sessionRunning) recognizer?.cancel()
        sessionRunning = false
        onListeningChanged(false)
    }

    fun destroy() {
        destroyed = true
        stop()
        recognizer?.destroy()
    }

    private fun startSession(delayMillis: Long) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (!shouldListen || destroyed || sessionRunning) return@postDelayed
            try {
                sessionRunning = true
                recognizer?.startListening(intent)
            } catch (_: RuntimeException) {
                sessionRunning = false
                startSession(500)
            }
        }, delayMillis)
    }

    private fun restartSession(delayMillis: Long = 150) {
        sessionRunning = false
        if (shouldListen) startSession(delayMillis)
    }

    private fun bestResult(results: Bundle?): String =
        results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            ?.let(transformResult)
            .orEmpty()

    override fun onPartialResults(partialResults: Bundle?) {
        bestResult(partialResults).takeIf { it.isNotEmpty() }?.let(onPartialResult)
    }

    override fun onResults(results: Bundle?) {
        bestResult(results).takeIf { it.isNotEmpty() }?.let(onFinalResult)
        restartSession()
    }

    override fun onError(error: Int) {
        val delay = when (error) {
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 750L
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_SERVER -> 1_500L
            else -> 200L
        }
        restartSession(delay)
    }

    override fun onEndOfSpeech() = Unit
    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
