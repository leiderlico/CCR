package com.leiderl.CCR.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class Peticion(val texto: String, val fecha: String)

class PeticionRepository {

    private val SCRIPT_URL = "https://script.google.com/macros/s/AKfycbwi2SBX5TjxPSPiK2oFR6pnQv7-T-8ByXNZDOwh1OTm8VPg4IAPffLPCSO-N5tIPaCzRg/exec"

    suspend fun enviarPeticion(texto: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("action", "peticion")
                put("texto", texto)
            }.toString()

            val conn = (URL(SCRIPT_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            OutputStreamWriter(conn.outputStream).use { it.write(body) }
            val json = JSONObject(BufferedReader(InputStreamReader(conn.inputStream)).readText())
            json.optBoolean("ok", false)
        } catch (e: Exception) { false }
    }

    suspend fun obtenerPeticiones(): List<Peticion> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("action", "obtener_peticiones")
            }.toString()

            val conn = (URL(SCRIPT_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 15_000
            }
            OutputStreamWriter(conn.outputStream).use { it.write(body) }
            val json = JSONObject(BufferedReader(InputStreamReader(conn.inputStream)).readText())
            val arr = json.optJSONArray("peticiones") ?: JSONArray()
            val lista = mutableListOf<Peticion>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                lista.add(Peticion(
                    texto = item.optString("texto", ""),
                    fecha = item.optString("fecha", "")
                ))
            }
            lista // ya vienen ordenadas desde el script (más reciente primero)
        } catch (e: Exception) { emptyList() }
    }
}