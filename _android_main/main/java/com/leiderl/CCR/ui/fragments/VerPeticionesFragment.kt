package com.leiderl.CCR.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.leiderl.CCR.R
import com.leiderl.CCR.data.repository.PeticionRepository
import com.leiderl.CCR.ui.MainActivity
import com.leiderl.CCR.ui.PeticionesAdapter
import kotlinx.coroutines.launch

class VerPeticionesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_ver_peticiones, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = "Peticiones recibidas"

        val layoutCargando = view.findViewById<View>(R.id.layoutCargando)
        val layoutVacio    = view.findViewById<View>(R.id.layoutVacio)
        val tvMensajeVacio = view.findViewById<android.widget.TextView>(R.id.tvMensajeVacio)
        val rv             = view.findViewById<RecyclerView>(R.id.rvPeticiones)

        rv.layoutManager = LinearLayoutManager(requireContext())

        // Padding dinámico — lista nunca tapada por el bottomnav en ningún teléfono
        val bottomNav = (activity as? MainActivity)?.findViewById<View>(R.id.bottomNav)
        bottomNav?.post {
            val alto = bottomNav.height
            if (alto > 0) rv.setPadding(rv.paddingLeft, rv.paddingTop, rv.paddingRight, alto)
        }

        lifecycleScope.launch {
            val peticiones = PeticionRepository().obtenerPeticiones()
            layoutCargando.visibility = View.GONE
            if (peticiones.isEmpty()) {
                tvMensajeVacio.text = "No hay peticiones aún"
                layoutVacio.visibility = View.VISIBLE
            } else {
                rv.visibility = View.VISIBLE
                rv.adapter = PeticionesAdapter(peticiones)
            }
        }
    }
}