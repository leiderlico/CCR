package com.leiderl.CCR.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.leiderl.CCR.R
import com.leiderl.CCR.data.repository.utils.Constants
import com.leiderl.CCR.databinding.FragmentVideosBinding
import com.leiderl.CCR.ui.adapters.VideosAdapter
import com.leiderl.CCR.ui.viewmodels.BibliaViewModel
import com.leiderl.CCR.ui.viewmodels.VideosViewModel

class VideosFragment : Fragment() {

    private var _binding: FragmentVideosBinding? = null
    private val binding get() = _binding!!
    private val videosViewModel: VideosViewModel by viewModels()
    private val bibliaViewModel: BibliaViewModel by activityViewModels()
    private lateinit var adapter: VideosAdapter

    private val nombresLibros = mapOf(
        1 to "Génesis", 2 to "Éxodo", 3 to "Levítico", 4 to "Números", 5 to "Deuteronomio",
        6 to "Josué", 7 to "Jueces", 8 to "Rut", 9 to "1 Samuel", 10 to "2 Samuel",
        11 to "1 Reyes", 12 to "2 Reyes", 13 to "1 Crónicas", 14 to "2 Crónicas",
        15 to "Esdras", 16 to "Nehemías", 17 to "Ester", 18 to "Job", 19 to "Salmos",
        20 to "Proverbios", 21 to "Eclesiastés", 22 to "Cantares", 23 to "Isaías",
        24 to "Jeremías", 25 to "Lamentaciones", 26 to "Ezequiel", 27 to "Daniel",
        28 to "Oseas", 29 to "Joel", 30 to "Amós", 31 to "Abdías", 32 to "Jonás",
        33 to "Miqueas", 34 to "Nahúm", 35 to "Habacuc", 36 to "Sofonías",
        37 to "Hageo", 38 to "Zacarías", 39 to "Malaquías",
        40 to "Mateo", 41 to "Marcos", 42 to "Lucas", 43 to "Juan", 44 to "Hechos",
        45 to "Romanos", 46 to "1 Corintios", 47 to "2 Corintios", 48 to "Gálatas",
        49 to "Efesios", 50 to "Filipenses", 51 to "Colosenses",
        52 to "1 Tesalonicenses", 53 to "2 Tesalonicenses",
        54 to "1 Timoteo", 55 to "2 Timoteo", 56 to "Tito", 57 to "Filemón",
        58 to "Hebreos", 59 to "Santiago", 60 to "1 Pedro", 61 to "2 Pedro",
        62 to "1 Juan", 63 to "2 Juan", 64 to "3 Juan", 65 to "Judas",
        66 to "Apocalipsis"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupBusqueda()
        setupObservers()
        videosViewModel.inicializar()

        // Filtro por libro+capítulo — viene desde VersiculosFragment (tiene prioridad)
        val filtroLibroId = arguments?.getInt("filtroLibroId", -1) ?: -1
        val filtroCapitulo = arguments?.getInt("filtroCapitulo", -1) ?: -1
        if (filtroLibroId != -1 && filtroCapitulo != -1) {
            val nombreLibro = nombresLibros[filtroLibroId] ?: "Libro $filtroLibroId"
            binding.tvSyncStatus.text = "Prédicas de $nombreLibro $filtroCapitulo"
            binding.tvSyncStatus.visibility = View.VISIBLE
            videosViewModel.filtrarPorLibroCapitulo(filtroLibroId, filtroCapitulo)
        } else {
            // Filtro por grupo — viene desde GruposSelectorFragment
            val grupoNombre = arguments?.getString("grupoNombre")
            if (!grupoNombre.isNullOrEmpty()) {
                requireActivity().title = grupoNombre
                videosViewModel.filtrarPorGrupo(grupoNombre)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = VideosAdapter(
            onVideoClick = { video ->
                val cita = if (video.libroId > 0) {
                    val nombreLibro = nombresLibros[video.libroId] ?: "Libro ${video.libroId}"
                    if (video.capitulo > 0 && video.versiculo.isNotEmpty()) "$nombreLibro ${video.capitulo}: ${video.versiculo}"
                    else if (video.capitulo > 0) "$nombreLibro ${video.capitulo}"
                    else nombreLibro
                } else ""
                val args = Bundle().apply {
                    putString("urlYoutube", video.urlYoutube)
                    putString("titulo", video.titulo)
                    putString("predicador", video.predicador)
                    putString("cita", cita)
                    putString("fecha", video.fecha)
                    putInt("libroId", video.libroId)
                    putInt("capitulo", video.capitulo)
                }
                findNavController().navigate(R.id.videoPlayerFragment, args)
            },
            onGrupoClick = { /* ya no se usa — cada pantalla es un solo grupo */ },
            onChipClick = { libroId, capitulo -> navegarACapitulo(libroId, capitulo) },
            nombreLibro = { libroId -> nombresLibros[libroId] ?: "Libro $libroId" }
        )
        binding.rvVideos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVideos.adapter = adapter
    }

    private fun navegarACapitulo(libroId: Int, capitulo: Int) {
        val navController = findNavController()
        val nombreLibro = nombresLibros[libroId] ?: "Libro $libroId"

        bibliaViewModel.cargarLibroParaNavegacion(libroId) { _ ->
            navController.popBackStack(R.id.librosFragment, false)
            val argsC = Bundle().apply {
                putInt(Constants.ARG_LIBRO_ID, libroId)
                putString(Constants.ARG_LIBRO_NOMBRE, nombreLibro)
            }
            navController.navigate(R.id.capitulosFragment, argsC)
            binding.root.post {
                val argsV = Bundle().apply {
                    putInt(Constants.ARG_LIBRO_ID, libroId)
                    putInt(Constants.ARG_CAPITULO, capitulo)
                    putInt(Constants.ARG_VERSICULO_HIGHLIGHT, -1)
                }
                navController.navigate(R.id.versiculosFragment, argsV)
            }
        }
    }

    private fun setupBusqueda() {
        binding.etBusqueda.addTextChangedListener { editable ->
            videosViewModel.buscar(editable?.toString() ?: "")
        }
        binding.chipRecientes.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                val grupoNombre = arguments?.getString("grupoNombre")
                if (!grupoNombre.isNullOrEmpty()) {
                    videosViewModel.limpiarFiltrosManteniendoGrupo(grupoNombre)
                } else {
                    videosViewModel.limpiarFiltros()
                }
            }
        }
    }

