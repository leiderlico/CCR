package com.leiderl.CCR.ui.fragments

import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.leiderl.CCR.R
import com.leiderl.CCR.databinding.FragmentCapitulosBinding
import com.leiderl.CCR.ui.adapters.CapitulosAdapter
import com.leiderl.CCR.ui.viewmodels.BibliaViewModel
import com.leiderl.CCR.data.repository.utils.Constants

class CapitulosFragment : Fragment() {

    private var _binding: FragmentCapitulosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BibliaViewModel by activityViewModels()
    private lateinit var adapter: CapitulosAdapter
    private var libroId: Int = -1
    private var libroNombre: String = ""
    private var libroAbreviacion: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCapitulosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        libroId = arguments?.getInt(Constants.ARG_LIBRO_ID) ?: -1
        libroNombre = arguments?.getString(Constants.ARG_LIBRO_NOMBRE) ?: ""
        libroAbreviacion = arguments?.getString(Constants.ARG_LIBRO_ABREVIACION)
            ?.takeIf { it.isNotBlank() } ?: libroNombre
        val capitulosTotal = arguments?.getInt(Constants.ARG_CAPITULOS_TOTAL, 0) ?: 0

        setupRecyclerView()
        setupSwipeGesture()

        if (capitulosTotal > 0) {
            adapter.submitList((1..capitulosTotal).toList())
        } else {
            observeViewModel()
        }
    }

    private fun setupRecyclerView() {
        adapter = CapitulosAdapter { capitulo ->
            val args = Bundle().apply {
                putInt(Constants.ARG_LIBRO_ID, libroId)
                putString(Constants.ARG_LIBRO_NOMBRE, libroNombre)
                putString(Constants.ARG_LIBRO_ABREVIACION, libroAbreviacion)
                putInt(Constants.ARG_CAPITULO, capitulo)
            }
            findNavController().navigate(R.id.action_capitulos_to_versiculos, args)
        }
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 5)
            adapter = this@CapitulosFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.capitulos.observe(viewLifecycleOwner) { caps ->
            if (!caps.isNullOrEmpty()) {
                adapter.submitList(caps)
            }
        }

        viewModel.libroSeleccionado.observe(viewLifecycleOwner) { libro ->
            libro?.let {
                val caps = (1..it.capitulos).toList()
                adapter.submitList(caps)
            }
        }
    }

    private fun navegarLibro(diffX: Float): Boolean {
        val libros = viewModel.libros.value ?: return false
        val idx = libros.indexOfFirst { it.id == libroId }
        if (idx < 0) return false
        val libroDestino = if (diffX < 0 && idx < libros.size - 1) libros[idx + 1]
                           else if (diffX > 0 && idx > 0) libros[idx - 1]
                           else return false
        libroId = libroDestino.id
        libroNombre = libroDestino.nombre
        libroAbreviacion = libroDestino.abreviacion
        requireActivity().title = libroNombre
        adapter.submitList((1..libroDestino.capitulos).toList())
        binding.recyclerView.scrollToPosition(0)
        arguments?.putInt(Constants.ARG_LIBRO_ID, libroId)
        arguments?.putString(Constants.ARG_LIBRO_NOMBRE, libroNombre)
        arguments?.putString(Constants.ARG_LIBRO_ABREVIACION, libroAbreviacion)
        arguments?.putInt(Constants.ARG_CAPITULOS_TOTAL, libroDestino.capitulos)
        return true
    }

    private fun setupSwipeGesture() {
        val SWIPE_MIN_DISTANCE  = 80f
        val SWIPE_THRESHOLD_VEL = 150f

        val detector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vX: Float, vY: Float): Boolean {
                val e1 = e1 ?: return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (kotlin.math.abs(diffX) < SWIPE_MIN_DISTANCE) return false
                if (kotlin.math.abs(vX) < SWIPE_THRESHOLD_VEL) return false
                return navegarLibro(diffX)
            }
        })

        binding.recyclerView.addOnItemTouchListener(object : androidx.recyclerview.widget.RecyclerView.OnItemTouchListener {
            private var startX = 0f
            private var startY = 0f
            private var decidido = false
            private var esHorizontal = false

            override fun onInterceptTouchEvent(rv: androidx.recyclerview.widget.RecyclerView, e: MotionEvent): Boolean {
                detector.onTouchEvent(e)
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = e.x; startY = e.y
                        decidido = false; esHorizontal = false
                        rv.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!decidido) {
                            val dx = kotlin.math.abs(e.x - startX)
                            val dy = kotlin.math.abs(e.y - startY)
                            if (dx > 20f || dy > 20f) {
                                decidido = true
                                esHorizontal = dx > dy * 2.5f
                            }
                        }
                        if (esHorizontal) {
                            rv.parent?.requestDisallowInterceptTouchEvent(true)
                            rv.stopScroll()
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        decidido = false; esHorizontal = false
                        rv.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                return false
            }

            override fun onTouchEvent(rv: androidx.recyclerview.widget.RecyclerView, e: MotionEvent) {
                detector.onTouchEvent(e)
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = libroNombre
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}