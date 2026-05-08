package com.leiderl.CCR.ui

import android.Manifest
import android.os.Build
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListPopupWindow
import android.graphics.drawable.ColorDrawable
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.leiderl.CCR.R
import com.leiderl.CCR.databinding.ActivityMainBinding
import com.leiderl.CCR.databinding.ItemSuggestionCardBinding
import com.leiderl.CCR.data.database.entities.Versiculo
import com.leiderl.CCR.services.CapituloTtsManager
import com.leiderl.CCR.services.Sugerencia
import com.leiderl.CCR.services.VoiceRecognitionManager
import com.leiderl.CCR.ui.viewmodels.BibliaViewModel
import com.leiderl.CCR.data.repository.utils.Constants
import com.leiderl.CCR.services.NotificationScheduler
import com.leiderl.CCR.services.RadioService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: BibliaViewModel by viewModels()
    private var voiceManager: VoiceRecognitionManager? = null
    private var ttsManager: CapituloTtsManager? = null
    private var dialogoDescarga: androidx.appcompat.app.AlertDialog? = null

    private val sugerenciasAcumuladas = mutableListOf<Sugerencia>()
    // Sin límite de sugerencias — se acumulan indefinidamente

    // Debounce para la búsqueda por texto
    private var searchJob: Job? = null
    private val SEARCH_DEBOUNCE_MS = 200L

    private val destinosBiblia = setOf(R.id.librosFragment)
    private val destinosRaiz = setOf(R.id.librosFragment, R.id.gruposSelectorFragment, R.id.devocionalFragment, R.id.juegoFragment)
    private val destinoVersionSelector = R.id.versiculosFragment

    private var libroIdActual: Int = -1
    private var capituloActual: Int = 1

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) iniciarMicrofono()
        else Toast.makeText(this, "Se necesita permiso de micrófono", Toast.LENGTH_LONG).show()
    }

    private val requestNotifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) NotificationScheduler.programarTodas(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val savedMode = getSharedPreferences("ccr_prefs", MODE_PRIVATE)
            .getInt("night_mode", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(savedMode)

        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestNotifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                NotificationScheduler.programarTodas(this)
            }
        } else {
            NotificationScheduler.programarTodas(this)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.toolbar.navigationIcon?.setTint(
            ContextCompat.getColor(this, R.color.toolbar_title_text)
        )

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)
        binding.bottomNav.itemIconTintList =
            androidx.core.content.ContextCompat.getColorStateList(this, R.color.bottom_nav_item_color)

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            binding.toolbar.navigationIcon?.setTint(
                ContextCompat.getColor(this, android.R.color.white)
            )

            if (!modoSeleccionActivo) title = if (destination.id in destinosRaiz) "CCR" else ""
            binding.appBarLayout.setExpanded(true, false)

            val esRaiz = destination.id in destinosRaiz
            val esBiblia = destination.id in destinosBiblia
            val esVersionSelector = destination.id == destinoVersionSelector

            supportActionBar?.setDisplayHomeAsUpEnabled(!esRaiz)
            binding.bottomNav.visibility = if (esRaiz) View.VISIBLE else View.GONE
            invalidateOptionsMenu()

            binding.micIndicator.visibility = if (esBiblia) View.VISIBLE else View.GONE
            binding.searchIndicator.visibility = if (esBiblia) View.VISIBLE else View.GONE
            binding.toolbar.getChildAt(0)?.visibility = if (esRaiz) View.VISIBLE else View.GONE

            // Si salimos de bíblia, cerramos la barra de búsqueda
            if (!esBiblia) cerrarBarraBusqueda()

            if (!modoSeleccionActivo) {
                if (esVersionSelector) {
                    libroIdActual  = arguments?.getInt(Constants.ARG_LIBRO_ID, -1) ?: -1
                    capituloActual = arguments?.getInt(Constants.ARG_CAPITULO, 1) ?: 1
                    actualizarBotonVersion(viewModel.versionActiva.value ?: BibliaViewModel.VERSION_DEFAULT)
                    binding.btnVersion.visibility = View.VISIBLE
                } else {
                    binding.btnVersion.visibility = View.GONE
                }
            }

            if (esBiblia && viewModel.voiceEnabled.value == true && hasMicPermission()) {
                iniciarMicrofono()
            } else if (!esBiblia) {
                detenerMicrofono()
                limpiarTodasLasSugerencias()
            }
        }

        setupVersionButton()
        setupObservers()
        setupControls()
        setupSwipeTabs()
        setupSearchBar()
        ttsManager = CapituloTtsManager(this)
    }

    // ── Barra de búsqueda por texto ───────────────────────────────────────

    private fun setupSearchBar() {
        binding.searchIndicator.setOnClickListener {
            if (binding.searchBarCard.visibility == View.VISIBLE) {
                cerrarBarraBusqueda(limpiarSugerencias = true)
            } else {
                abrirBarraBusqueda()
            }
        }

        binding.btnCloseSearch.setOnClickListener {
            cerrarBarraBusqueda(limpiarSugerencias = false)
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                cerrarBarraBusqueda(limpiarSugerencias = false)
                true
            } else false
        }

        // TextWatcher con debounce — mismo comportamiento que el micrófono
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val texto = s?.toString()?.trim() ?: return
                if (texto.length < 3) return  // mínimo 3 caracteres
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(SEARCH_DEBOUNCE_MS)
                    viewModel.procesarVoz(texto)
                }
            }
        })
    }

    private fun abrirBarraBusqueda() {
        binding.searchBarCard.visibility = View.VISIBLE
        binding.etSearch.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        binding.searchIndicator.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.mic_active_color)
    }

    private fun cerrarBarraBusqueda(limpiarSugerencias: Boolean = false) {
        binding.searchBarCard.visibility = View.GONE
        binding.etSearch.text?.clear()
        searchJob?.cancel()
        cerrarTeclado()
        binding.searchIndicator.backgroundTintList =
            ContextCompat.getColorStateList(this, android.R.color.darker_gray)
        if (limpiarSugerencias) limpiarTodasLasSugerencias()
    }

    private fun cerrarTeclado() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    // ── Título clickable del toolbar (VersiculosFragment) ─────────────────
    // Muestra el nombre del libro + capítulo con un ▾ que abre el selector de libros.

    fun mostrarTituloClickable(titulo: String, onClick: () -> Unit) {
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.tvCustomTitle.text = titulo
        binding.llCustomTitle.visibility = View.VISIBLE
        binding.llCustomTitle.setOnClickListener { onClick() }
    }

    fun actualizarTituloClickable(titulo: String) {
        if (binding.llCustomTitle.visibility == View.VISIBLE) {
            binding.tvCustomTitle.text = titulo
        }
    }

    fun ocultarTituloClickable() {
        binding.llCustomTitle.visibility = View.GONE
        supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    // ─────────────────────────────────────────────────────────────────────

    private fun setupVersionButton() {
        binding.btnVersion.setOnClickListener { boton ->
            val versiones = BibliaViewModel.VERSIONES_DISPONIBLES
            val nombres = versiones.map { v ->
                if (viewModel.isVersionDisponible(v)) v.nombreCompleto
                else "⬇ ${v.nombreCompleto}"
            }

            val adapter = ArrayAdapter(this, R.layout.item_version_popup, R.id.tvVersionNombre, nombres)

            val listPopup = ListPopupWindow(this)
            listPopup.setAdapter(adapter)
            listPopup.anchorView = boton

            val paint = android.graphics.Paint().apply {
                textSize = 15 * resources.displayMetrics.scaledDensity
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            }
            val paddingPx = (24 * resources.displayMetrics.density).toInt()
            val maxTextWidth = nombres.maxOf { paint.measureText(it).toInt() }
            listPopup.width = maxTextWidth + paddingPx

            listPopup.isModal = true
            listPopup.setBackgroundDrawable(
                ColorDrawable(ContextCompat.getColor(this, R.color.toolbar_popup_background))
            )
            listPopup.setOnItemClickListener { _, _, position, _ ->
                val versionElegida = versiones[position]
                listPopup.dismiss()
                if (viewModel.isVersionDisponible(versionElegida)) {
                    viewModel.cambiarVersion(versionElegida, libroIdActual, capituloActual)
                } else {
                    mostrarDialogoDescarga(versionElegida)
                }
            }
            listPopup.show()
        }

        viewModel.versionActiva.observe(this) { version ->
            if (binding.btnVersion.visibility == View.VISIBLE) {
                actualizarBotonVersion(version)
            }
        }
    }

    private fun mostrarDialogoDescarga(version: BibliaViewModel.Version) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Descargar ${version.nombreCompleto}")
            .setMessage("Esta versión (~6 MB) se descargará una sola vez y quedará disponible sin internet. ¿Continuar?")
            .setPositiveButton("Descargar") { _, _ ->
                viewModel.descargarVersion(version, libroIdActual, capituloActual)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarBotonVersion(version: BibliaViewModel.Version) {
        binding.btnVersion.text = "${version.nombre} ▾"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        val esRaiz = navController.currentDestination?.id in destinosRaiz
        menu.findItem(R.id.action_menu)?.isVisible = esRaiz
        actualizarIconoModoNoche(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val esRaiz = navController.currentDestination?.id in destinosRaiz
        val esVersiculos = navController.currentDestination?.id == destinoVersionSelector

        menu.findItem(R.id.action_menu)?.isVisible = if (modoSeleccionActivo) false else esRaiz
        menu.findItem(R.id.action_radio)?.apply {
            isVisible = if (modoSeleccionActivo) false else esRaiz
            setIcon(if (RadioService.estaActiva) R.drawable.ic_radioon else R.drawable.ic_radio)
        }
        menu.findItem(R.id.action_modo_noche)?.isVisible = !modoSeleccionActivo
        menu.findItem(R.id.action_menu_versiculos)?.isVisible = esVersiculos && !modoSeleccionActivo
        menu.findItem(R.id.action_escuchar)?.isVisible = modoSeleccionActivo
        menu.findItem(R.id.action_compartir)?.isVisible = modoSeleccionActivo
        menu.findItem(R.id.action_resaltar)?.isVisible = modoSeleccionActivo

        if (!modoSeleccionActivo) actualizarIconoModoNoche(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    private fun actualizarIconoModoNoche(menu: Menu) {
        val esNoche = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        menu.findItem(R.id.action_modo_noche)?.apply {
            setIcon(if (esNoche) R.drawable.noche else R.drawable.dia)
            icon?.mutate()?.setTint(android.graphics.Color.WHITE)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_radio -> { mostrarRadio(); true }
            R.id.action_modo_noche -> {
                val esNoche = resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES
                val nuevoModo = if (esNoche)
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                else
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nuevoModo)
                getSharedPreferences("ccr_prefs", MODE_PRIVATE)
                    .edit().putInt("night_mode", nuevoModo).apply()
                true
            }
            R.id.action_menu -> { mostrarMenuBottomSheet(); true }
            R.id.action_menu_versiculos -> { mostrarMenuVersiculos(); true }
            R.id.action_compartir -> { onCompartirSeleccion?.invoke(); true }
            R.id.action_resaltar -> {
                val versFragment = supportFragmentManager.primaryNavigationFragment
                    ?.childFragmentManager?.fragments
                    ?.filterIsInstance<com.leiderl.CCR.ui.fragments.VersiculosFragment>()
                    ?.firstOrNull()
                if (versFragment != null) {
                    versFragment.onBotonResaltarTocado()
                } else {
                    binding.panelColores.visibility =
                        if (binding.panelColores.visibility == android.view.View.VISIBLE)
                            android.view.View.GONE else android.view.View.VISIBLE
                }
                true
            }
            R.id.action_escuchar -> { onEscucharSeleccion?.invoke(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ── Menú 3 puntos en versículos — solo ícono, sin texto ──────────────
    private fun mostrarMenuVersiculos() {
        val anchorView = binding.toolbar.findViewById<View>(R.id.action_menu_versiculos) ?: binding.toolbar

        val iconos = listOf(R.drawable.ic_escuchar)

        val adapter = object : android.widget.BaseAdapter() {
            override fun getCount() = iconos.size
            override fun getItem(pos: Int) = iconos[pos]
            override fun getItemId(pos: Int) = pos.toLong()
            override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.item_menu_solo_icono, parent, false)
                view.findViewById<ImageView>(R.id.ivMenuIcono).setImageResource(iconos[pos])
                return view
            }
        }

        val listPopup = ListPopupWindow(this)
        listPopup.setAdapter(adapter)
        listPopup.anchorView = anchorView
        listPopup.width = (64 * resources.displayMetrics.density).toInt()
        listPopup.isModal = true
        listPopup.setBackgroundDrawable(
            ColorDrawable(ContextCompat.getColor(this, R.color.toolbar_popup_background))
        )
        listPopup.setOnItemClickListener { _, _, position, _ ->
            listPopup.dismiss()
            when (position) {
                0 -> {
                    val versFragment = supportFragmentManager.primaryNavigationFragment
                        ?.childFragmentManager?.fragments
                        ?.filterIsInstance<com.leiderl.CCR.ui.fragments.VersiculosFragment>()
                        ?.firstOrNull()
                    versFragment?.escucharCapitulo()
                }
            }
        }
        listPopup.show()
    }

    // ── Menú sandwich principal ───────────────────────────────────────────
    private fun mostrarMenuBottomSheet() {
        val anchorView = binding.toolbar.findViewById<View>(R.id.action_menu) ?: binding.toolbar
        val opciones = listOf("Peticiones", "Acerca de CCR", "Compartir app", "Configuración")
        val adapter = ArrayAdapter(this, R.layout.item_version_popup, R.id.tvVersionNombre, opciones)

        val listPopup = ListPopupWindow(this)
        listPopup.setAdapter(adapter)
        listPopup.anchorView = anchorView

        val paint = android.graphics.Paint().apply {
            textSize = 15 * resources.displayMetrics.scaledDensity
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }
        val paddingPx = (24 * resources.displayMetrics.density).toInt()
        val maxTextWidth = opciones.maxOf { paint.measureText(it).toInt() }
        listPopup.width = maxTextWidth + paddingPx

        listPopup.isModal = true
        listPopup.setBackgroundDrawable(
            ColorDrawable(ContextCompat.getColor(this, R.color.toolbar_popup_background))
        )
        listPopup.setOnItemClickListener { _, _, position, _ ->
            listPopup.dismiss()
            when (position) {
                0 -> mostrarPeticiones()
                1 -> mostrarAcercaDeCCR()
                2 -> compartirApp()
                3 -> { /* Configuración — próximamente */ }
            }
        }
        listPopup.show()
    }

    private fun mostrarRadio() { navController.navigate(R.id.radioFragment) }
    private fun mostrarPeticiones() { navController.navigate(R.id.peticionesFragment) }
    private fun mostrarAcercaDeCCR() { navController.navigate(R.id.acercaFragment) }

    private fun compartirApp() {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, "https://tu-link-aqui.com")
        }
        startActivity(android.content.Intent.createChooser(intent, "Compartir Biblia CCR"))
    }

    // ── Toolbar modo selección ────────────────────────────────────────────

    private var onColorSeleccionado: ((Int) -> Unit)? = null
    private var onBorrarResaltado: (() -> Unit)? = null
    private var onCancelarSeleccion: (() -> Unit)? = null
    private var onSalirSeleccion: (() -> Unit)? = null
    private var onCompartirSeleccion: (() -> Unit)? = null
    private var onEscucharSeleccion: (() -> Unit)? = null
    var modoSeleccionActivo = false
        private set

    fun entrarModoSeleccion(
        onCancelar: () -> Unit,
        onSalir: () -> Unit,
        onColor: (Int) -> Unit,
        onBorrar: () -> Unit,
        onCompartir: () -> Unit,
        onEscuchar: () -> Unit,
        colores: List<Int>
    ) {
        modoSeleccionActivo = true
        onCancelarSeleccion = onCancelar
        onSalirSeleccion = onSalir
        onColorSeleccionado = onColor
        onBorrarResaltado = onBorrar
        onCompartirSeleccion = onCompartir
        onEscucharSeleccion = onEscuchar

        val chips = listOf(
            binding.colorChip1, binding.colorChip2, binding.colorChip3,
            binding.colorChip4, binding.colorChip5, binding.colorChip6
        )
        chips.forEachIndexed { i, chip ->
            chip.setBackgroundColor(colores[i])
            chip.setOnClickListener { onColor(colores[i]); salirModoSeleccion() }
        }
        binding.colorChipBorrar.setOnClickListener { onBorrar(); salirModoSeleccion() }

        invalidateOptionsMenu()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel)
        binding.btnVersion.visibility = View.GONE
    }

    fun salirModoSeleccion() {
        modoSeleccionActivo = false
        ttsManager?.detener()
        onCancelarSeleccion?.invoke()
        onCancelarSeleccion = null
        onColorSeleccionado = null
        onBorrarResaltado = null
        onCompartirSeleccion = null
        onEscucharSeleccion = null
        binding.panelColores.visibility = android.view.View.GONE
        invalidateOptionsMenu()
        supportActionBar?.setHomeAsUpIndicator(null)
        if (navController.currentDestination?.id == destinoVersionSelector) {
            binding.btnVersion.visibility = View.VISIBLE
        }
    }

    fun ocultarToolbarResaltado() {
        if (modoSeleccionActivo) salirModoSeleccion()
    }

    fun escucharVersiculos(intro: String?, versiculos: List<Versiculo>) {
        if (versiculos.isEmpty()) {
            ttsManager?.detener()
            return
        }
        if (ttsManager?.estaHablando() == true) {
            ttsManager?.detener()
            return
        }
        val textos = mutableListOf<String>()
        if (!intro.isNullOrBlank()) textos.add(intro)
        versiculos.forEach { textos.add(it.texto) }
        ttsManager?.hablar(textos)
    }

    fun actualizarContadorResaltado(contador: String) { }

    fun mostrarPanelColores() {
        binding.panelColores.visibility = android.view.View.VISIBLE
    }

    fun panelColoresVisible(): Boolean =
        binding.panelColores.visibility == android.view.View.VISIBLE

    fun ocultarPanelColores() {
        binding.panelColores.visibility = android.view.View.GONE
    }

    fun navegarAVideosConFiltro(libroId: Int, capitulo: Int) {
        // Navegamos directo a VideosFragment SIN limpiar el back stack,
        // para que al presionar ← el usuario regrese al capítulo donde estaba.
        val args = Bundle().apply {
            putInt("filtroLibroId", libroId)
            putInt("filtroCapitulo", capitulo)
        }
        navController.navigate(R.id.action_versiculos_to_videos_filtro, args)
    }

    private val tabsOrden = listOf(
        R.id.librosFragment,
        R.id.gruposSelectorFragment,
        R.id.devocionalFragment,
        R.id.juegoFragment
    )
    private lateinit var swipeTabDetector: GestureDetector

    private fun setupSwipeTabs() {
        val density = resources.displayMetrics.density
        val swipeMinDistancePx     = (50  * density)
        val swipeMaxOffPathPx      = (40  * density)
        val swipeThresholdVelocity = (60  * density)

        swipeTabDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vX: Float, vY: Float): Boolean {
                val e1 = e1 ?: return false
                val currentDest = navController.currentDestination?.id ?: return false
                if (currentDest !in tabsOrden) return false

                val diffY = kotlin.math.abs(e2.y - e1.y)
                val diffX = e2.x - e1.x
                if (diffY > swipeMaxOffPathPx) return false
                if (kotlin.math.abs(diffX) < swipeMinDistancePx) return false
                if (kotlin.math.abs(vX) < swipeThresholdVelocity) return false

                val idx = tabsOrden.indexOf(currentDest)
                val destino = if (diffX < 0 && idx < tabsOrden.size - 1) tabsOrden[idx + 1]
                else if (diffX > 0 && idx > 0) tabsOrden[idx - 1]
                else return false

                binding.bottomNav.selectedItemId = destino
                return true
            }
        })
    }

    private var tabSwipeStartX = 0f
    private var tabSwipeStartY = 0f
    private var tabSwipeDecidido = false
    private var tabSwipeEsHorizontal = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val density = resources.displayMetrics.density
        val decidirUmbralPx = (10 * density)

        val currentDest = navController.currentDestination?.id
        val enRaiz = currentDest in tabsOrden

        if (::swipeTabDetector.isInitialized && enRaiz) {
            swipeTabDetector.onTouchEvent(ev)

            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    tabSwipeStartX = ev.x; tabSwipeStartY = ev.y
                    tabSwipeDecidido = false; tabSwipeEsHorizontal = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!tabSwipeDecidido) {
                        val dx = kotlin.math.abs(ev.x - tabSwipeStartX)
                        val dy = kotlin.math.abs(ev.y - tabSwipeStartY)
                        if (dx > decidirUmbralPx || dy > decidirUmbralPx) {
                            tabSwipeDecidido = true
                            tabSwipeEsHorizontal = dx > dy * 2.0f
                        }
                    }
                    if (tabSwipeEsHorizontal) {
                        val cancel = MotionEvent.obtain(ev).also {
                            it.action = MotionEvent.ACTION_CANCEL
                        }
                        super.dispatchTouchEvent(cancel)
                        cancel.recycle()
                        swipeTabDetector.onTouchEvent(ev)
                        return true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    tabSwipeDecidido = false; tabSwipeEsHorizontal = false
                }
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { loading ->
            binding.loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.isDbReady.observe(this) { ready ->
            if (ready && viewModel.voiceEnabled.value == true) {
                checkAndRequestMicPermission()
            }
        }

        viewModel.descargaProgreso.observe(this) { progreso ->
            if (progreso != null) {
                if (dialogoDescarga == null) {
                    val version = viewModel.descargaVersion.value
                    dialogoDescarga = androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Descargando ${version?.nombreCompleto ?: ""}")
                        .setMessage("$progreso%")
                        .setCancelable(false)
                        .create()
                    dialogoDescarga?.show()
                } else {
                    dialogoDescarga?.setMessage("$progreso%")
                }
            } else {
                dialogoDescarga?.dismiss()
                dialogoDescarga = null
            }
        }

        viewModel.sugerencia.observe(this) { sugerencia ->
            sugerencia ?: return@observe
            val existente = sugerenciasAcumuladas.indexOfFirst { it.referencia == sugerencia.referencia }
            if (existente >= 0) {
                sugerenciasAcumuladas[existente] = sugerencia
                actualizarCardExistente(sugerencia)
            } else {
                sugerenciasAcumuladas.add(sugerencia)
                agregarNuevoCard(sugerencia)
                // Sin límite — las sugerencias se acumulan indefinidamente
            }
            if (binding.suggestionPanel.visibility != View.VISIBLE) binding.suggestionPanel.visibility = View.VISIBLE
            binding.suggestionsScrollView.post { binding.suggestionsScrollView.fullScroll(View.FOCUS_UP) }
        }

        viewModel.voiceEnabled.observe(this) { enabled ->
            if (enabled) {
                binding.micIndicator.setImageResource(R.drawable.ic_mic_on)
                binding.micIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.mic_active_color)
                if (navController.currentDestination?.id in destinosBiblia && hasMicPermission()) iniciarMicrofono()
            } else {
                binding.micIndicator.setImageResource(R.drawable.ic_mic_off)
                binding.micIndicator.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
                detenerMicrofono()
                limpiarTodasLasSugerencias()
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show(); viewModel.clearError() }
        }
    }

    private fun setupControls() {
        binding.micIndicator.setImageResource(R.drawable.ic_mic_off)
        binding.micIndicator.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
        binding.micIndicator.setOnClickListener {
            val ahora = viewModel.toggleVoice()
            if (ahora && !hasMicPermission()) checkAndRequestMicPermission()
        }
        binding.btnClearAllSuggestions.setOnClickListener {
            limpiarTodasLasSugerencias()
            if (navController.currentDestination?.id in destinosBiblia && hasMicPermission()) iniciarMicrofono()
        }
    }

    private fun agregarNuevoCard(sugerencia: Sugerencia) {
        val cardBinding = ItemSuggestionCardBinding.inflate(LayoutInflater.from(this), binding.suggestionsContainer, false)
        cardBinding.tvSuggestionReference.text = sugerencia.referencia
        cardBinding.tvSuggestionText.text = sugerencia.texto.take(120).let {
            if (sugerencia.texto.length > 120) "\"$it...\"" else "\"$it\""
        }
        cardBinding.btnGoToVerse.setOnClickListener { limpiarTodasLasSugerencias(); navigateToVerse(sugerencia) }
        cardBinding.btnCloseCard.setOnClickListener {
            binding.suggestionsContainer.removeView(cardBinding.root)
            sugerenciasAcumuladas.remove(sugerencia)
            if (sugerenciasAcumuladas.isEmpty()) binding.suggestionPanel.visibility = View.GONE
        }
        cardBinding.root.tag = sugerencia.referencia
        binding.suggestionsContainer.addView(cardBinding.root, 0)
    }

    private fun actualizarCardExistente(sugerencia: Sugerencia) {
        val cardView = binding.suggestionsContainer.findViewWithTag<View>(sugerencia.referencia) ?: return
        val cardBinding = ItemSuggestionCardBinding.bind(cardView)
        cardBinding.tvSuggestionText.text = sugerencia.texto.take(120).let {
            if (sugerencia.texto.length > 120) "\"$it...\"" else "\"$it\""
        }
    }

    private fun limpiarTodasLasSugerencias() {
        sugerenciasAcumuladas.clear()
        binding.suggestionsContainer.removeAllViews()
        binding.suggestionPanel.visibility = View.GONE
        viewModel.descartarSugerencia()
    }

    private fun navigateToVerse(sugerencia: Sugerencia) {
        viewModel.cargarLibroParaNavegacion(sugerencia.libroId) { libro ->
            val libroNombre = libro?.nombre ?: sugerencia.referencia.substringBefore(" ${sugerencia.capitulo}")
            val argsC = Bundle().apply {
                putInt(Constants.ARG_LIBRO_ID, sugerencia.libroId)
                putString(Constants.ARG_LIBRO_NOMBRE, libroNombre)
            }
            navController.navigate(R.id.capitulosFragment, argsC)
            binding.root.post {
                val argsV = Bundle().apply {
                    putInt(Constants.ARG_LIBRO_ID, sugerencia.libroId)
                    putInt(Constants.ARG_CAPITULO, sugerencia.capitulo)
                    putInt(Constants.ARG_VERSICULO_HIGHLIGHT, sugerencia.versiculo)
                }
                navController.navigate(R.id.versiculosFragment, argsV)
            }
        }
    }

    private fun checkAndRequestMicPermission() {
        when {
            hasMicPermission() -> iniciarMicrofono()
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                Toast.makeText(this, "El micrófono permite buscar versículos por voz", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun iniciarMicrofono() {
        if (voiceManager == null) {
            voiceManager = VoiceRecognitionManager(
                context = this,
                onResult = { texto -> viewModel.procesarVoz(texto) },
                onStateChanged = { activo -> viewModel.setMicrofono(activo) },
                onNotAvailable = {
                    Toast.makeText(
                        this,
                        "Reconocimiento de voz no disponible en este dispositivo",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
        voiceManager?.startListening()
    }

    private fun detenerMicrofono() {
        voiceManager?.stopListening()
        viewModel.setMicrofono(false)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (modoSeleccionActivo) {
            salirModoSeleccion()
            return true
        }
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        searchJob?.cancel()
        voiceManager?.destroy()
        ttsManager?.destroy()
        super.onDestroy()
    }
}