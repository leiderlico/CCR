package com.leiderl.CCR.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.leiderl.CCR.R
import com.leiderl.CCR.data.repository.PeticionRepository
import kotlinx.coroutines.launch

class PeticionesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_peticion, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = "Peticiones"

        val headerPeticion     = view.findViewById<View>(R.id.headerPeticion)
        val layoutFormulario   = view.findViewById<View>(R.id.layoutFormulario)
        val layoutConfirmacion = view.findViewById<View>(R.id.layoutConfirmacion)
        val etPeticion         = view.findViewById<TextInputEditText>(R.id.etPeticion)
        val btnEnviar          = view.findViewById<MaterialButton>(R.id.btnEnviarPeticion)
        val btnNueva           = view.findViewById<MaterialButton>(R.id.btnNuevaPeticion)
        val btnSalir           = view.findViewById<MaterialButton>(R.id.btnSalirPeticion)
        val btnVerPeticiones   = view.findViewById<ImageButton>(R.id.btnVerPeticiones)

        btnEnviar.setOnClickListener {
            val texto = etPeticion.text?.toString()?.trim() ?: ""
            if (texto.isEmpty()) {
                etPeticion.error = "Por favor escribe tu petición"
                return@setOnClickListener
            }
            headerPeticion.visibility     = View.GONE
            layoutFormulario.visibility   = View.GONE
            layoutConfirmacion.visibility = View.VISIBLE
            lifecycleScope.launch {
                PeticionRepository().enviarPeticion(texto)
            }
        }

        btnNueva.setOnClickListener {
            etPeticion.setText("")
            headerPeticion.visibility     = View.VISIBLE
            layoutConfirmacion.visibility = View.GONE
            layoutFormulario.visibility   = View.VISIBLE
        }

        btnSalir.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        btnVerPeticiones.setOnClickListener {
            mostrarDialogPin {
                findNavController().navigate(R.id.action_peticiones_to_verPeticiones)
            }
        }

        etPeticion.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etPeticion, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun mostrarDialogPin(onExito: () -> Unit) {
        val et = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "PIN"
            gravity = android.view.Gravity.CENTER
            textSize = 20f
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Área restringida")
            .setMessage("Confirma que eres una persona autorizada")
            .setView(et)
            .setPositiveButton("Entrar") { _, _ ->
                if (et.text.toString() == "00000000") onExito()
                else Toast.makeText(requireContext(), "PIN incorrecto", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}