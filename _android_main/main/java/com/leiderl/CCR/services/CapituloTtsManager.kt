package com.leiderl.CCR.services

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class CapituloTtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var listo = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                listo = true
            }
        }
    }

    fun hablar(textos: List<String>, onTerminado: () -> Unit = {}) {
        if (!listo || textos.isEmpty()) return
        tts?.stop()

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onError(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (utteranceId == "ultimo") onTerminado()
            }
        })

        textos.forEachIndexed { i, texto ->
            val id = if (i == textos.lastIndex) "ultimo" else "item_$i"
            val modo = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(texto, modo, null, id)
        }
    }

    fun detener() {
        tts?.stop()
    }

    fun estaHablando(): Boolean = tts?.isSpeaking == true

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        listo = false
    }
}