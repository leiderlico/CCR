package com.leiderl.CCR.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.leiderl.CCR.data.database.entities.PreguntaTrivia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

sealed class ResultadoScript {
    data class Exito(val data: JSONObject) : ResultadoScript()
    data class Error(val mensaje: String)  : ResultadoScript()
}

class JuegoRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("juego_prefs", Context.MODE_PRIVATE)

    private val SCRIPT_URL = "https://script.google.com/macros/s/AKfycbwi2SBX5TjxPSPiK2oFR6pnQv7-T-8ByXNZDOwh1OTm8VPg4IAPffLPCSO-N5tIPaCzRg/exec"

    // ── Jugador local ─────────────────────────────────────────────────────────
    fun esJugadorRegistrado(): Boolean = prefs.contains("jugador_id")
    fun obtenerNombreLocal(): String   = prefs.getString("jugador_nombre", "") ?: ""
    private fun obtenerIdLocal(): String = prefs.getString("jugador_id", "") ?: ""

    // ── Preguntas — 100% local desde JSON en res/raw ──────────────────────────
    fun obtenerPreguntasLocales(dificultad: String): List<PreguntaTrivia> {
        val resId = context.resources.getIdentifier(
            "preguntas_biblicas", "raw", context.packageName
        )
        val json  = context.resources.openRawResource(resId)
            .bufferedReader().readText()
        val arr   = JSONArray(json)
        val todas = mutableListOf<PreguntaTrivia>()

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.getString("dificultad") != dificultad) continue
            todas.add(PreguntaTrivia(
                id       = obj.getInt("id").toString(),
                pregunta = obj.getString("pregunta"),
                correcta = obj.getString("correcta"),
                opciones = listOf(
                    obj.getString("correcta"),
                    obj.getString("opcion_b"),
                    obj.getString("opcion_c"),
                    obj.getString("opcion_d")
                ).shuffled()
            ))
        }
        return todas.shuffled().take(10)
    }

    // ── Guardar puntaje — única llamada al servidor ───────────────────────────
    suspend fun sumarPuntaje(puntaje: Int, dificultad: String): ResultadoScript {
        val body = JSONObject().apply {
            put("action",     "sumar_puntaje")
            put("id",         obtenerIdLocal())
            put("puntaje",    puntaje)
            put("dificultad", dificultad)
        }
        return llamarScript(body)
    }

    // ── Registro ──────────────────────────────────────────────────────────────
    suspend fun registrarJugador(nombre: String, apellido: String): ResultadoScript {
        val id = java.util.UUID.randomUUID().toString()
        val body = JSONObject().apply {
            put("action",   "registrar")
            put("id",       id)
            put("nombre",   nombre)
            put("apellido", apellido)
        }
        return when (val r = llamarScript(body)) {
            is ResultadoScript.Exito -> {
                prefs.edit()
                    .putString("jugador_id",      id)
                    .putString("jugador_nombre",   "$nombre $apellido")
                    .apply()
                r
            }
            is ResultadoScript.Error -> r
        }
    }

    // ── Obtener jugador ───────────────────────────────────────────────────────
    suspend fun obtenerJugador(): ResultadoScript {
        val body = JSONObject().apply {
            put("action", "obtener_jugador")
            put("id",     obtenerIdLocal())
        }
        return llamarScript(body)
    }

    // ── Ranking ───────────────────────────────────────────────────────────────
    suspend fun obtenerRanking(): ResultadoScript {
        val body = JSONObject().apply { put("action", "obtener_ranking") }
        return llamarScript(body)
    }

    // ── HTTP con redirect manual (Apps Script POST → GET) ────────────────────
    private suspend fun llamarScript(body: JSONObject): ResultadoScript =
        withContext(Dispatchers.IO) {
            try {
                val bytes = body.toString().toByteArray()

                // 1. POST inicial
                var conn = (URL(SCRIPT_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod        = "POST"
                    doOutput             = true
                    instanceFollowRedirects = false
                    connectTimeout       = 20_000
                    readTimeout          = 20_000
                    setRequestProperty("Content-Type", "application/json")
                }
                conn.outputStream.use { it.write(bytes) }
                conn.connect()

                // 2. Seguir redirect manual si es 302
                val finalUrl = if (conn.responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    conn.responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                    conn.getHeaderField("Location")
                } else null

                val respuesta = if (finalUrl != null) {
                    conn.disconnect()
                    val conn2 = (URL(finalUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod  = "GET"
                        connectTimeout = 20_000
                        readTimeout    = 20_000
                    }
                    conn2.connect()
                    leerRespuesta(conn2).also { conn2.disconnect() }
                } else {
                    leerRespuesta(conn).also { conn.disconnect() }
                }

                ResultadoScript.Exito(JSONObject(respuesta))
            } catch (e: Exception) {
                ResultadoScript.Error("Error de conexión: ${e.message}")
            }
        }

    private fun leerRespuesta(conn: HttpURLConnection): String {
        val stream = if (conn.responseCode < 400) conn.inputStream else conn.errorStream
        return BufferedReader(InputStreamReader(stream)).readText()
    }
}