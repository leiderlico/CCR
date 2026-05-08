package com.leiderl.CCR.ui.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.leiderl.CCR.R
import com.leiderl.CCR.data.database.entities.PreguntaTrivia
import com.leiderl.CCR.databinding.FragmentJuegoBinding
import com.leiderl.CCR.ui.MainActivity
import com.leiderl.CCR.ui.viewmodels.EstadoJuego
import com.leiderl.CCR.ui.viewmodels.JuegoViewModel
import com.leiderl.CCR.ui.viewmodels.RankingData
import com.leiderl.CCR.ui.viewmodels.RankingJugador

class JuegoFragment : Fragment() {

    private var _binding: FragmentJuegoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: JuegoViewModel by viewModels()

    private var mpPin: MediaPlayer? = null
    private var mpTicTac: MediaPlayer? = null
    private val ticHandler = Handler(Looper.getMainLooper())
    private var ticRunnable: Runnable? = null
    private var ticActivo = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJuegoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Ajustar el padding inferior de todos los ScrollViews al alto
        //    real del BottomNavigationView — funciona en cualquier teléfono ──
        ajustarPaddingBottomNav()

        setupObservers()
        setupClicks()
        inicializarSonidos()
    }

    /**
     * Lee el alto real del BottomNav desde la Activity y aplica ese valor
     * como paddingBottom en todos los ScrollViews del fragment.
     * Esto reemplaza el paddingBottom="80dp" hardcodeado que fallaba en
     * pantallas más cuadradas o con navegación por gestos.
     */
    private fun ajustarPaddingBottomNav() {
        val bottomNav = (activity as? MainActivity)
            ?.findViewById<View>(R.id.bottomNav)
            ?: return

        // Esperar a que el BottomNav esté medido para leer su alto real
        bottomNav.post {
            val altoNav = bottomNav.height
            if (altoNav == 0) return@post

            val scrollViews = listOf(
                binding.panelRegistro,
                binding.panelDificultad,
                binding.panelJuego,
                binding.panelResultado
            )
            scrollViews.forEach { scroll ->
                if (scroll is ScrollView) {
                    scroll.setPadding(
                        scroll.paddingLeft,
                        scroll.paddingTop,
                        scroll.paddingRight,
                        altoNav
                    )
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is EstadoJuego.Verificando -> mostrarPanel(binding.panelVerificando)
                is EstadoJuego.Inicio      -> mostrarPanel(binding.panelRegistro)
                is EstadoJuego.SeleccionDificultad -> {
                    binding.tvBienvenida.text = "¡Hola, ${viewModel.nombreJugador.value?.split(" ")?.firstOrNull() ?: ""}!"
                    ocultarSpinnersDificultad()
                    mostrarPanel(binding.panelDificultad)
                }
                is EstadoJuego.Jugando -> { mostrarPanel(binding.panelJuego); iniciarTicTac() }
                is EstadoJuego.Resultado -> { detenerTicTac()
                    binding.tvPuntajeFinal.text = "${estado.puntaje}"
                    binding.tvNuevoTotal.text = when (estado.dificultad) {
                        "facil"   -> "Modo FÁCIL"
                        "medio"   -> "Modo MEDIO"
                        "dificil" -> "Modo DIFÍCIL"
                        else      -> ""
                    }
                    mostrarPanel(binding.panelResultado)
                }
            }
        }

        viewModel.cargando.observe(viewLifecycleOwner) { cargando ->
            binding.spinnerRegistrar.visibility = if (cargando) View.VISIBLE else View.GONE
            binding.btnRegistrar.text           = if (cargando) "" else "COMENZAR"
            binding.btnRegistrar.isEnabled      = !cargando
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
                ocultarSpinnersDificultad()
            }
        }

        viewModel.ranking.observe(viewLifecycleOwner) { ranking ->
            if (ranking != null) llenarRankingMini(ranking)
        }

        viewModel.tiempoMs.observe(viewLifecycleOwner) { ms ->
            val segundos = ms / 1000.0
            binding.tvCronometro.text  = String.format("%.1f", segundos)
            binding.barTiempo.progress = ms.toInt()
            binding.tvCronometro.setTextColor(when {
                ms > 30000 -> requireContext().getColor(R.color.primary)
                ms > 15000 -> requireContext().getColor(R.color.accent)
                else       -> requireContext().getColor(R.color.red_logo)
            })
        }

        viewModel.preguntaActual.observe(viewLifecycleOwner) { idx ->
            val pregs = viewModel.preguntas.value ?: return@observe
            if (idx < pregs.size) mostrarPregunta(pregs[idx], idx, pregs.size)
        }

        viewModel.preguntas.observe(viewLifecycleOwner) { pregs ->
            val idx = viewModel.preguntaActual.value ?: 0
            if (pregs.isNotEmpty() && idx < pregs.size) mostrarPregunta(pregs[idx], idx, pregs.size)
        }
    }

    private fun llenarRankingMini(ranking: RankingData) {
        fun fill(tv: android.widget.TextView, j: RankingJugador?) {
            if (j == null) { tv.text = "-"; return }
            val nombre = j.nombre.split(" ").firstOrNull() ?: j.nombre
            tv.text = "$nombre\n${j.puntaje}pts"
        }
        fill(binding.rankF1, ranking.top_facil.getOrNull(0))
        fill(binding.rankF2, ranking.top_facil.getOrNull(1))
        fill(binding.rankF3, ranking.top_facil.getOrNull(2))
        fill(binding.rankM1, ranking.top_medio.getOrNull(0))
        fill(binding.rankM2, ranking.top_medio.getOrNull(1))
        fill(binding.rankM3, ranking.top_medio.getOrNull(2))
        fill(binding.rankD1, ranking.top_dificil.getOrNull(0))
        fill(binding.rankD2, ranking.top_dificil.getOrNull(1))
        fill(binding.rankD3, ranking.top_dificil.getOrNull(2))
    }

    private fun mostrarPregunta(pregunta: PreguntaTrivia, idx: Int, total: Int) {
        binding.tvContadorPreguntas.text = "Pregunta ${idx + 1} de $total"
        binding.tvPregunta.text = pregunta.pregunta
        val botones = listOf(binding.btnOpcionA, binding.btnOpcionB, binding.btnOpcionC, binding.btnOpcionD)
        pregunta.opciones.forEachIndexed { i, opcion ->
            botones[i].text      = opcion
            botones[i].isEnabled = true
        }
    }

    private fun setupClicks() {
        binding.btnRegistrar.setOnClickListener {
            viewModel.registrar(
                binding.etNombre.text.toString(),
                binding.etApellido.text.toString()
            )
        }

        binding.btnFacil.setOnClickListener   { mostrarSpinnerDificultad("facil");   viewModel.iniciarJuego("facil") }
        binding.btnMedio.setOnClickListener   { mostrarSpinnerDificultad("medio");   viewModel.iniciarJuego("medio") }
        binding.btnDificil.setOnClickListener { mostrarSpinnerDificultad("dificil"); viewModel.iniciarJuego("dificil") }

        listOf(binding.btnOpcionA, binding.btnOpcionB, binding.btnOpcionC, binding.btnOpcionD)
            .forEach { btn ->
                btn.setOnClickListener { reproducirPin(); viewModel.responder(btn.text.toString()) }
            }

        binding.btnJugarOtraVez.setOnClickListener { viewModel.reiniciar() }

        binding.btnVerRankingResultado.setOnClickListener {
            findNavController().navigate(R.id.action_juegoFragment_to_rankingFragment)
        }

        binding.btnVerRanking.setOnClickListener {
            findNavController().navigate(R.id.action_juegoFragment_to_rankingFragment)
        }
    }

    private fun mostrarPanel(panelVisible: View) {
        listOf(
            binding.panelVerificando,
            binding.panelRegistro,
            binding.panelDificultad,
            binding.panelJuego,
            binding.panelResultado
        ).forEach { it.visibility = View.GONE }
        panelVisible.visibility = View.VISIBLE
    }

    private fun mostrarSpinnerDificultad(dificultad: String) {
        binding.spinnerFacil.visibility   = View.GONE
        binding.spinnerMedio.visibility   = View.GONE
        binding.spinnerDificil.visibility = View.GONE
        binding.btnFacil.isEnabled   = false
        binding.btnMedio.isEnabled   = false
        binding.btnDificil.isEnabled = false
        when (dificultad) {
            "facil"   -> binding.spinnerFacil.visibility   = View.VISIBLE
            "medio"   -> binding.spinnerMedio.visibility   = View.VISIBLE
            "dificil" -> binding.spinnerDificil.visibility = View.VISIBLE
        }
    }

    private fun ocultarSpinnersDificultad() {
        binding.spinnerFacil.visibility   = View.GONE
        binding.spinnerMedio.visibility   = View.GONE
        binding.spinnerDificil.visibility = View.GONE
        binding.btnFacil.isEnabled   = true
        binding.btnMedio.isEnabled   = true
        binding.btnDificil.isEnabled = true
    }

    private fun inicializarSonidos() {
        try {
            mpPin    = MediaPlayer.create(requireContext(), R.raw.pin)
            mpTicTac = MediaPlayer.create(requireContext(), R.raw.tictac)
        } catch (e: Exception) { /* sonido no disponible */ }
    }

    private fun reproducirPin() {
        try {
            mpPin?.let { if (it.isPlaying) it.seekTo(0) else it.start() }
        } catch (e: Exception) {}
    }

    private fun iniciarTicTac() {
        if (ticActivo) return
        ticActivo = true
        ticRunnable = object : Runnable {
            override fun run() {
                try { mpTicTac?.let { mp -> mp.seekTo(0); mp.start() } } catch (e: Exception) {}
                if (ticActivo) ticHandler.postDelayed(this, 1000)
            }
        }
        ticHandler.post(ticRunnable!!)
    }

    private fun detenerTicTac() {
        ticActivo = false
        ticRunnable?.let { ticHandler.removeCallbacks(it) }
        try { mpTicTac?.pause(); mpTicTac?.seekTo(0) } catch (e: Exception) {}
    }

    override fun onDestroyView() {
        detenerTicTac()
        mpPin?.release();    mpPin    = null
        mpTicTac?.release(); mpTicTac = null
        super.onDestroyView()
        _binding = null
    }
}