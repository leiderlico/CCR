package com.leiderl.CCR.ui.fragments

import android.graphics.Color
import android.view.animation.AnimationUtils
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.leiderl.CCR.data.database.entities.Libro
import com.leiderl.CCR.data.repository.HighlightStore
import com.leiderl.CCR.data.repository.VideoRepository
import com.leiderl.CCR.databinding.FragmentVersiculosBinding
import com.leiderl.CCR.ui.MainActivity
import com.leiderl.CCR.ui.adapters.LibrosAdapter
import com.leiderl.CCR.ui.adapters.VersiculosAdapter
import com.leiderl.CCR.ui.viewmodels.BibliaViewModel
import com.leiderl.CCR.data.repository.utils.Constants
import kotlinx.coroutines.launch
import com.leiderl.CCR.R

class VersiculosFragment : Fragment() {

    private var _binding: FragmentVersiculosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BibliaViewModel by activityViewModels()
    private lateinit var adapter: VersiculosAdapter
    private var libroId: Int = -1
    private var capitulo: Int = 1
    private var versiculoHighlight: Int = -1
    private var libroNombreDisplay: String = ""
    private var libroAbreviacion: String = ""
    private var scrollTopOnNextUpdate: Boolean = false
    private var capitulosTotal: Int = Int.MAX_VALUE
    private val panelColoresHandler = Handler(Looper.getMainLooper())
    private var panelColoresRunnable: Runnable? = null

    private var panelEstaEnAutoShow = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVersiculosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        libroId = arguments?.getInt(Constants.ARG_LIBRO_ID) ?: -1
        capitulo = arguments?.getInt(Constants.ARG_CAPITULO) ?: 1
        versiculoHighlight = arguments?.getInt(Constants.ARG_VERSICULO_HIGHLIGHT, -1) ?: -1
        libroNombreDisplay = arguments?.getString(Constants.ARG_LIBRO_NOMBRE)
            ?: viewModel.libroSeleccionado.value?.nombre ?: ""
        libroAbreviacion = arguments?.getString(Constants.ARG_LIBRO_ABREVIACION)
            ?.takeIf { it.isNotBlank() } ?: libroNombreDisplay

        capitulosTotal = arguments?.getInt(Constants.ARG_CAPITULOS_TOTAL, -1)
            ?.takeIf { it > 0 }
            ?: viewModel.libroSeleccionado.value?.capitulos
                    ?: Int.MAX_VALUE

        requireActivity().title = "$libroNombreDisplay $capitulo"
        mainActivity().mostrarTituloClickable("$libroAbreviacion $capitulo") {
            mostrarMiniSelectorLibros()
        }

        setupRecyclerView()
        setupToolbarResaltado()
        setupSwipeGesture()
        observeViewModel()
        verificarVideosDelCapitulo()

