package com.leiderl.CCR.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.leiderl.CCR.R

class CapitulosAdapter(
    private val onCapituloClick: (Int) -> Unit
) : ListAdapter<Int, CapitulosAdapter.ViewHolder>(CapituloDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_capitulo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onCapituloClick)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNumero: TextView = view.findViewById(R.id.tvCapituloNumero)
        private val card: MaterialCardView = view.findViewById(R.id.cardCapitulo)

        fun bind(cap: Int, onClick: (Int) -> Unit) {
            tvNumero.text = cap.toString()
            card.setOnClickListener { onClick(cap) }
        }
    }

    class CapituloDiff : DiffUtil.ItemCallback<Int>() {
        override fun areItemsTheSame(a: Int, b: Int) = a == b
        override fun areContentsTheSame(a: Int, b: Int) = a == b
    }
}