package com.leiderl.CCR.ui.fragments

import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.leiderl.CCR.R
import com.leiderl.CCR.databinding.FragmentGruposSelectorBinding

class GruposSelectorFragment : Fragment() {

    private var _binding: FragmentGruposSelectorBinding? = null
    private val binding get() = _binding!!

    data class GrupoConfig(
        val nombre: String,
        val iconoRes: Int,
        val nightRingColor: Int
    )

    // Modo noche: todas las tarjetas usan el mismo fondo #0E2231 (igual que el toolbar),
    // cada una mantiene su propio color de anillos como decoración sutil.
    // Modo día: el fondo viene del XML (@color/grupo_card_background = cyan).
    private val nightBgColor = 0xFF0E2231.toInt()

    private val gruposConfig = listOf(
        GrupoConfig(
            nombre         = "ESCUELA DOMINICAL",
            iconoRes       = R.drawable.ic_grupo_escuela_dominical,
            nightRingColor = 0xFFE8520F.toInt()
        ),
        GrupoConfig(
            nombre         = "SERVICIOS DE SANIDAD Y MILAGRO",
            iconoRes       = R.drawable.ic_grupo_sanidad_milagro,
            nightRingColor = 0xFFB8860B.toInt()
        ),
        GrupoConfig(
            nombre         = "AYUNO Y ORACIÓN",
            iconoRes       = R.drawable.ic_grupo_ayuno_oracion,
            nightRingColor = 0xFF5B45C8.toInt()
        ),
        GrupoConfig(
            nombre         = "PREDICAS ESPECIALES",
            iconoRes       = R.drawable.ic_grupo_predicas_especiales,
            nightRingColor = 0xFF1E8044.toInt()
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGruposSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        construirTarjetas()
        aplicarPaddingBottomNav()
    }

    // ── Padding inferior ──────────────────────────────────────────────────
    // El BottomNavigationView flota sobre el contenido (layout_gravity="bottom"),
    // por eso el ScrollView necesita un paddingBottom igual a la altura real del
    // BottomNav para que la última tarjeta nunca quede oculta.
    // Usamos post{} para leer la altura después del primer layout pass.
    private fun aplicarPaddingBottomNav() {
        binding.scrollGrupos.post {
            if (_binding == null) return@post
            val bottomNav = requireActivity().findViewById<View>(R.id.bottomNav)
            val bottomNavHeight = bottomNav?.height ?: 0
            val extraPx = (16 * resources.displayMetrics.density).toInt()
            binding.scrollGrupos.setPadding(
                binding.scrollGrupos.paddingLeft,
                binding.scrollGrupos.paddingTop,
                binding.scrollGrupos.paddingRight,
                bottomNavHeight + extraPx
            )
        }
    }

    // ── Drawable de anillos de vinilo ─────────────────────────────────────
    // Círculos concéntricos desde la esquina superior derecha del card.
    // El MaterialCardView los recorta con sus cornerRadius, dando el efecto
    // de arcos parciales característico del diseño vinilo.
    private fun crearDrawableAnillos(ringColor: Int): Drawable {
        val dp = resources.displayMetrics.density
        return object : Drawable() {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style       = Paint.Style.STROKE
                strokeWidth = dp * 1.5f
            }
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

            override fun draw(canvas: Canvas) {
                val cx = bounds.width().toFloat()
                val cy = 0f

                strokePaint.color = ringColor
                strokePaint.alpha = 85;  canvas.drawCircle(cx, cy, dp * 78f, strokePaint)
                strokePaint.alpha = 105; canvas.drawCircle(cx, cy, dp * 58f, strokePaint)
                strokePaint.strokeWidth = dp * 2f
                strokePaint.alpha = 125; canvas.drawCircle(cx, cy, dp * 40f, strokePaint)
                strokePaint.alpha = 145; canvas.drawCircle(cx, cy, dp * 24f, strokePaint)

                dotPaint.color = ringColor; dotPaint.alpha = 165
                canvas.drawCircle(cx, cy, dp * 9f, dotPaint)
            }

            override fun setAlpha(alpha: Int) { strokePaint.alpha = alpha; invalidateSelf() }
            override fun setColorFilter(cf: ColorFilter?) {
                strokePaint.colorFilter = cf; invalidateSelf()
            }
            override fun getOpacity() = PixelFormat.TRANSLUCENT
        }
    }

    // ── Construcción de tarjetas ──────────────────────────────────────────
    private fun construirTarjetas() {
        val inflater  = LayoutInflater.from(requireContext())
        val container = binding.containerGrupos
        val esNoche   = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES

        gruposConfig.forEach { config ->
            val cardView = inflater.inflate(R.layout.item_grupo_selector_card, container, false)

            // Modo noche: mismo fondo oscuro para todas las tarjetas
            // Modo día:   el XML aplica @color/grupo_card_background (cyan), no se toca
            if (esNoche) {
                (cardView as MaterialCardView).setCardBackgroundColor(nightBgColor)
            }

            cardView.findViewById<ImageView>(R.id.ivGrupoIcono)
                .setImageResource(config.iconoRes)

            cardView.findViewById<TextView>(R.id.tvGrupoNombreCard)
                .text = config.nombre

            // Anillos: coloridos en noche, blanco translúcido en día
            val ringColor = if (esNoche) config.nightRingColor else 0x40FFFFFF
            cardView.findViewById<View>(R.id.viewVinylRings)
                .background = crearDrawableAnillos(ringColor)

            cardView.setOnClickListener {
                val args = Bundle().apply { putString("grupoNombre", config.nombre) }
                findNavController().navigate(R.id.action_gruposSelector_to_videos, args)
            }

            container.addView(cardView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}