package com.leiderl.CCR.ui.adapters

import android.content.res.Configuration
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.leiderl.CCR.R
import com.leiderl.CCR.data.database.entities.Libro

class LibrosAdapter(
    private val onLibroClick: (Libro) -> Unit
) : ListAdapter<Libro, LibrosAdapter.LibroViewHolder>(LibroDiffCallback()) {

    // Color final del degradado: #0D7AB0 día / #06334B noche
    private val headerDayR = 13;  private val headerDayG = 122; private val headerDayB = 176
    private val headerNightR = 6; private val headerNightG = 51; private val headerNightB = 75

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibroViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_libro, parent, false)
        return LibroViewHolder(view)
    }

    override fun onBindViewHolder(holder: LibroViewHolder, position: Int) {
        val total = itemCount
        val isNight = (holder.itemView.context.resources.configuration.uiMode
                and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // Modo noche: #0F1E2E → #06334B  (azul oscuro que destaca sobre negro)
        // Modo día:   #F5FBFF → #0D7AB0  (blanco azulado → azul)
        val startR = if (isNight) 15  else 245
        val startG = if (isNight) 30  else 251
        val startB = if (isNight) 46  else 255

        val hR = if (isNight) headerNightR else headerDayR
        val hG = if (isNight) headerNightG else headerDayG
        val hB = if (isNight) headerNightB else headerDayB

        val t = if (total > 1) position.toFloat() / (total - 1).toFloat() else 0f
        val r = lerp(startR, hR, t)
        val g = lerp(startG, hG, t)
        val b = lerp(startB, hB, t)

        val textColor = if (isNight) Color.parseColor("#E8E8E8") else Color.parseColor("#1A1A1A")
        holder.bind(getItem(position), onLibroClick, Color.rgb(r, g, b), textColor)
    }

    private fun lerp(a: Int, b: Int, t: Float): Int = (a + (b - a) * t).toInt()

    class LibroViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombre: TextView = view.findViewById(R.id.tvNombre)

        fun bind(libro: Libro, onClick: (Libro) -> Unit, bgColor: Int, textColor: Int) {
            tvNombre.text = libro.nombre
            tvNombre.setTextColor(textColor)

            // El itemView es el LinearLayout interno — su padre es la MaterialCardView
            val card = itemView.parent as? com.google.android.material.card.MaterialCardView
            card?.setCardBackgroundColor(bgColor)

            // Noche: borde sutil para que la card destaque sobre el fondo negro
            val isNight = (itemView.context.resources.configuration.uiMode
                    and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            if (isNight) {
                card?.strokeWidth = 1
                card?.strokeColor = Color.argb(40, 26, 174, 232) // #281AAEE8
                card?.cardElevation = 0f
            } else {
                card?.strokeWidth = 0
                card?.cardElevation = 2f * itemView.context.resources.displayMetrics.density
            }

            itemView.setOnClickListener { onClick(libro) }
        }
    }
}

class LibroDiffCallback : DiffUtil.ItemCallback<Libro>() {
    override fun areItemsTheSame(oldItem: Libro, newItem: Libro) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Libro, newItem: Libro) = oldItem == newItem
}