        if (libroId != -1) {
            scrollTopOnNextUpdate = true
            viewModel.cargarVersiculos(libroId, capitulo)
        }
    }

    private fun setupRecyclerView() {
        adapter = VersiculosAdapter(
            highlightVersiculo = versiculoHighlight,
            onLongPress = { versiculo ->
                adapter.modoSeleccion = true
                adapter.seleccionados.add(versiculo.versiculo)
                adapter.notifyDataSetChanged()
                mostrarToolbarResaltado()
                mostrarPanelColoresTemporalmente()
            },
            onPress = {
                if (adapter.seleccionados.isEmpty()) ocultarToolbarResaltado()
                else {
                    actualizarContadorSeleccion()
                    mostrarPanelColoresTemporalmente()
                }
            }
        )
        adapter.tituloCapitulo = "$libroNombreDisplay $capitulo"
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@VersiculosFragment.adapter
        }
        cargarColoresGuardados()
    }

    private fun cargarColoresGuardados() {
        if (libroId == -1) return
        val colores = HighlightStore.getColoresCapitulo(requireContext(), libroId, capitulo)
        adapter.highlightColors = colores
    }

    // ── Toolbar resaltado ──────────────────────────────────────────────────

    private fun mainActivity() = requireActivity() as MainActivity

    private fun restaurarTitulo() {
        val titulo = "$libroNombreDisplay $capitulo"
        requireActivity().title = titulo
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)
            ?.supportActionBar?.title = titulo
        mainActivity().actualizarTituloClickable("$libroAbreviacion $capitulo")
    }

    private fun mostrarToolbarResaltado() {
        mainActivity().entrarModoSeleccion(
            onCancelar  = { adapter.modoSeleccion = false },
            onSalir     = { restaurarTitulo() },
            onColor     = { color -> aplicarColor(color) },
            onBorrar    = { aplicarColor(null) },
            onCompartir = { compartirSeleccion() },
            onEscuchar  = { escucharSeleccion() },
            colores     = HighlightStore.COLORES
        )
    }

    private fun mostrarPanelColoresTemporalmente() {
        val activity = mainActivity()
        activity.mostrarPanelColores()
        panelColoresRunnable?.let { panelColoresHandler.removeCallbacks(it) }
        panelEstaEnAutoShow = true
        panelColoresRunnable = Runnable {
            if (_binding != null) {
                activity.ocultarPanelColores()
                panelEstaEnAutoShow = false
            }
        }
        panelColoresHandler.postDelayed(panelColoresRunnable!!, 2000L)
    }

    fun onBotonResaltarTocado() {
        if (panelEstaEnAutoShow) {
            panelColoresRunnable?.let { panelColoresHandler.removeCallbacks(it) }
            panelColoresRunnable = null
            panelEstaEnAutoShow = false
        } else {
            val activity = mainActivity()
            if (activity.panelColoresVisible()) {
                activity.ocultarPanelColores()
            } else {
                activity.mostrarPanelColores()
            }
        }
    }

    // ── TTS ───────────────────────────────────────────────────────────────

    fun escucharCapitulo() {
        val versiculos = viewModel.versiculos.value ?: return
        if (versiculos.isEmpty()) return
        val intro = "$libroNombreDisplay, capítulo $capitulo"
        mainActivity().escucharVersiculos(intro, versiculos)
    }

    private fun escucharSeleccion() {
        val seleccionados = adapter.seleccionados.sorted()
        if (seleccionados.isEmpty()) return
        val todos = viewModel.versiculos.value ?: return
        val versiculosSeleccionados = todos
            .filter { it.versiculo in seleccionados }
            .sortedBy { it.versiculo }
        mainActivity().escucharVersiculos(null, versiculosSeleccionados)
    }

    // ─────────────────────────────────────────────────────────────────────

    private fun ocultarToolbarResaltado() {
        mainActivity().ocultarToolbarResaltado()
        adapter.modoSeleccion = false
    }

    private fun actualizarContadorSeleccion() { /* contador eliminado */ }
    private fun setupToolbarResaltado() { /* se configura al entrar en modo selección */ }

    private fun compartirSeleccion() {
        val seleccionados = adapter.seleccionados.sorted()
        if (seleccionados.isEmpty()) return

        val todosLosVersiculos = viewModel.versiculos.value ?: return
        val versiculosSeleccionados = todosLosVersiculos
            .filter { it.versiculo in seleccionados }
            .sortedBy { it.versiculo }

        val titulo = if (seleccionados.size == 1) {
            "$libroNombreDisplay $capitulo:${seleccionados.first()}"
        } else {
            "$libroNombreDisplay $capitulo:${seleccionados.first()}-${seleccionados.last()}"
        }

        val cuerpo = versiculosSeleccionados.joinToString("\n") { v ->
            "${v.versiculo}. ${v.texto}"
        }

        val texto = "$titulo\n\n$cuerpo"

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, texto)
        }
        startActivity(android.content.Intent.createChooser(intent, "Compartir versículo"))
    }

    private fun aplicarColor(color: Int?) {
        if (libroId == -1) return
        val seleccionados = adapter.seleccionados.toSet()
        seleccionados.forEach { numVersiculo ->
            if (color != null) {
                HighlightStore.setColor(requireContext(), libroId, capitulo, numVersiculo, color)
            } else {
                HighlightStore.removeColor(requireContext(), libroId, capitulo, numVersiculo)
            }
        }
        cargarColoresGuardados()
        adapter.modoSeleccion = false
    }

    // ── Swipe capítulos ───────────────────────────────────────────────────

    private fun setupSwipeGesture() {
        val density = resources.displayMetrics.density
        val swipeMinDistancePx      = (60  * density)
        val swipeMaxOffPathPx       = (40  * density)
        val swipeThresholdVelocity  = (50  * density)
        val decidirUmbralPx         = (8   * density)

        val detector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vX: Float, vY: Float): Boolean {
                val e1 = e1 ?: return false
                if (adapter.modoSeleccion) return false
                val diffY = kotlin.math.abs(e2.y - e1.y)
                val diffX = e2.x - e1.x
                if (diffY > swipeMaxOffPathPx) return false
                if (kotlin.math.abs(diffX) < swipeMinDistancePx) return false
                if (kotlin.math.abs(vX) < swipeThresholdVelocity) return false

                val caps = viewModel.libroSeleccionado.value?.capitulos ?: capitulosTotal
                if (diffX < 0 && capitulo < caps) { capitulo++; actualizarCapitulo(haciaDelante = true) }
                else if (diffX > 0 && capitulo > 1) { capitulo--; actualizarCapitulo(haciaDelante = false) }
                return true
            }
        })
        binding.recyclerView.addOnItemTouchListener(object : androidx.recyclerview.widget.RecyclerView.OnItemTouchListener {
            private var startX = 0f
            private var startY = 0f
            private var decidido = false
            private var esHorizontal = false

            override fun onInterceptTouchEvent(rv: androidx.recyclerview.widget.RecyclerView, e: MotionEvent): Boolean {
                detector.onTouchEvent(e)
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = e.x; startY = e.y
                        decidido = false; esHorizontal = false
                        rv.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!decidido) {
                            val dx = kotlin.math.abs(e.x - startX)
                            val dy = kotlin.math.abs(e.y - startY)
                            if (dx > decidirUmbralPx || dy > decidirUmbralPx) {
                                decidido = true
                                esHorizontal = dx > dy * 2.5f
                            }
                        }
                        if (esHorizontal && !adapter.modoSeleccion) {
                            rv.parent?.requestDisallowInterceptTouchEvent(true)
                            rv.stopScroll()
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        decidido = false; esHorizontal = false
                        rv.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                return false
            }

            override fun onTouchEvent(rv: androidx.recyclerview.widget.RecyclerView, e: MotionEvent) {
                detector.onTouchEvent(e)
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    private fun actualizarCapitulo(haciaDelante: Boolean = true) {
        requireActivity().title = "$libroNombreDisplay $capitulo"
        mainActivity().actualizarTituloClickable("$libroAbreviacion $capitulo")
        adapter.tituloCapitulo = "$libroNombreDisplay $capitulo"
        scrollTopOnNextUpdate = true
        viewModel.cargarVersiculos(libroId, capitulo)
        binding.fabVideosCapitulo.visibility = View.GONE
        binding.tvVideosBadge.visibility = View.GONE
        cargarColoresGuardados()
        verificarVideosDelCapitulo()
        val anim = AnimationUtils.loadAnimation(
            requireContext(),
            if (haciaDelante) R.anim.anim_slide_in_right else R.anim.anim_slide_in_left
        )
        binding.recyclerView.startAnimation(anim)
    }

    // ── ViewModel ─────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.versionActiva.observe(viewLifecycleOwner) { scrollTopOnNextUpdate = true }

        viewModel.libroSeleccionado.observe(viewLifecycleOwner) { libro ->
            libro?.let { capitulosTotal = it.capitulos }
        }

        viewModel.versiculos.observe(viewLifecycleOwner) { versiculos ->
            // IMPORTANTE: capturar y resetear el flag ANTES de llamar submitList,
            // para evitar condiciones de carrera donde el callback de submitList
            // se dispara sincrónicamente (si la lista no cambió) y consume el flag
            // antes de que la lógica de scroll se haya ejecutado.
            val debeScrollArriba = scrollTopOnNextUpdate
            if (debeScrollArriba) scrollTopOnNextUpdate = false

            adapter.submitList(versiculos) {
                if (debeScrollArriba) {
                    // post {} garantiza que el scroll ocurre DESPUÉS del layout pass
                    // de RecyclerView, evitando que las animaciones de DiffUtil o
                    // el propio layout lo anulen y provoquen scroll al último ítem.
                    binding.recyclerView.post {
                        (_binding?.recyclerView?.layoutManager as? LinearLayoutManager)
                            ?.scrollToPositionWithOffset(0, 0)
                    }
                    return@submitList
                }
                if (versiculoHighlight > 0 && versiculos.isNotEmpty()) {
                    val index = versiculos.indexOfFirst { it.versiculo == versiculoHighlight }
                    if (index >= 0) {
                        binding.recyclerView.postDelayed({
                            val rv = _binding?.recyclerView ?: return@postDelayed
                            // +1 porque la posición 0 del RecyclerView es el header
                            (rv.layoutManager as LinearLayoutManager)
                                .scrollToPositionWithOffset(index + 1, 100)
                        }, 300L)
                    }
                }
            }
        }
    }

    private fun verificarVideosDelCapitulo() {
        if (libroId == -1) return
        val videoRepository = VideoRepository(requireContext())
        lifecycleScope.launch {
            val videos = videoRepository.getByLibroCapitulo(libroId, capitulo)
            if (videos.isNotEmpty()) {
                val count = videos.size
                binding.fabVideosCapitulo.apply {
                    text = if (count == 1) "1 prédica de este capítulo" else "$count prédicas de este capítulo"
                    extend()
                    visibility = View.VISIBLE
                    setOnClickListener {
                        val activity = requireActivity() as MainActivity
                        activity.navegarAVideosConFiltro(libroId, capitulo)
                    }
                    postDelayed({
                        if (_binding != null) {
                            shrink()
                            binding.tvVideosBadge.text = if (count > 99) "99+" else count.toString()
                            binding.tvVideosBadge.visibility = View.VISIBLE
                        }
                    }, 1500L)
                }
            } else {
                binding.fabVideosCapitulo.visibility = View.GONE
            }
        }
    }

    // ── Mini selector de libros ────────────────────────────────────────────

    private fun mostrarMiniSelectorLibros() {
        val libros = viewModel.libros.value
        if (libros.isNullOrEmpty()) return

        val dialog = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_mini_libros, null)

        val adapterAntiguo = LibrosAdapter { libro ->
            dialog.dismiss()
            navegarALibroDesdeVersiculo(libro)
        }
        val adapterNuevo = LibrosAdapter { libro ->
            dialog.dismiss()
            navegarALibroDesdeVersiculo(libro)
        }

        sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerMiniAntiguo).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adapterAntiguo
        }
        sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerMiniNuevo).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adapterNuevo
        }

        adapterAntiguo.submitList(libros.filter { it.testamento == Constants.ANTIGUO_TESTAMENTO })
        adapterNuevo.submitList(libros.filter { it.testamento == Constants.NUEVO_TESTAMENTO })

        dialog.setContentView(sheetView)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<android.widget.FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(it)
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isDraggable = false
            }
        }

        dialog.show()
    }

    private fun navegarALibroDesdeVersiculo(libro: Libro) {
        val args = Bundle().apply {
            putInt(Constants.ARG_LIBRO_ID, libro.id)
            putString(Constants.ARG_LIBRO_NOMBRE, libro.nombre)
            putString(Constants.ARG_LIBRO_ABREVIACION, libro.abreviacion)
            putInt(Constants.ARG_CAPITULOS_TOTAL, libro.capitulos)
        }
        findNavController().navigate(R.id.action_versiculos_to_capitulos_mini, args)
    }

    // ─────────────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        requireActivity().title = "$libroNombreDisplay $capitulo"
        mainActivity().mostrarTituloClickable("$libroAbreviacion $capitulo") {
            mostrarMiniSelectorLibros()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        panelColoresRunnable?.let { panelColoresHandler.removeCallbacks(it) }
        panelEstaEnAutoShow = false
        (activity as? MainActivity)?.let {
            it.ocultarTituloClickable()
            it.ocultarToolbarResaltado()
            it.escucharVersiculos(null, emptyList())
        }
        _binding = null
    }
}