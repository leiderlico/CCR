package com.leiderl.CCR.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.leiderl.CCR.data.database.entities.Libro
import com.leiderl.CCR.data.database.entities.Versiculo
import com.leiderl.CCR.data.repository.BibliaDownloadManager
import com.leiderl.CCR.data.repository.BibliaRepository
import com.leiderl.CCR.services.Sugerencia
import com.leiderl.CCR.services.SuggestionManager
import kotlinx.coroutines.launch

class BibliaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BibliaRepository(application)
    private val suggestionManager = SuggestionManager(repository)

    // ─────────────────────────────────────────────────
    // VERSIONES DISPONIBLES
    // ─────────────────────────────────────────────────

    data class Version(
        val nombre: String,
        val nombreCompleto: String,
        val archivoJson: String,
        val urlDescarga: String? = null   // null = viene en assets
    )

    companion object {
        val VERSION_DEFAULT = Version("RVA1960", "Reina Valera 1960", "biblia_rv1960.json")

        val VERSIONES_DISPONIBLES = listOf(
            VERSION_DEFAULT,
            Version("NVI",      "Nueva Versión Internacional",    "biblia_nvi.json",    "https://github.com/leiderlico/bibliasccr/releases/download/v1/biblia_nvi.json"),
            Version("NTV",      "Nueva Traducción Viviente",      "biblia_ntv.json",    "https://github.com/leiderlico/bibliasccr/releases/download/v1/biblia_ntv.json"),
            Version("RVA2015",  "Reina Valera 2015",              "biblia_rva2015.json","https://github.com/leiderlico/bibliasccr/releases/download/v1/biblia_rva2015.json"),
            Version("LBLA",     "La Biblia de las Américas",      "biblia_lbla.json",   "https://github.com/leiderlico/bibliasccr/releases/download/v1/biblia_lbla.json"),
            Version("BDO",      "Biblia del Oso",                 "bdo.json",           "https://github.com/leiderlico/bibliasccr/releases/download/v1/bdo.json"),
            Version("BTX",      "La Biblia Textual",              "btx.json",           "https://github.com/leiderlico/bibliasccr/releases/download/v1/btx.json"),
            Version("PDT",      "Palabra de Dios para Todos",     "pdt.json",           "https://github.com/leiderlico/bibliasccr/releases/download/v1/pdt.json"),
            //Version("PESHITTA", "Biblia Peshitta",                "psh.json",           "https://github.com/leiderlico/bibliasccr/releases/download/v1/psh.json"),
        )
    }

    private val _versionActiva = MutableLiveData(VERSION_DEFAULT)
    val versionActiva: LiveData<Version> = _versionActiva

    // ─────────────────────────────────────────────────
    // DESCARGA BAJO DEMANDA
    // ─────────────────────────────────────────────────

    private val _descargaProgreso = MutableLiveData<Int?>(null)
    val descargaProgreso: LiveData<Int?> = _descargaProgreso

    /** Devuelve true si la versión ya está disponible (en assets o descargada) */
    fun isVersionDisponible(version: Version): Boolean {
        if (version.urlDescarga == null) return true   // siempre en assets
        return BibliaDownloadManager.isDescargada(getApplication(), version)
    }

    /** Descarga la versión y luego la activa automáticamente */
    fun descargarVersion(version: Version, libroId: Int, capitulo: Int) {
        val url = version.urlDescarga ?: return
        viewModelScope.launch {
            try {
                _descargaVersion.value = version
                _descargaProgreso.value = 0
                BibliaDownloadManager.descargar(
                    context  = getApplication(),
                    version  = version,
                    url      = url,
                    onProgress = { progreso -> _descargaProgreso.postValue(progreso) }
                )
                _descargaProgreso.value = null
                cambiarVersion(version, libroId, capitulo)
            } catch (e: Exception) {
                _descargaProgreso.value = null
                Log.e("BibliaVM", "Error descargando ${version.nombre}: ${e.message}")
                _error.value = "Error al descargar ${version.nombreCompleto}. Verifica tu conexión."
            }
        }
    }

    // ─────────────────────────────────────────────────
    // Estado de inicialización
    // ─────────────────────────────────────────────────

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isDbReady = MutableLiveData(false)
    val isDbReady: LiveData<Boolean> = _isDbReady

    val libros: LiveData<List<Libro>> = repository.getAllLibros().asLiveData()

    private val _versiculos = MutableLiveData<List<Versiculo>>()
    val versiculos: LiveData<List<Versiculo>> = _versiculos

    private val _libroSeleccionado = MutableLiveData<Libro?>()
    val libroSeleccionado: LiveData<Libro?> = _libroSeleccionado

    private val _capitulos = MutableLiveData<List<Int>>()
    val capitulos: LiveData<List<Int>> = _capitulos

    private val _sugerencia = MutableLiveData<Sugerencia?>()
    val sugerencia: LiveData<Sugerencia?> = _sugerencia

    private val _microfono = MutableLiveData(false)
    val microfono: LiveData<Boolean> = _microfono

    private val _voiceEnabled = MutableLiveData(false)
    val voiceEnabled: LiveData<Boolean> = _voiceEnabled

    private val _descargaVersion = MutableLiveData<Version?>(null)
    val descargaVersion: LiveData<Version?> = _descargaVersion

    fun toggleVoice(): Boolean {
        val nuevoEstado = !(_voiceEnabled.value ?: false)
        _voiceEnabled.value = nuevoEstado
        return nuevoEstado
    }

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        initializeIfNeeded()
    }

    fun initializeIfNeeded() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val initialized = repository.isDatabaseInitialized()
                if (!initialized) {
                    Log.d("BibliaVM", "Inicializando base de datos...")
                    repository.initializeDatabase()
                    Log.d("BibliaVM", "Base de datos lista")
                }
                _isDbReady.value = true
            } catch (e: Exception) {
                Log.e("BibliaVM", "Error: ${e.message}", e)
                _error.value = "Error al cargar la Biblia: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun seleccionarLibro(libro: Libro) {
        _libroSeleccionado.value = libro
        viewModelScope.launch {
            val caps = repository.getCapitulosByLibro(libro.id)
            _capitulos.value = caps
        }
    }

    fun cargarLibroParaNavegacion(libroId: Int, onListo: (Libro?) -> Unit) {
        viewModelScope.launch {
            val libro = repository.getLibroById(libroId)
            libro?.let { seleccionarLibro(it) }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onListo(libro)
            }
        }
    }

    fun cargarVersiculos(libroId: Int, capitulo: Int) {
        val version = _versionActiva.value ?: VERSION_DEFAULT

        if (version.archivoJson == VERSION_DEFAULT.archivoJson) {
            // RV1960 → desde Room
            viewModelScope.launch {
                try {
                    repository.getVersiculos(libroId, capitulo).collect { list ->
                        _versiculos.value = list
                    }
                } catch (e: Exception) {
                    _error.value = "Error cargando versículos"
                }
            }
        } else {
            // Otras versiones → desde JSON (assets o archivo descargado)
            viewModelScope.launch {
                try {
                    val jsonSource = BibliaDownloadManager.leerJson(getApplication(), version)
                    val lista = repository.getVersiculosDesdeJsonString(jsonSource, libroId, capitulo)
                    _versiculos.value = lista
                } catch (e: Exception) {
                    Log.e("BibliaVM", "Error cargando versión ${version.nombre}: ${e.message}")
                    _error.value = "No tienes la versión ${version.nombre} descargada, descárgala"
                }
            }
        }
    }

    fun cambiarVersion(version: Version, libroId: Int, capitulo: Int) {
        _versionActiva.value = version
        cargarVersiculos(libroId, capitulo)
    }

    fun procesarVoz(texto: String) {
        viewModelScope.launch {
            try {
                val sugerencia = suggestionManager.procesarTextoVoz(texto)
                _sugerencia.value = sugerencia
            } catch (e: Exception) {
                Log.e("BibliaVM", "Error procesando voz: ${e.message}")
            }
        }
    }

    fun descartarSugerencia() {
        _sugerencia.value = null
    }

    fun setMicrofono(activo: Boolean) {
        _microfono.value = activo
    }

    fun clearError() {
        _error.value = null
    }
}