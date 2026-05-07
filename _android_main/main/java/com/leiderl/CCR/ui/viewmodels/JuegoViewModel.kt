package com.leiderl.CCR.ui.viewmodels

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.leiderl.CCR.data.database.entities.PreguntaTrivia
import com.leiderl.CCR.data.repository.JuegoRepository
import com.leiderl.CCR.data.repository.ResultadoScript
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import org.json.JSONArray

data class RankingJugador(val nombre: String, val puntaje: Int)
data class RankingData(
    val top_facil:   List<RankingJugador>,
    val top_medio:   List<RankingJugador>,
    val top_dificil: List<RankingJugador>
)

class JuegoViewModel(app: Application) : AndroidViewModel(app) {

    val repo = JuegoRepository(app)

    private val _estado = MutableLiveData<EstadoJuego>(EstadoJuego.Verificando)
    val estado: LiveData<EstadoJuego> = _estado

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Cargando solo para operaciones de red (registrar, obtener preguntas)
    // NO se usa en la pantalla de dificultad para no tapar el ranking
    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    private val _nombreJugador = MutableLiveData<String>()
    val nombreJugador: LiveData<String> = _nombreJugador

    private val _partidas = MutableLiveData(0)
    val partidas: LiveData<Int> = _partidas

    private val _ranking = MutableLiveData<RankingData?>()
    val ranking: LiveData<RankingData?> = _ranking

    private val _preguntas = MutableLiveData<List<PreguntaTrivia>>()
    val preguntas: LiveData<List<PreguntaTrivia>> = _preguntas

    private val _preguntaActual = MutableLiveData(0)
    val preguntaActual: LiveData<Int> = _preguntaActual

    private val _tiempoMs = MutableLiveData(60_000L)
    val tiempoMs: LiveData<Long> = _tiempoMs

    private val _respuestaSeleccionada = MutableLiveData<String?>()
    val respuestaSeleccionada: LiveData<String?> = _respuestaSeleccionada

    private var timer: CountDownTimer? = null
    private val TIEMPO_TOTAL_MS = 60_000L

    private var correctas        = 0
    private var dificultadActual = "facil"

    init {
        // Al arrancar: verificar silenciosamente si ya está registrado
        // mientras tanto se muestra EstadoJuego.Verificando (solo logo + spinner)
        verificarRegistro()
    }

    private fun verificarRegistro() {
        if (repo.esJugadorRegistrado()) {
            // Ya registrado — cargar datos y ranking en paralelo
            viewModelScope.launch {
                val datosDeferred  = async { repo.obtenerJugador() }
                val rankingDeferred = async { repo.obtenerRanking() }

                val datosResult = datosDeferred.await()
                if (datosResult is ResultadoScript.Exito) {
                    _partidas.value    = datosResult.data.optInt("partidas")
                    _nombreJugador.value = repo.obtenerNombreLocal()
                }

                val rankingResult = rankingDeferred.await()
                if (rankingResult is ResultadoScript.Exito) {
                    _ranking.value = RankingData(
                        parsearLista(rankingResult.data.getJSONArray("top_facil"),   "p_facil"),
                        parsearLista(rankingResult.data.getJSONArray("top_medio"),   "p_medio"),
                        parsearLista(rankingResult.data.getJSONArray("top_dificil"), "p_dificil")
                    )
                }

                // Mostrar pantalla de dificultad solo cuando todo esté listo
                _estado.value = EstadoJuego.SeleccionDificultad
            }
        } else {
            // No registrado — mostrar formulario
            _estado.value = EstadoJuego.Inicio
        }
    }

    // ── Registro ──────────────────────────────────────────────────────────────
    fun registrar(nombre: String, apellido: String) {
        if (nombre.isBlank() || apellido.isBlank()) { _error.value = "Ingresá nombre y apellido"; return }
        _cargando.value = true
        viewModelScope.launch {
            when (val r = repo.registrarJugador(nombre.trim(), apellido.trim())) {
                is ResultadoScript.Exito -> {
                    _nombreJugador.value = "${nombre.trim()} ${apellido.trim()}"
                    // Cargar ranking en paralelo mientras transicionamos
                    launch { cargarRanking() }
                    _estado.value = EstadoJuego.SeleccionDificultad
                }
                is ResultadoScript.Error -> _error.value = r.mensaje
            }
            _cargando.value = false
        }
    }

