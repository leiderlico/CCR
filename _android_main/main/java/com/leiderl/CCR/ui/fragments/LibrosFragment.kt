package com.leiderl.CCR.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.leiderl.CCR.R
import com.leiderl.CCR.data.database.entities.Libro
import com.leiderl.CCR.databinding.FragmentLibrosBinding
import com.leiderl.CCR.ui.adapters.LibrosAdapter
import com.leiderl.CCR.ui.viewmodels.BibliaViewModel
import com.leiderl.CCR.data.repository.utils.Constants

class LibrosFragment : Fragment() {

    private var _binding: FragmentLibrosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BibliaViewModel by activityViewModels()
    private lateinit var adapterAntiguo: LibrosAdapter
    private lateinit var adapterNuevo: LibrosAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibrosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        adapterAntiguo = LibrosAdapter { libro -> onLibroSelected(libro) }
        adapterNuevo   = LibrosAdapter { libro -> onLibroSelected(libro) }

        binding.recyclerAntiguo.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adapterAntiguo
        }
        binding.recyclerNuevo.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adapterNuevo
        }
    }

    private fun observeViewModel() {
        viewModel.libros.observe(viewLifecycleOwner) { libros ->
            if (libros.isNullOrEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
            } else {
                binding.tvEmpty.visibility = View.GONE
                adapterAntiguo.submitList(libros.filter { it.testamento == "Antiguo" })
                adapterNuevo.submitList(libros.filter { it.testamento == "Nuevo" })
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun onLibroSelected(libro: Libro) {
        val args = Bundle().apply {
            putInt(Constants.ARG_LIBRO_ID, libro.id)
            putString(Constants.ARG_LIBRO_NOMBRE, libro.nombre)
            putString(Constants.ARG_LIBRO_ABREVIACION, libro.abreviacion)
            putInt(Constants.ARG_CAPITULOS_TOTAL, libro.capitulos)
        }
        findNavController().navigate(R.id.action_libros_to_capitulos, args)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "CCR"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}