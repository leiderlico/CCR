package com.leiderl.CCR.ui.fragments

import android.animation.ObjectAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.Player
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.leiderl.CCR.R
import com.leiderl.CCR.services.RadioService

data class EstacionRadio(
    val nombre: String,
    val streamUrl: String,
    val frecuencia: String = "",
    val imagenRes: Int = R.drawable.ic_radio
)

class RadioFragment : Fragment() {

    private val estaciones = listOf(
        EstacionRadio("Radio Melodía Celestial",  "https://stream-178.zeno.fm/1g5aal5p0hfvv",      "105.1 FM", R.drawable.uno),
        EstacionRadio("Minuto de Dios CTG",       "https://sp4.colombiatelecom.com.co/8042/stream", "89.5 FM",  R.drawable.dos),
        EstacionRadio("Verdad y Vida Radio",      "https://stream-204.zeno.fm/vvon5xkmvhwvv",       "100.1 AM", R.drawable.tres),
        EstacionRadio("Radio Estrella",           "https://stream-204.zeno.fm/fhgeiizn0xguv",       "89.3 FM",  R.drawable.cuatro),
        EstacionRadio("Radio Cristiana Colombia", "https://stream-204.zeno.fm/a3y646rxcfhvv",       "",         R.drawable.cinco),
        EstacionRadio("Cielo Cartagena",          "https://stream-204.zeno.fm/v2pspfgm54zuv",       "103.0 FM", R.drawable.seis),
        EstacionRadio("Alfa y Omega",             "https://stream-204.zeno.fm/ebsnme8phdovv",       "",         R.drawable.siete)
    )

    private var radioService    : RadioService? = null
    private var servicioConectado = false
    private var estacionActual    = 0
    private val tarjetas          = mutableListOf<MaterialCardView>()
    private var reproduciendo     = false

    // ── Vistas principales ────────────────────────────────────────────────
    private lateinit var tvNombre       : TextView
    private lateinit var tvFrecuencia   : TextView
    private lateinit var tvEstado       : TextView
    private lateinit var tvBadgeEnVivo  : TextView
    private lateinit var btnPlayPause   : FloatingActionButton
    private lateinit var cardAnuncio    : MaterialCardView
    private lateinit var imgEstacion    : ImageView
    private lateinit var layoutEcualizador : LinearLayout

    // Barras del ecualizador principal (player)
    private lateinit var eqBars: List<View>

    // ── Ecualizador ───────────────────────────────────────────────────────
    private val eqHandler = Handler(Looper.getMainLooper())
    private val eqRunnable = object : Runnable {
        override fun run() {
            if (!reproduciendo || _rootView == null) return
            animarBarras(eqBars, 4, 18)
            // También anima las barras del item activo en la lista
            tarjetas.getOrNull(estacionActual)?.let { card ->
                val bars = listOf(
                    card.findViewById<View>(R.id.eqItemBar1),
                    card.findViewById<View>(R.id.eqItemBar2),
                    card.findViewById<View>(R.id.eqItemBar3),
                    card.findViewById<View>(R.id.eqItemBar4),
                    card.findViewById<View>(R.id.eqItemBar5)
                )
                animarBarras(bars, 3, 14)
            }
            eqHandler.postDelayed(this, 280)
        }
    }

    private fun animarBarras(bars: List<View?>, minDp: Int, maxDp: Int) {
        val density = resources.displayMetrics.density
        bars.forEach { bar ->
            bar ?: return@forEach
            val targetPx = ((minDp + (Math.random() * (maxDp - minDp))).toInt() * density).toInt()
            ObjectAnimator.ofInt(bar, "height", bar.height, targetPx).apply {
                duration = 220
                start()
            }
        }
    }

    private fun iniciarEcualizador() {
        layoutEcualizador.visibility = View.VISIBLE
        eqHandler.removeCallbacks(eqRunnable)
        eqHandler.post(eqRunnable)
    }

    private fun detenerEcualizador() {
        eqHandler.removeCallbacks(eqRunnable)
        layoutEcualizador.visibility = View.GONE
        // Ocultar también el eq del item
        tarjetas.forEach { card ->
            card.findViewById<View>(R.id.layoutEqItem)?.visibility = View.GONE
        }
    }

    // ── Root view ref para evitar post después de destroy ─────────────────
    private var _rootView: View? = null

