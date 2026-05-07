package com.leiderl.CCR.ui.fragments

import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.leiderl.CCR.R
import com.leiderl.CCR.databinding.FragmentDevocionalBinding
import com.leiderl.CCR.ui.MainActivity
import com.leiderl.CCR.ui.viewmodels.DevocionalViewModel

class DevocionalFragment : Fragment() {

    private var _binding: FragmentDevocionalBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DevocionalViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDevocionalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ajustarPaddingBottomNav(view)
        aplicarFuentes()

        // Pergamino según modo día/noche
        val esNoche = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        binding.imgPergamino.setImageResource(
            if (esNoche) R.drawable.pergaminodevocionalnight
            else R.drawable.pergaminodevocional
        )

        viewModel.devocional.observe(viewLifecycleOwner) { dev ->
            if (dev == null) return@observe
            binding.tvFecha.text               = dev.fecha.uppercase()
            binding.tvTitulo.text              = dev.titulo
            binding.tvVersiculoTexto.text      = dev.versiculo_texto
            binding.tvVersiculoReferencia.text = "— ${dev.versiculo_referencia.uppercase()}"
            binding.tvCuerpo.text              = formatearCuerpo(dev.cuerpo)
        }
    }

    // ── Aplica Lora por código como respaldo a la declaración en XML ──────
    // Esto garantiza que funcione en todos los niveles de API.
    private fun aplicarFuentes() {
        val lora: Typeface = ResourcesCompat.getFont(requireContext(), R.font.lora)
            ?: Typeface.SERIF
        val loraItalic: Typeface = ResourcesCompat.getFont(requireContext(), R.font.lora_italic)
            ?: Typeface.create(Typeface.SERIF, Typeface.ITALIC)

        binding.tvFecha.typeface              = lora
        binding.tvTitulo.typeface             = lora
        binding.tvVersiculoTexto.typeface     = loraItalic
        binding.tvVersiculoReferencia.typeface = lora
        binding.tvCuerpo.typeface             = lora
    }

    private fun ajustarPaddingBottomNav(view: View) {
        val bottomNav = (activity as? MainActivity)
            ?.findViewById<View>(R.id.bottomNav) ?: return
        bottomNav.post {
            val alto = bottomNav.height
            if (alto == 0) return@post
            val scroll = view.findViewById<ScrollView>(R.id.scrollDevocional) ?: return@post
            scroll.setPadding(scroll.paddingLeft, scroll.paddingTop, scroll.paddingRight, alto)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.verificarYRecargarSiCambioDia()

        val esNoche = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        binding.imgPergamino.setImageResource(
            if (esNoche) R.drawable.pergaminodevocionalnight
            else R.drawable.pergaminodevocional
        )
    }

    private fun formatearCuerpo(texto: String): String {
        return texto.split("\n\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}