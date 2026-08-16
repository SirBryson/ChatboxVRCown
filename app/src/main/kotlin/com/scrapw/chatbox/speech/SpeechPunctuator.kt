package com.scrapw.chatbox.speech

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.OnlinePunctuation
import com.k2fsa.sherpa.onnx.OnlinePunctuationConfig
import com.k2fsa.sherpa.onnx.OnlinePunctuationModelConfig

/** Adds English punctuation and true-casing locally without replacing Samsung STT. */
class SpeechPunctuator(context: Context) {

    private val punctuation = OnlinePunctuation(
        assetManager = context.assets,
        config = OnlinePunctuationConfig(
            model = OnlinePunctuationModelConfig(
                cnnBilstm = "punctuation/model.int8.onnx",
                bpeVocab = "punctuation/bpe.vocab",
                numThreads = 1,
                debug = false,
                provider = "cpu"
            )
        )
    )

    fun transform(text: String): String = runCatching {
        punctuation.addPunctuation(text)
    }.onFailure {
        Log.e("SpeechPunctuator", "Punctuation inference failed", it)
    }.getOrDefault(text)

    fun release() {
        punctuation.release()
    }
}
