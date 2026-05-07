package com.leiderl.CCR.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.leiderl.CCR.data.model.Devocional
import com.leiderl.CCR.data.repository.DevocionalRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DevocionalViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = DevocionalRepository(app)

    private val _devocional = MutableLiveData<Devocional?>()
    val devocional: LiveData<Devocional?> = _devocional

    // Guarda qué día se cargó por última vez
    private var fechaCargada: String? = null

    init {
        cargar()
    }

    fun verificarYRecargarSiCambioDia() {
        val hoy = hoyString()
        if (fechaCargada != hoy) cargar()
    }

    private fun cargar() {
        _devocional.value = repo.obtenerHoy()
        fechaCargada = hoyString()
    }

    private fun hoyString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}