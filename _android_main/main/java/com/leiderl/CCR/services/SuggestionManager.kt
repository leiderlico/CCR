package com.leiderl.CCR.services

import android.util.Log
import com.leiderl.CCR.data.repository.BibliaRepository
import com.leiderl.CCR.utils.BibliaParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Sugerencia(
    val referencia: String,        // "Juan 3:16"
    val texto: String,             // Texto del versículo
    val libroId: Int,
    val capitulo: Int,
    val versiculo: Int,
    val prioridad: Int             // 1=referencia exacta, 2=famoso, 3=FTS
)

class SuggestionManager(private val repository: BibliaRepository) {

    /**
     * Procesa el texto reconocido por voz y devuelve una sugerencia si hay coincidencia.
     *
     * IMPORTANTE: Todo el texto se normaliza (sin acentos, minúsculas) antes de buscar,
     * para que "Amó Dios" y "amo dios" sean equivalentes.
     *
     * Prioridad:
     * 1. Referencia directa (Génesis 5:8)
     * 2. Versículo famoso (frase inicial conocida)
     * 3. Búsqueda FTS5 general
     */
    suspend fun procesarTextoVoz(texto: String): Sugerencia? = withContext(Dispatchers.IO) {
        Log.d("SuggestionMgr", "Procesando: $texto")

        // Normalizar el texto de entrada UNA SOLA VEZ aquí
        // Esto resuelve el problema de acentos: el micrófono puede devolver
        // "amó" o "amo", "Jehová" o "Jehova" — con normalización son equivalentes
        val textoNormalizado = BibliaParser.normalizar(texto)
        Log.d("SuggestionMgr", "Normalizado: $textoNormalizado")

        // PRIORIDAD 1: Intentar parsear como referencia bíblica
        // BibliaParser.esReferencia y parsearReferencia ya normalizan internamente,
        // pero pasamos el texto original para no perder información de libro con tildes
        if (BibliaParser.esReferencia(texto)) {
            val ref = BibliaParser.parsearReferencia(texto)
            if (ref != null) {
                val versiculo = repository.buscarPorReferencia(
                    ref.libroId, ref.capitulo, ref.versiculo ?: 1
                )
                if (versiculo != null) {
                    val libro = repository.getLibroById(ref.libroId)
                    val refTexto = "${libro?.nombre ?: "?"} ${ref.capitulo}:${ref.versiculo ?: 1}"
                    Log.d("SuggestionMgr", "Referencia encontrada: $refTexto")
                    return@withContext Sugerencia(
                        referencia = refTexto,
                        texto = versiculo.texto,
                        libroId = ref.libroId,
                        capitulo = ref.capitulo,
                        versiculo = ref.versiculo ?: 1,
                        prioridad = 1
                    )
                }
            }
        }

        // PRIORIDAD 2: Buscar en versículos famosos
        // Usamos el texto NORMALIZADO para comparar contra las frases_iniciales
        // (que también están guardadas sin acentos en la BD)
        val famosos = repository.buscarVersiculoFamoso(textoNormalizado)
        if (famosos.isNotEmpty()) {
            val (famoso, versiculo) = famosos.first()
            if (versiculo != null) {
                val libro = repository.getLibroById(famoso.libro_id)
                val refTexto = "${libro?.nombre ?: "?"} ${famoso.capitulo}:${famoso.versiculo}"
                Log.d("SuggestionMgr", "Versículo famoso: $refTexto")
                return@withContext Sugerencia(
                    referencia = refTexto,
                    texto = versiculo.texto,
                    libroId = famoso.libro_id,
                    capitulo = famoso.capitulo,
                    versiculo = famoso.versiculo,
                    prioridad = 2
                )
            }
        }

        // PRIORIDAD 3: Búsqueda FTS5 por contenido
        // Mínimo 10 chars para evitar búsquedas demasiado cortas/ambiguas
        if (textoNormalizado.length >= 10) {
            val resultados = repository.buscarPorContenido(textoNormalizado)
            if (resultados.isNotEmpty()) {
                val v = resultados.first()
                val libro = repository.getLibroById(v.libro_id)
                val refTexto = "${libro?.nombre ?: "?"} ${v.capitulo}:${v.versiculo}"
                Log.d("SuggestionMgr", "FTS resultado: $refTexto")
                return@withContext Sugerencia(
                    referencia = refTexto,
                    texto = v.texto,
                    libroId = v.libro_id,
                    capitulo = v.capitulo,
                    versiculo = v.versiculo,
                    prioridad = 3
                )
            }
        }

        Log.d("SuggestionMgr", "Sin resultados para: $texto")
        null
    }
}