    // ── Ranking — solo actualiza el LiveData, sin tocar _cargando ────────────
    fun cargarRanking() {
        viewModelScope.launch {
            when (val r = repo.obtenerRanking()) {
                is ResultadoScript.Exito -> {
                    _ranking.value = RankingData(
                        parsearLista(r.data.getJSONArray("top_facil"),   "p_facil"),
                        parsearLista(r.data.getJSONArray("top_medio"),   "p_medio"),
                        parsearLista(r.data.getJSONArray("top_dificil"), "p_dificil")
                    )
                }
                is ResultadoScript.Error -> { /* ranking falla silenciosamente */ }
            }
        }
    }

    private fun parsearLista(arr: JSONArray, campo: String): List<RankingJugador> {
        val lista = mutableListOf<RankingJugador>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            lista.add(RankingJugador(obj.optString("nombre", "-"), obj.optInt(campo, 0)))
        }
        return lista
    }

    // ── Iniciar juego — preguntas locales, arranque instantáneo ─────────────
    fun iniciarJuego(dificultad: String) {
        dificultadActual      = dificultad
        correctas             = 0
        // Cargar desde JSON local — sin red, sin espera
        val lista = repo.obtenerPreguntasLocales(dificultad)
        _preguntas.value      = lista
        _preguntaActual.value = 0
        _estado.value         = EstadoJuego.Jugando
        iniciarTimerGlobal()
    }

    // ── Timer global 60s ──────────────────────────────────────────────────────
    private fun iniciarTimerGlobal() {
        timer?.cancel()
        _tiempoMs.value = TIEMPO_TOTAL_MS
        timer = object : CountDownTimer(TIEMPO_TOTAL_MS, 100) {
            override fun onTick(msRestantes: Long) { _tiempoMs.value = msRestantes }
            override fun onFinish() {
                _tiempoMs.value = 0
                calcularYGuardarPuntaje(
                    preguntasRespondidas = _preguntaActual.value ?: 0,
                    tiempoUsadoMs        = TIEMPO_TOTAL_MS
                )
            }
        }.start()
    }

    // ── Responder ─────────────────────────────────────────────────────────────
    fun responder(opcion: String) {
        val pregs = _preguntas.value ?: return
        if (pregs.isEmpty()) return
        val idx = _preguntaActual.value ?: return

        if (opcion == pregs[idx].correcta) correctas++

        val siguiente = idx + 1
        if (siguiente < pregs.size) {
            _preguntaActual.value = siguiente
        } else {
            timer?.cancel()
            val tiempoUsado = TIEMPO_TOTAL_MS - (_tiempoMs.value ?: 0L)
            calcularYGuardarPuntaje(preguntasRespondidas = pregs.size, tiempoUsadoMs = tiempoUsado)
        }
    }

    // ── Fórmula ───────────────────────────────────────────────────────────────
    private fun calcularYGuardarPuntaje(preguntasRespondidas: Int, tiempoUsadoMs: Long) {
        val R = preguntasRespondidas.coerceAtLeast(1)
        val C = correctas
        val t = tiempoUsadoMs / 1000.0
        val T = 60.0

        val fp = Math.pow(C.toDouble() / R, 1.5)
        val fc = Math.pow(R.toDouble() / 10.0, 1.2)
        val ft = Math.exp(-2.0 * t / T)

        val puntaje = (1000.0 * fp * fc * ft).toInt().coerceIn(0, 1000)

        // Mostrar resultado inmediatamente — sin esperar la red
        _estado.value   = EstadoJuego.Resultado(puntaje, dificultadActual)
        _partidas.value = (_partidas.value ?: 0) + 1

        // Guardar en servidor en background — el usuario ya vio su resultado
        viewModelScope.launch {
            repo.sumarPuntaje(puntaje, dificultadActual)
        }
    }

    fun reiniciar() {
        _estado.value                = EstadoJuego.SeleccionDificultad
        _respuestaSeleccionada.value = null
        // Refrescar ranking al volver
        cargarRanking()
    }

    fun clearError() { _error.value = null }

    override fun onCleared() { timer?.cancel(); super.onCleared() }
}

sealed class EstadoJuego {
    object Verificando          : EstadoJuego()   // ← nuevo: spinner inicial
    object Inicio               : EstadoJuego()   // formulario registro
    object SeleccionDificultad  : EstadoJuego()
    object Jugando              : EstadoJuego()
    data class Resultado(val puntaje: Int, val dificultad: String) : EstadoJuego()
}