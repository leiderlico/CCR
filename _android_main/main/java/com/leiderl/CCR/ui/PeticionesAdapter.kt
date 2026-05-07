package com.leiderl.CCR.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.leiderl.CCR.data.repository.Peticion
import com.leiderl.CCR.databinding.ItemPeticionBurbujaBinding

class PeticionesAdapter(private val items: List<Peticion>) :
    RecyclerView.Adapter<PeticionesAdapter.VH>() {

    inner class VH(val b: ItemPeticionBurbujaBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemPeticionBurbujaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.tvTextoBurbuja.text  = item.texto
        holder.b.tvFechaBurbuja.text  = item.fecha
    }
}