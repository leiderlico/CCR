package com.leiderl.CCR.data.repository

import android.content.Context
import com.leiderl.CCR.data.model.Devocional
import org.json.JSONArray
import java.util.Calendar

class DevocionalRepository(private val context: Context) {

    private var cache: List<Devocional>? = null

    private fun cargarTodos(): List<Devocional> {
        cache?.let { return it }
        val json = context.resources.openRawResource(
            context.resources.getIdentifier("devocionales", "raw", context.packageName)
        ).bufferedReader().readText()

        val arr   = JSONArray(json)
        val lista = mutableListOf<Devocional>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            lista.add(Devocional(
                año                = obj.getInt("año"),
                dia_del_año        = obj.getInt("dia_del_año"),
                fecha              = obj.getString("fecha"),
                titulo             = obj.getString("titulo"),
                versiculo_texto    = obj.getString("versiculo_texto"),
                versiculo_referencia = obj.getString("versiculo_referencia"),
                cuerpo             = obj.getString("cuerpo")
            ))
        }
        cache = lista
        return lista
    }

    fun obtenerHoy(): Devocional? {
        val cal  = Calendar.getInstance()
        val dia  = cal.get(Calendar.DAY_OF_YEAR)
        val anio = cal.get(Calendar.YEAR)
        return cargarTodos().find { it.dia_del_año == dia && it.año == anio }
    }
}