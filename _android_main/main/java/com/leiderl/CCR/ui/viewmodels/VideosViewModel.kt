package com.leiderl.CCR.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.leiderl.CCR.data.database.entities.Video
import com.leiderl.CCR.data.repository.VideoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class GrupoVideos(
    val nombre: String,
    val videos: List<Video>,
    val expandido: Boolean = true  // dentro de un grupo siempre expandido
)

class VideosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository(application)

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _syncStatus = MutableLiveData<String?>(null)
    val syncStatus: LiveData<String?> = _syncStatus

    private val _grupos = MutableLiveData<List<GrupoVideos>>()
    val grupos: LiveData<List<GrupoVideos>> = _grupos

    private val _filtroPredicador = MutableLiveData<String?>(null)
    private val _filtroGrupo = MutableLiveData<String?>(null)
    private val _filtroLibroId = MutableLiveData<Int?>(null)
    private val _filtroCapitulo = MutableLiveData<Int?>(null)

    private val _predicadores = MutableLiveData<List<String>>()
    val predicadores: LiveData<List<String>> = _predicadores

    private val _queryBusqueda = MutableLiveData("")
    private var searchJob: Job? = null

    // Contexto de origen — se fija al llegar desde VersiculosFragment y nunca se borra
    private var contextoLibroId: Int? = null
    private var contextoCapitulo: Int? = null

    private var todosLosVideos: List<Video> = emptyList()

    fun inicializar() {
        viewModelScope.launch {
            _isLoading.value = true
            cargarDesdeCache()
            when (val result = repository.sincronizarDesdeSheet()) {
                is VideoRepository.SyncResult.Success -> {
                    cargarDesdeCache()
                    _syncStatus.value = null
                }
                is VideoRepository.SyncResult.NoInternet -> {
                    _syncStatus.value = if (todosLosVideos.isEmpty())
                        "Sin conexión — no hay videos en caché"
                    else
                        "Sin conexión — mostrando datos guardados"
                }
                is VideoRepository.SyncResult.Error -> {
                    _syncStatus.value = "Error al actualizar: ${result.message}"
                }
            }
            _isLoading.value = false
        }
    }

    private suspend fun cargarDesdeCache() {
        todosLosVideos = repository.getVideos()
        // Predicadores filtrados al grupo activo
        val grupoActual = _filtroGrupo.value
        val videosDelGrupo = if (grupoActual != null)
            todosLosVideos.filter { it.grupo == grupoActual }
        else todosLosVideos
        _predicadores.value = videosDelGrupo.map { it.predicador }
            .filter { it.isNotBlank() }.distinct().sorted()
        aplicarFiltros()
    }

    fun buscar(query: String) {
        _queryBusqueda.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            aplicarFiltros()
        }
    }

    fun filtrarPorGrupo(grupo: String) {
        _filtroGrupo.value = grupo
        _filtroPredicador.value = null
        _filtroLibroId.value = null
        _filtroCapitulo.value = null
        _queryBusqueda.value = ""
        // Actualizar chips de predicadores al grupo seleccionado
        val videosDelGrupo = todosLosVideos.filter { it.grupo == grupo }
        _predicadores.value = videosDelGrupo.map { it.predicador }
            .filter { it.isNotBlank() }.distinct().sorted()
        aplicarFiltros()
    }

    fun filtrarPorPredicador(predicador: String?) {
        _filtroPredicador.value = predicador
        aplicarFiltros()
    }

    fun filtrarPorLibro(libroId: Int?) {
        _filtroLibroId.value = libroId
        _filtroCapitulo.value = null
        aplicarFiltros()
    }

    fun filtrarPorLibroCapitulo(libroId: Int, capitulo: Int) {
        contextoLibroId = libroId
        contextoCapitulo = capitulo
        _filtroLibroId.value = libroId
        _filtroCapitulo.value = capitulo
        _filtroPredicador.value = null
        _filtroGrupo.value = null
        _queryBusqueda.value = ""
        aplicarFiltros()
    }

    fun limpiarFiltros() {
        _filtroPredicador.value = null
        _filtroLibroId.value = contextoLibroId
        _filtroCapitulo.value = contextoCapitulo
        _filtroGrupo.value = null
        _queryBusqueda.value = ""
        aplicarFiltros()
    }

    // Limpia texto/predicador pero mantiene el grupo activo
    fun limpiarFiltrosManteniendoGrupo(grupoNombre: String) {
        _filtroPredicador.value = null
        _filtroLibroId.value = contextoLibroId
        _filtroCapitulo.value = contextoCapitulo
        _queryBusqueda.value = ""
        _filtroGrupo.value = grupoNombre
        aplicarFiltros()
    }

    fun toggleGrupo(nombreGrupo: String) {
        val actuales = _grupos.value ?: return
        _grupos.value = actuales.map { grupo ->
            if (grupo.nombre == nombreGrupo) grupo.copy(expandido = !grupo.expandido)
            else grupo
        }
    }

    private fun aplicarFiltros() {
        var filtrados = todosLosVideos

        val grupo = _filtroGrupo.value
        if (grupo != null) {
            filtrados = filtrados.filter { it.grupo == grupo }
        }

        val query = _queryBusqueda.value?.trim() ?: ""
        if (query.isNotEmpty()) {
            filtrados = filtrados.filter { video ->
                video.titulo.contains(query, ignoreCase = true) ||
                        video.predicador.contains(query, ignoreCase = true) ||
                        video.predicador.contains(query, ignoreCase = true)
            }
        }

        val predicador = _filtroPredicador.value
        if (predicador != null) {
            filtrados = filtrados.filter { it.predicador == predicador }
        }

        val libroId = _filtroLibroId.value
        if (libroId != null) {
            filtrados = filtrados.filter { it.libroId == libroId }
            val capitulo = _filtroCapitulo.value
            if (capitulo != null) {
                filtrados = filtrados.filter { it.capitulo == capitulo }
            }
        }

        // Siempre ordenar por fecha descendente
        filtrados = filtrados.sortedByDescending { it.fechaOrden }

        // Agrupar — dentro de un grupo solo habrá un grupo en la lista
        val grupos = filtrados
            .groupBy { it.grupo }
            .map { (nombre, videos) -> GrupoVideos(nombre = nombre, videos = videos) }
            .sortedBy { it.nombre }

        _grupos.value = grupos
    }

    fun clearSyncStatus() {
        _syncStatus.value = null
    }
}