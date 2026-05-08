package com.leiderl.CCR.data.repository

import android.content.Context
import com.leiderl.CCR.ui.viewmodels.BibliaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object BibliaDownloadManager {

    // Versiones que siempre están en assets (no se descargan)
    private val VERSIONES_EN_ASSETS = setOf("RVA1960")

    private fun archivoLocal(context: Context, version: BibliaViewModel.Version): File =
        File(context.filesDir, version.archivoJson)

    /** True si la versión ya está lista para usar */
    fun isDescargada(context: Context, version: BibliaViewModel.Version): Boolean {
        if (version.nombre in VERSIONES_EN_ASSETS) return true
        return archivoLocal(context, version).exists()
    }

    /** Descarga el JSON desde la URL y lo guarda en filesDir */
    suspend fun descargar(
        context: Context,
        version: BibliaViewModel.Version,
        url: String,
        onProgress: (Int) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val file = archivoLocal(context, version)
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout    = 60_000
        connection.connect()

        val total = connection.contentLength
        var downloaded = 0

        connection.inputStream.use { input ->
            file.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytes: Int
                while (input.read(buffer).also { bytes = it } != -1) {
                    output.write(buffer, 0, bytes)
                    downloaded += bytes
                    if (total > 0) onProgress(downloaded * 100 / total)
                }
            }
        }
    }

    /** Lee el JSON: desde assets si es versión base, desde filesDir si fue descargada */
    suspend fun leerJson(context: Context, version: BibliaViewModel.Version): String =
        withContext(Dispatchers.IO) {
            if (version.nombre in VERSIONES_EN_ASSETS) {
                context.assets.open(version.archivoJson).bufferedReader().use { it.readText() }
            } else {
                archivoLocal(context, version).readText()
            }
        }
}