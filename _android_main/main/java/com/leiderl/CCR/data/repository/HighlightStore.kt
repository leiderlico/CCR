package com.leiderl.CCR.data.repository

import android.content.Context

object HighlightStore {

    private const val PREFS_NAME = "versiculo_highlights"

    // Colores disponibles (6 opciones)
    val COLORES = listOf(
        0x66F48FB1.toInt(), // Rosado
        0x66FFCC80.toInt(), // Naranja
        0x66A5D6A7.toInt(), // Verde
        0x66BDBDBD.toInt(), // Gris bajo
        0x66CF94DA.toInt(), // Morado
        0x66EF9A9A.toInt(), // Rojo
    )

    private fun key(libroId: Int, capitulo: Int, versiculo: Int) =
        "${libroId}_${capitulo}_${versiculo}"

    fun setColor(context: Context, libroId: Int, capitulo: Int, versiculo: Int, color: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(key(libroId, capitulo, versiculo), color).apply()
    }

    fun removeColor(context: Context, libroId: Int, capitulo: Int, versiculo: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(key(libroId, capitulo, versiculo)).apply()
    }

    fun getColor(context: Context, libroId: Int, capitulo: Int, versiculo: Int): Int? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val k = key(libroId, capitulo, versiculo)
        return if (prefs.contains(k)) prefs.getInt(k, 0) else null
    }

    // Devuelve mapa versiculo→color para todo el capítulo
    fun getColoresCapitulo(context: Context, libroId: Int, capitulo: Int): Map<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prefix = "${libroId}_${capitulo}_"
        return prefs.all
            .filter { it.key.startsWith(prefix) }
            .mapNotNull { (k, v) ->
                val versiculo = k.removePrefix(prefix).toIntOrNull() ?: return@mapNotNull null
                versiculo to (v as Int)
            }.toMap()
    }
}