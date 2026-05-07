package com.leiderl.CCR.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.*

class VoiceRecognitionManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onStateChanged: (Boolean) -> Unit, // true = escuchando
    private val onNotAvailable: (() -> Unit)? = null // nuevo: callback si el servicio no existe
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var shouldRestart = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ─────────────────────────────────────────────────────────────────────────
    // ESTRATEGIA:
    //
    // onPartialResults se llama continuamente mientras el usuario habla.
    // En vez de esperar silencio (debounce largo), buscamos INMEDIATAMENTE
    // en cuanto el texto parcial tiene suficiente contenido.
    //
    // Para evitar lanzar una búsqueda por CADA sílaba usamos un debounce muy
    // corto (150 ms): si llegan dos parciales seguidos, solo procesamos el
    // último. Esto NO requiere silencio — simplemente agrupa ráfagas rápidas.
    //
    // El texto que se manda a onResult va CRECIENDO palabra a palabra:
    //   "porque"  → muy corto, ignorar
    //   "porque de tal"  → 3 palabras, buscar
    //   "porque de tal manera"  → 4 palabras, buscar (puede mejorar la sugerencia)
    //
    // SuggestionManager buscará primero en famosos, luego FTS. Si ya tenía una
    // sugerencia de prioridad 1, puede quedarse igual o mejorar.
    // ─────────────────────────────────────────────────────────────────────────

    // Umbral mínimo de palabras para lanzar una búsqueda desde parciales
    private val MIN_PALABRAS = 3

    // Debounce corto: sugiere rápido, la siguiente sugerencia mejorará si es necesario
    private val PARTIAL_DEBOUNCE_MS = 200L

    private var partialJob: Job? = null
    private var lastPartialEnviado: String = ""
    private var lastFinalProcessed: String = ""

    fun startListening() {
        if (isListening) return
        shouldRestart = true
        lastFinalProcessed = ""
        lastPartialEnviado = ""
        initAndStart()
    }

    fun stopListening() {
        shouldRestart = false
        isListening = false
        partialJob?.cancel()
        scope.launch {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
        onStateChanged(false)
    }

    private fun initAndStart() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("VoiceManager", "Speech recognition no disponible")
            // ── FIX: antes retornaba silencioso, dejando el botón trabado.
            // Ahora resetea el estado y notifica para mostrar un mensaje al usuario.
            onStateChanged(false)
            onNotAvailable?.invoke()
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    onStateChanged(true)
                    Log.d("VoiceManager", "Listo para escuchar")
                }

                override fun onBeginningOfSpeech() {
                    Log.d("VoiceManager", "Inicio de habla")
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d("VoiceManager", "Fin de habla detectado")
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH      -> "Sin coincidencia"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo agotado"
                        SpeechRecognizer.ERROR_AUDIO         -> "Error de audio"
                        SpeechRecognizer.ERROR_NETWORK       -> "Error de red"
                        else -> "Error $error"
                    }
                    Log.d("VoiceManager", "Error: $errorMsg")

                    if (shouldRestart) {
                        scope.launch {
                            delay(500L)
                            if (shouldRestart) initAndStart()
                        }
                    } else {
                        onStateChanged(false)
                    }
                }

                // ── RESULTADO FINAL ─────────────────────────────────────────
                // Llega cuando el motor detecta silencio o fin de sesión.
                // El texto final suele ser más preciso que el parcial.
                // Lo procesamos solo si difiere del último texto que ya enviamos
                // desde onPartialResults (para no duplicar búsquedas).
                override fun onResults(results: Bundle?) {
                    isListening = false
                    partialJob?.cancel()

                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val texto = matches?.firstOrNull()

                    if (!texto.isNullOrBlank() && texto != lastPartialEnviado) {
                        Log.d("VoiceManager", "Resultado final (nuevo): $texto")
                        lastFinalProcessed = texto
                        onResult(texto)
                    } else if (!texto.isNullOrBlank()) {
                        Log.d("VoiceManager", "Resultado final igual al último parcial, ignorando")
                    }

                    if (shouldRestart) {
                        scope.launch {
                            delay(300L)
                            if (shouldRestart) {
                                lastPartialEnviado = ""
                                lastFinalProcessed = ""
                                initAndStart()
                            }
                        }
                    }
                }

                // ── RESULTADOS PARCIALES ─────────────────────────────────────
                // Se llaman continuamente mientras el usuario habla.
                // Buscamos INMEDIATAMENTE si hay suficientes palabras,
                // con un debounce mínimo solo para agrupar ráfagas.
                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?: return

                    Log.d("VoiceManager", "Parcial recibido: \"$partial\"")

                    // Ignorar si tiene muy pocas palabras
                    val palabras = partial.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
                    if (palabras.size < MIN_PALABRAS) {
                        Log.d("VoiceManager", "Parcial muy corto (${palabras.size} palabras), ignorando")
                        return
                    }

                    // Ignorar si es exactamente igual al último que ya procesamos
                    if (partial == lastPartialEnviado) {
                        Log.d("VoiceManager", "Parcial duplicado, ignorando")
                        return
                    }

                    // Cancelar el job anterior y lanzar uno nuevo con debounce mínimo
                    // Este debounce (150ms) solo agrupa parciales que llegan en ráfaga
                    // inmediata. NO requiere silencio del usuario.
                    partialJob?.cancel()
                    partialJob = scope.launch {
                        delay(PARTIAL_DEBOUNCE_MS)
                        Log.d("VoiceManager", "Procesando parcial: \"$partial\"")
                        lastPartialEnviado = partial
                        onResult(partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

            // ── IMPORTANTE: estos valores controlan cuándo el motor considera
            // que el usuario terminó de hablar y manda onResults.
            // En un entorno con audio continuo, el silencio nunca llega,
            // por eso NO dependemos de onResults para sugerir. Igual dejamos
            // valores generosos para cuando SÍ haya silencio.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        }

        speechRecognizer?.startListening(intent)
    }

    fun destroy() {
        shouldRestart = false
        isListening = false
        partialJob?.cancel()
        scope.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}