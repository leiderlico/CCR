package com.leiderl.CCR.ui.fragments

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.leiderl.CCR.R
import com.leiderl.CCR.data.repository.HighlightStore
import com.leiderl.CCR.databinding.FragmentVideoPlayerBinding
import com.leiderl.CCR.ui.adapters.CapitulosAdapter
import com.leiderl.CCR.ui.adapters.LibrosAdapter
import com.leiderl.CCR.ui.adapters.VersiculosAdapter
import com.leiderl.CCR.ui.viewmodels.BibliaViewModel


class VideoPlayerFragment : Fragment() {

    private var _binding: FragmentVideoPlayerBinding? = null
    private val binding get() = _binding!!
    private val bibliaViewModel: BibliaViewModel by activityViewModels()

    private enum class PanelState { HIDDEN, LIBROS, CAPITULOS, VERSICULOS }
    private var panelState = PanelState.HIDDEN

    private lateinit var versiculosAdapter: VersiculosAdapter
    private lateinit var capitulosAdapter: CapitulosAdapter
    private lateinit var librosAntiguoAdapter: LibrosAdapter
    private lateinit var librosNuevoAdapter: LibrosAdapter

    private var panelLibroId: Int = 0
    private var panelCapitulo: Int = 0
    private var panelLibroNombre: String = ""

    private var videoLibroId: Int = 0
    private var videoCapitulo: Int = 0

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

    private var fullscreenView: View? = null
    private var fullscreenContainer: FrameLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val url        = arguments?.getString("urlYoutube") ?: return
        val titulo     = arguments?.getString("titulo") ?: ""
        val predicador = arguments?.getString("predicador") ?: ""
        val cita       = arguments?.getString("cita") ?: ""
        videoLibroId   = arguments?.getInt("libroId", 0) ?: 0
        videoCapitulo  = arguments?.getInt("capitulo", 0) ?: 0

        requireActivity().title = "REPRODUCTOR"
        binding.tvVideoPlayerTitulo.text = titulo
        binding.tvVideoPlayerCita.text = cita
        binding.tvVideoPlayerPredicador.text = predicador

        setupPanelBiblia()

        if (videoLibroId > 0 && videoCapitulo > 0 && cita.isNotEmpty()) {
            binding.ivExpandirBiblia.visibility = View.VISIBLE
            binding.layoutCitaExpandir.setOnClickListener { togglePanel() }
        }

        fullscreenContainer = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        (requireActivity().window.decorView as FrameLayout).addView(fullscreenContainer)