    private fun setupObservers() {
        videosViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressVideos.visibility = if (loading) View.VISIBLE else View.GONE
        }

        videosViewModel.syncStatus.observe(viewLifecycleOwner) { status ->
            val filtroLibroId = arguments?.getInt("filtroLibroId", -1) ?: -1
            val grupoNombre = arguments?.getString("grupoNombre")
            if (filtroLibroId == -1 && grupoNombre.isNullOrEmpty()) {
                binding.tvSyncStatus.visibility = if (status != null) View.VISIBLE else View.GONE
                binding.tvSyncStatus.text = status ?: ""
            }
        }

        videosViewModel.grupos.observe(viewLifecycleOwner) { grupos ->
            // Aplanar todos los videos de todos los grupos en una sola lista para el adapter
            // (ya estamos dentro de un grupo, no necesitamos headers)
            val todosLosVideos = grupos.flatMap { it.videos }
            adapter.submitVideosSinGrupos(todosLosVideos)
            binding.emptyState.visibility = if (todosLosVideos.isEmpty()) View.VISIBLE else View.GONE
            binding.rvVideos.visibility = if (todosLosVideos.isEmpty()) View.GONE else View.VISIBLE
        }

        videosViewModel.predicadores.observe(viewLifecycleOwner) { predicadores ->
            agregarChipsPredicadores(predicadores)
        }
    }

    private fun agregarChipsPredicadores(predicadores: List<String>) {
        val chipGroup = binding.chipGroupFiltros
        val count = chipGroup.childCount
        if (count > 1) chipGroup.removeViews(1, count - 1)

        predicadores.forEach { predicador ->
            val chip = Chip(requireContext()).apply {
                text = predicador
                isCheckable = true
                setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        binding.chipRecientes.isChecked = false
                        videosViewModel.filtrarPorPredicador(predicador)
                    } else {
                        videosViewModel.filtrarPorPredicador(null)
                    }
                }
            }
            chipGroup.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}