    // ── ServiceConnection ─────────────────────────────────────────────────
    private val serviceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            radioService      = (binder as RadioService.RadioBinder).getService()
            servicioConectado = true

            radioService?.onEstacionCambiada = { index ->
                requireActivity().runOnUiThread {
                    estacionActual = index
                    mostrarInfoEstacion(index)
                }
            }

            radioService?.onDetener = {
                requireActivity().runOnUiThread {
                    setEstadoReproduciendo(false)
                    tvEstado.text = "Detenido"
                    tvEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.radio_subtitle))
                    desconectarServicio()
                }
            }

            radioService?.onEstadoCambiado = { state ->
                requireActivity().runOnUiThread {
                    val activo = state == Player.STATE_READY || state == Player.STATE_BUFFERING
                    setEstadoReproduciendo(activo)
                    actualizarTextoEstado(state)
                }
            }

            radioService?.onError = {
                requireActivity().runOnUiThread {
                    tvEstado.text = "Error — reintentando..."
                    tvEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.radio_live_text))
                }
            }

            val estaActivo = radioService?.estaReproduciendo == true
            setEstadoReproduciendo(estaActivo)
            actualizarTextoEstado(radioService?.estadoActual ?: Player.STATE_IDLE)

            val indexServicio = radioService?.indiceActualPublic ?: 0
            if (indexServicio != estacionActual) {
                estacionActual = indexServicio
                mostrarInfoEstacion(indexServicio)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            radioService      = null
            servicioConectado = false
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_radio, container, false)
        _rootView = v
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().title = "Radio"

        tvNombre          = view.findViewById(R.id.tvNombreEstacion)
        tvFrecuencia      = view.findViewById(R.id.tvFrecuencia)
        tvEstado          = view.findViewById(R.id.tvEstado)
        tvBadgeEnVivo     = view.findViewById(R.id.tvBadgeEnVivo)
        btnPlayPause      = view.findViewById(R.id.btnPlayPause)
        cardAnuncio       = view.findViewById(R.id.cardAnuncio)
        imgEstacion       = view.findViewById(R.id.imgEstacion)
        layoutEcualizador = view.findViewById(R.id.layoutEcualizador)

        eqBars = listOf(
            view.findViewById(R.id.eqBar1),
            view.findViewById(R.id.eqBar2),
            view.findViewById(R.id.eqBar3),
            view.findViewById(R.id.eqBar4),
            view.findViewById(R.id.eqBar5)
        )

        // Botón Play/Pause
        btnPlayPause.setOnClickListener {
            if (reproduciendo) {
                desconectarServicio()
                RadioService.detener(requireContext())
                setEstadoReproduciendo(false)
                tvEstado.text = "Detenido"
                tvEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.radio_subtitle))
            } else {
                tvEstado.text = "Conectando..."
                tvEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.radio_subtitle))
                btnPlayPause.setImageResource(R.drawable.ic_pause)
                iniciarServicio(estaciones[estacionActual])
            }
        }

        // Construir tarjetas de estaciones
        val contenedor = view.findViewById<ViewGroup>(R.id.contenedorEstaciones)
        estaciones.forEachIndexed { index, estacion ->
            val card = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_estacion_radio, contenedor, false) as MaterialCardView

            // Imagen thumbnail — misma imagen del banner, centerCrop en 40dp
            card.findViewById<ImageView>(R.id.imgEstacionItem).apply {
                setImageResource(estacion.imagenRes)
                // Esquinas redondeadas en el thumbnail
                clipToOutline = true
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        val r = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics)
                        outline.setRoundRect(0, 0, view.width, view.height, r)
                    }
                }
            }

            card.findViewById<TextView>(R.id.tvNombreEstacionItem).text = estacion.nombre

            val tvFreq = card.findViewById<TextView>(R.id.tvFrecuenciaItem)
            if (estacion.frecuencia.isNotEmpty()) {
                tvFreq.text = estacion.frecuencia
                tvFreq.visibility = View.VISIBLE
            } else {
                tvFreq.visibility = View.GONE
            }

            card.setOnClickListener { seleccionarEstacion(index) }
            contenedor.addView(card)
            tarjetas.add(card)
        }

        mostrarInfoEstacion(0)

        requireContext().bindService(
            Intent(requireContext(), RadioService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onDestroyView() {
        detenerEcualizador()
        _rootView = null
        radioService?.onEstacionCambiada = null
        radioService?.onDetener          = null
        radioService?.onEstadoCambiado   = null
        radioService?.onError            = null
        if (servicioConectado) {
            requireContext().unbindService(serviceConnection)
            servicioConectado = false
        }
        super.onDestroyView()
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun setEstadoReproduciendo(activo: Boolean) {
        reproduciendo = activo
        btnPlayPause.setImageResource(
            if (activo) R.drawable.ic_pause else R.drawable.ic_play_arrow
        )
        // Badge EN VIVO y ecualizador
        if (activo) {
            tvBadgeEnVivo.visibility = View.VISIBLE
            iniciarEcualizador()
            // Mostrar eq en el item activo
            tarjetas.getOrNull(estacionActual)
                ?.findViewById<View>(R.id.layoutEqItem)
                ?.visibility = View.VISIBLE
        } else {
            tvBadgeEnVivo.visibility = View.GONE
            detenerEcualizador()
        }
        requireActivity().invalidateOptionsMenu()
    }

    private fun actualizarTextoEstado(state: Int) {
        val texto = when (state) {
            Player.STATE_BUFFERING -> "Conectando..."
            Player.STATE_READY     -> estaciones.getOrNull(estacionActual)?.nombre ?: "En vivo"
            Player.STATE_ENDED,
            Player.STATE_IDLE      -> "Detenido"
            else                   -> ""
        }
        val colorRes = when (state) {
            Player.STATE_READY -> R.color.radio_title
            else               -> R.color.radio_subtitle
        }
        tvEstado.text = texto
        tvEstado.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun desconectarServicio() {
        if (servicioConectado) {
            radioService?.onEstacionCambiada = null
            radioService?.onDetener          = null
            radioService?.onEstadoCambiado   = null
            radioService?.onError            = null
            requireContext().unbindService(serviceConnection)
            servicioConectado = false
        }
        radioService = null
    }

    private fun seleccionarEstacion(index: Int) {
        estacionActual = index
        mostrarInfoEstacion(index)
        iniciarServicio(estaciones[index])
    }

    private fun mostrarInfoEstacion(index: Int) {
        estacionActual = index
        val estacion = estaciones[index]
        imgEstacion.setImageResource(estacion.imagenRes)
        tvNombre.text     = estacion.nombre
        tvFrecuencia.text = if (estacion.frecuencia.isNotEmpty()) estacion.frecuencia else "En línea"
        cardAnuncio.visibility = if (index == 0) View.VISIBLE else View.GONE

        val density = resources.displayMetrics.density

        tarjetas.forEachIndexed { i, card ->
            val activa = i == index
            card.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(),
                    if (activa) R.color.primary else R.color.radio_card_inactive)
            )
            card.cardElevation = if (activa) 4f * density else 0f

            card.findViewById<TextView>(R.id.tvNombreEstacionItem).setTextColor(
                ContextCompat.getColor(requireContext(),
                    if (activa) R.color.white else R.color.radio_title)
            )
            val tvFreqItem = card.findViewById<TextView>(R.id.tvFrecuenciaItem)
            if (tvFreqItem.visibility == View.VISIBLE) {
                tvFreqItem.setTextColor(
                    if (activa) 0xB3FFFFFF.toInt()
                    else ContextCompat.getColor(requireContext(), R.color.radio_subtitle)
                )
            }

            // Ecualizador del item: visible solo si está activa Y reproduciendo
            val eqItem = card.findViewById<View>(R.id.layoutEqItem)
            eqItem?.visibility = if (activa && reproduciendo) View.VISIBLE else View.GONE
        }
    }

    private fun iniciarServicio(estacion: EstacionRadio) {
        RadioService.iniciar(
            context     = requireContext(),
            url         = estacion.streamUrl,
            nombre      = estacion.nombre,
            frecuencia  = estacion.frecuencia,
            index       = estacionActual,
            urls        = ArrayList(estaciones.map { it.streamUrl }),
            nombres     = ArrayList(estaciones.map { it.nombre }),
            frecuencias = ArrayList(estaciones.map { it.frecuencia })
        )
        if (!servicioConectado) {
            requireContext().bindService(
                Intent(requireContext(), RadioService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }
}