        val videoId = extraerYoutubeId(url)
        if (videoId != null) configurarWebView(videoId) else configurarWebViewUrl(url)
    }

    // ─────────────────────────────────────────────────
    // Setup panel bíblico
    // ─────────────────────────────────────────────────

    private fun setupPanelBiblia() {
        // Versículos
        versiculosAdapter = VersiculosAdapter()
        binding.rvVersiculos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = versiculosAdapter
            isNestedScrollingEnabled = false
        }

        // Capítulos (grid 5 col)
        capitulosAdapter = CapitulosAdapter { cap -> mostrarVersiculos(panelLibroId, cap) }
        binding.rvCapitulos.apply {
            layoutManager = GridLayoutManager(requireContext(), 5)
            adapter = capitulosAdapter
            isNestedScrollingEnabled = false
        }

        // Libros — dos adapters, uno por testamento
        librosAntiguoAdapter = LibrosAdapter { libro -> mostrarCapitulos(libro.id, libro.nombre, libro.capitulos) }
        binding.rvLibrosAntiguo.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = librosAntiguoAdapter
            isNestedScrollingEnabled = false
        }

        librosNuevoAdapter = LibrosAdapter { libro -> mostrarCapitulos(libro.id, libro.nombre, libro.capitulos) }
        binding.rvLibrosNuevo.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = librosNuevoAdapter
            isNestedScrollingEnabled = false
        }

        // Observar libros y dividirlos en AT/NT
        bibliaViewModel.libros.observe(viewLifecycleOwner) { libros ->
            librosAntiguoAdapter.submitList(libros.filter { it.testamento == "Antiguo" })
            librosNuevoAdapter.submitList(libros.filter { it.testamento == "Nuevo" })
        }

        // Spinner versiones
        val versiones = BibliaViewModel.VERSIONES_DISPONIBLES
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            versiones.map { it.nombre }
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerVersion.adapter = spinnerAdapter

        bibliaViewModel.versionActiva.observe(viewLifecycleOwner) { versionActiva ->
            val index = versiones.indexOfFirst { it.nombre == versionActiva.nombre }
            if (index >= 0) binding.spinnerVersion.setSelection(index)
            if (panelState == PanelState.VERSICULOS) {
                bibliaViewModel.cargarVersiculos(panelLibroId, panelCapitulo)
            }
        }

        binding.spinnerVersion.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, v: View?, pos: Int, id: Long) {
                val version = versiones[pos]
                if (version.nombre != bibliaViewModel.versionActiva.value?.nombre) {
                    bibliaViewModel.cambiarVersion(version, panelLibroId, panelCapitulo)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        bibliaViewModel.versiculos.observe(viewLifecycleOwner) { versiculos ->
            if (panelState == PanelState.VERSICULOS) versiculosAdapter.submitList(versiculos)
        }

        setupSwipeVersiculos()

        // Flecha atrás: versículos→capítulos, capítulos→libros
        binding.ivBibliaBack.setOnClickListener {
            when (panelState) {
                PanelState.VERSICULOS -> mostrarCapitulos(panelLibroId, panelLibroNombre)
                PanelState.CAPITULOS  -> mostrarLibros()
                else -> {}
            }
        }
    }

    // ─────────────────────────────────────────────────
    // Navegación entre estados
    // ─────────────────────────────────────────────────

    private fun setupSwipeVersiculos() {
        val detector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_MIN_DISTANCE = 100f
            private val SWIPE_MAX_OFF_PATH = 80f
            private val SWIPE_THRESHOLD_VELOCITY = 100f

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vX: Float, vY: Float): Boolean {
                val e1 = e1 ?: return false
                if (panelState != PanelState.VERSICULOS) return false
                val diffY = kotlin.math.abs(e2.y - e1.y)
                val diffX = e2.x - e1.x
                if (diffY > SWIPE_MAX_OFF_PATH) return false
                if (kotlin.math.abs(diffX) < SWIPE_MIN_DISTANCE) return false
                if (kotlin.math.abs(vX) < SWIPE_THRESHOLD_VELOCITY) return false

                val totalCaps = bibliaViewModel.libroSeleccionado.value?.capitulos ?: Int.MAX_VALUE
                if (diffX < 0 && panelCapitulo < totalCaps) {
                    mostrarVersiculos(panelLibroId, panelCapitulo + 1)
                } else if (diffX > 0 && panelCapitulo > 1) {
                    mostrarVersiculos(panelLibroId, panelCapitulo - 1)
                }
                return true
            }
        })

        binding.rvVersiculos.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            false
        }

        // Swipe en capítulos → cambia de libro
        val detectorCaps = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_MIN_DISTANCE = 100f
            private val SWIPE_MAX_OFF_PATH = 80f
            private val SWIPE_THRESHOLD_VELOCITY = 100f

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vX: Float, vY: Float): Boolean {
                val e1 = e1 ?: return false
                if (panelState != PanelState.CAPITULOS) return false
                val diffY = kotlin.math.abs(e2.y - e1.y)
                val diffX = e2.x - e1.x
                if (diffY > SWIPE_MAX_OFF_PATH) return false
                if (kotlin.math.abs(diffX) < SWIPE_MIN_DISTANCE) return false
                if (kotlin.math.abs(vX) < SWIPE_THRESHOLD_VELOCITY) return false

                val libros = bibliaViewModel.libros.value ?: return false
                val idx = libros.indexOfFirst { it.id == panelLibroId }
                if (idx < 0) return false

                val destino = if (diffX < 0 && idx < libros.size - 1) libros[idx + 1]
                else if (diffX > 0 && idx > 0) libros[idx - 1]
                else return false

                mostrarCapitulos(destino.id, destino.nombre, destino.capitulos)
                return true
            }
        })

        binding.rvCapitulos.setOnTouchListener { _, event ->
            detectorCaps.onTouchEvent(event)
            false
        }
    }

    private fun togglePanel() {
        when (panelState) {
            PanelState.HIDDEN -> {
                panelLibroId     = videoLibroId
                panelCapitulo    = videoCapitulo
                panelLibroNombre = nombresLibros[videoLibroId] ?: "Libro $videoLibroId"
                mostrarVersiculos(videoLibroId, videoCapitulo)
            }
            else -> {
                panelState = PanelState.HIDDEN
                binding.panelBiblia.visibility = View.GONE
                binding.ivExpandirBiblia.setImageResource(R.drawable.ic_expand_more)
            }
        }
    }

    private fun mostrarLibros() {
        panelState = PanelState.LIBROS
        binding.tvBibliaHeader.visibility = View.GONE
        binding.ivBibliaBack.visibility = View.GONE
        binding.spinnerVersion.visibility = View.GONE
        binding.panelLibros.visibility = View.VISIBLE
        binding.rvCapitulos.visibility = View.GONE
        binding.rvVersiculos.visibility = View.GONE
        binding.panelBiblia.visibility = View.VISIBLE
        binding.ivExpandirBiblia.setImageResource(R.drawable.ic_expand_less)
    }

    private fun mostrarCapitulos(libroId: Int, nombreLibro: String, totalCaps: Int = 0) {
        panelLibroId     = libroId
        panelLibroNombre = nombreLibro
        panelState       = PanelState.CAPITULOS

        if (totalCaps > 0) {
            capitulosAdapter.submitList((1..totalCaps).toList())
        } else {
            bibliaViewModel.cargarLibroParaNavegacion(libroId) { libro ->
                val caps = libro?.capitulos ?: 0
                capitulosAdapter.submitList((1..caps).toList())
            }
        }

        // El header del libro es clickeable → va a libros
        binding.tvBibliaHeader.visibility = View.VISIBLE
        binding.tvBibliaHeader.text = nombreLibro
        binding.tvBibliaHeader.setOnClickListener { mostrarLibros() }
        binding.ivBibliaBack.visibility = View.VISIBLE
        binding.spinnerVersion.visibility = View.GONE
        binding.panelLibros.visibility = View.GONE
        binding.rvCapitulos.visibility = View.VISIBLE
        binding.rvVersiculos.visibility = View.GONE
        binding.panelBiblia.visibility = View.VISIBLE
        binding.ivExpandirBiblia.setImageResource(R.drawable.ic_expand_less)
    }

    private fun mostrarVersiculos(libroId: Int, capitulo: Int) {
        panelLibroId     = libroId
        panelCapitulo    = capitulo
        panelLibroNombre = nombresLibros[libroId] ?: "Libro $libroId"
        panelState       = PanelState.VERSICULOS

        binding.tvBibliaHeader.visibility = View.VISIBLE
        binding.tvBibliaHeader.text = "$panelLibroNombre $capitulo"
        binding.tvBibliaHeader.setOnClickListener(null)
        binding.ivBibliaBack.visibility = View.VISIBLE
        binding.spinnerVersion.visibility = View.VISIBLE
        binding.panelLibros.visibility = View.GONE
        binding.rvCapitulos.visibility = View.GONE
        binding.rvVersiculos.visibility = View.VISIBLE
        binding.panelBiblia.visibility = View.VISIBLE
        binding.ivExpandirBiblia.setImageResource(R.drawable.ic_expand_less)

        bibliaViewModel.cargarVersiculos(libroId, capitulo)

        // Cargar colores guardados (solo lectura, sin selección)
        val colores = HighlightStore.getColoresCapitulo(requireContext(), libroId, capitulo)
        versiculosAdapter.highlightColors = colores
    }

    // ─────────────────────────────────────────────────
    // WebView
    // ─────────────────────────────────────────────────

    private fun configurarWebView(videoId: String) {
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    fullscreenView = view
                    fullscreenContainer?.apply {
                        addView(view, FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        ))
                        visibility = View.VISIBLE
                    }
                    requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    requireActivity().window.decorView.systemUiVisibility =
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                }
                override fun onHideCustomView() {
                    fullscreenContainer?.apply {
                        removeView(fullscreenView)
                        visibility = View.GONE
                    }
                    fullscreenView = null
                    requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }

            val baseUrl = "https://com.leiderl.bibliaccr"
            val html = """
                <!DOCTYPE html><html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <meta name="referrer" content="strict-origin-when-cross-origin">
                    <style>
                        * { margin:0; padding:0; box-sizing:border-box; }
                        body { background:#000; width:100vw; height:100vh; overflow:hidden; }
                        .video-container { position:relative; width:100%; padding-bottom:56.25%; height:0; }
                        iframe { position:absolute; top:0; left:0; width:100%; height:100%; border:none; }
                    </style>
                </head>
                <body>
                    <div class="video-container">
                        <iframe
                            src="https://www.youtube.com/embed/$videoId?autoplay=1&rel=0&playsinline=0&origin=https://com.leiderl.bibliaccr"
                            referrerpolicy="strict-origin-when-cross-origin"
                            allow="autoplay; encrypted-media; fullscreen; accelerometer; gyroscope; picture-in-picture"
                            allowfullscreen></iframe>
                    </div>
                </body></html>
            """.trimIndent()
            loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", null)
        }
    }

    private fun configurarWebViewUrl(url: String) {
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            loadUrl(url)
        }
    }

    private fun extraerYoutubeId(url: String): String? {
        listOf(
            Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/embed/([a-zA-Z0-9_-]{11})")
        ).forEach { pattern ->
            pattern.find(url)?.let { return it.groupValues[1] }
        }
        return null
    }

    override fun onDestroyView() {
        fullscreenContainer?.let {
            it.removeAllViews()
            (requireActivity().window.decorView as? FrameLayout)?.removeView(it)
        }
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        binding.webView.destroy()
        super.onDestroyView()
        _binding = null
    }
}