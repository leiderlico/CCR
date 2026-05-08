package com.leiderl.CCR.data.repository

import android.content.Context
import android.util.Log
import com.leiderl.CCR.data.database.BibliaDatabase
import com.leiderl.CCR.data.database.entities.Video
import com.leiderl.CCR.data.repository.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class VideoRepository(context: Context) {

    private val videoDao = BibliaDatabase.getInstance(context).videoDao()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    sealed class SyncResult {
        object Success : SyncResult()
        data class Error(val message: String) : SyncResult()
        object NoInternet : SyncResult()
    }

    suspend fun getVideos(): List<Video> = withContext(Dispatchers.IO) { videoDao.getAll() }
    suspend fun getGrupos(): List<String> = withContext(Dispatchers.IO) { videoDao.getGrupos() }
    suspend fun getPredicadores(): List<String> = withContext(Dispatchers.IO) { videoDao.getPredicadores() }
    suspend fun getByGrupo(grupo: String): List<Video> = withContext(Dispatchers.IO) { videoDao.getByGrupo(grupo) }
    suspend fun getByPredicador(predicador: String): List<Video> = withContext(Dispatchers.IO) { videoDao.getByPredicador(predicador) }
    suspend fun getByLibroCapitulo(libroId: Int, capitulo: Int): List<Video> = withContext(Dispatchers.IO) { videoDao.getByLibroCapitulo(libroId, capitulo) }
    suspend fun getByLibro(libroId: Int): List<Video> = withContext(Dispatchers.IO) { videoDao.getByLibro(libroId) }
    suspend fun search(query: String): List<Video> = withContext(Dispatchers.IO) { videoDao.search(query) }
    suspend fun hasVideos(): Boolean = withContext(Dispatchers.IO) { videoDao.count() > 0 }

    suspend fun sincronizarDesdeSheet(): SyncResult = withContext(Dispatchers.IO) {
        try {
            Log.d("VideoRepo", "Sincronizando desde Google Sheets...")
            val csv = URL(Constants.SHEET_CSV_URL).readText()
            val videos = parsearCSV(csv)
            if (videos.isEmpty()) {
                Log.w("VideoRepo", "CSV vacío o sin datos válidos")
                return@withContext SyncResult.Error("No se encontraron videos en el Sheet")
            }
            videoDao.deleteAll()
            videoDao.insertAll(videos)
            Log.d("VideoRepo", "Sincronizados ${videos.size} videos")
            SyncResult.Success
        } catch (e: java.net.UnknownHostException) {
            Log.w("VideoRepo", "Sin conexión a internet")
            SyncResult.NoInternet
        } catch (e: Exception) {
            Log.e("VideoRepo", "Error sincronizando: ${e.message}", e)
            SyncResult.Error(e.message ?: "Error desconocido")
        }
    }

    private fun parsearCSV(csv: String): List<Video> {
        val lineas = csv.lines().drop(1)
        val videos = mutableListOf<Video>()

        for ((index, linea) in lineas.withIndex()) {
            if (linea.isBlank()) continue
            try {
                val cols = parsearLineaCSV(linea)
                if (cols.size < 5) continue

                val titulo     = cols.getOrElse(0) { "" }.trim()
                val predicador = cols.getOrElse(1) { "" }.trim()
                val grupo      = cols.getOrElse(2) { "" }.trim()
                val fecha      = cols.getOrElse(3) { "" }.trim()
                val url        = cols.getOrElse(4) { "" }.trim()
                val libroId    = cols.getOrElse(5) { "0" }.trim().toIntOrNull() ?: 0
                val capitulo   = cols.getOrElse(6) { "0" }.trim().toIntOrNull() ?: 0
                val versiculo  = cols.getOrElse(7) { "" }.trim() // String: "12" o "12-15"

                if (titulo.isEmpty() || url.isEmpty()) continue

                val fechaOrden = try {
                    dateFormat.parse(fecha)?.time ?: 0L
                } catch (e: Exception) { 0L }

                videos.add(Video(
                    titulo = titulo,
                    predicador = predicador,
                    grupo = grupo,
                    fecha = fecha,
                    fechaOrden = fechaOrden,
                    urlYoutube = url,
                    libroId = libroId,
                    capitulo = capitulo,
                    versiculo = versiculo
                ))
            } catch (e: Exception) {
                Log.w("VideoRepo", "Error parseando línea ${index + 2}: ${e.message}")
            }
        }
        return videos
    }

    private fun parsearLineaCSV(linea: String): List<String> {
        val cols = mutableListOf<String>()
        val sb = StringBuilder()
        var dentroComillas = false
        for (char in linea) {
            when {
                char == '"' -> dentroComillas = !dentroComillas
                char == ',' && !dentroComillas -> { cols.add(sb.toString()); sb.clear() }
                else -> sb.append(char)
            }
        }
        cols.add(sb.toString())
        return cols
    }
}