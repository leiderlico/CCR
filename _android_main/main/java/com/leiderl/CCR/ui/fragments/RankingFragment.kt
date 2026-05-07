package com.leiderl.CCR.ui.fragments

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.leiderl.CCR.R
import com.leiderl.CCR.databinding.FragmentRankingBinding
import com.leiderl.CCR.ui.viewmodels.JuegoViewModel
import com.leiderl.CCR.ui.viewmodels.RankingJugador

class RankingFragment : Fragment() {

    private var _binding: FragmentRankingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: JuegoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRankingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = "Ranking"

        binding.rankingCargando.visibility = View.VISIBLE
        binding.rankingContenido.visibility = View.GONE

        viewModel.cargarRanking()

        viewModel.ranking.observe(viewLifecycleOwner) { ranking ->
            if (ranking != null) {
                binding.rankingCargando.visibility = View.GONE
                binding.rankingContenido.visibility = View.VISIBLE
                llenarTabla(binding.tablaFacil,   ranking.top_facil)
                llenarTabla(binding.tablaMedio,   ranking.top_medio)
                llenarTabla(binding.tablaDificil, ranking.top_dificil)
            }
        }
    }

    private fun llenarTabla(contenedor: LinearLayout, lista: List<RankingJugador>) {
        contenedor.removeAllViews()
        val medallas = listOf("🥇", "🥈", "🥉")

        lista.forEachIndexed { idx, jugador ->
            val fila = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(12, 10, 12, 10)
                setBackgroundColor(if (idx % 2 == 0) Color.parseColor("#0A000000") else Color.TRANSPARENT)
            }

            val tvPos = TextView(requireContext()).apply {
                text = medallas.getOrElse(idx) { "${idx + 1}" }
                textSize = if (idx < 3) 16f else 13f
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(40.dpToPx(), LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            val tvNombre = TextView(requireContext()).apply {
                text = jugador.nombre
                textSize = 14f
                setTextColor(requireContext().getColor(R.color.juego_placeholder_text))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvPts = TextView(requireContext()).apply {
                text = "${jugador.puntaje} pts"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(requireContext().getColor(R.color.primary))
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            fila.addView(tvPos)
            fila.addView(tvNombre)
            fila.addView(tvPts)
            contenedor.addView(fila)
        }
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}