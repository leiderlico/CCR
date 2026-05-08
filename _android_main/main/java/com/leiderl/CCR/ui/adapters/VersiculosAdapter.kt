package com.leiderl.CCR.ui.adapters

import android.graphics.Color
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.leiderl.CCR.R
import com.leiderl.CCR.data.database.entities.Versiculo
import com.leiderl.CCR.utils.HighlighterSpan

class VersiculosAdapter(
    private val highlightVersiculo: Int = -1,
    private val onLongPress: ((Versiculo) -> Unit)? = null,
    private val onPress: ((Versiculo) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var highlightColors: Map<Int, Int> = emptyMap()
        set(value) { field = value; notifyDataSetChanged() }

    val seleccionados = mutableSetOf<Int>()
    var modoSeleccion: Boolean = false
        set(value) {
            field = value
            if (!value) seleccionados.clear()
            notifyDataSetChanged()
        }

    // Encabezado: "Libro Capítulo" centrado arriba de los versículos
    var tituloCapitulo: String = ""
        set(value) { field = value; notifyItemChanged(0) }

    companion object {
        private const val TYPE_HEADER    = 0
        private const val TYPE_VERSICULO = 1
    }

    // ── AsyncListDiffer con offset +1 para el header ──────────────────────
    //
    // DiffUtil opera sobre la lista interna (posiciones 0..N-1).
    // Pero en el RecyclerView la posición 0 es el header y los versículos
    // están en 1..N, por eso se suma +1 a todas las notificaciones.
    // Sin este offset, DiffUtil notificaba al header como si fuera un
    // versículo cambiado y dejaba el último versículo sin actualizar,
    // provocando scroll al fondo y parpadeo en el swipe de capítulos.
    private val differ = AsyncListDiffer(
        object : ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) =
                notifyItemRangeInserted(position + 1, count)
            override fun onRemoved(position: Int, count: Int) =
                notifyItemRangeRemoved(position + 1, count)
            override fun onMoved(fromPosition: Int, toPosition: Int) =
                notifyItemMoved(fromPosition + 1, toPosition + 1)
            override fun onChanged(position: Int, count: Int, payload: Any?) =
                notifyItemRangeChanged(position + 1, count, payload)
        },
        AsyncDifferConfig.Builder(VersiculoDiff()).build()
    )

    fun submitList(list: List<Versiculo>?, commitCallback: (() -> Unit)? = null) {
        differ.submitList(list) { commitCallback?.invoke() }
    }

    private fun getVersiculo(position: Int): Versiculo = differ.currentList[position - 1]

    // ─────────────────────────────────────────────────────────────────────

    override fun getItemViewType(position: Int): Int =
        if (position == 0) TYPE_HEADER else TYPE_VERSICULO

    override fun getItemCount(): Int = differ.currentList.size + 1  // +1 por el header

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_versiculo_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_versiculo, parent, false)
            VersiculoViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.bind(tituloCapitulo)
            return
        }

        val v = getVersiculo(position)
        val isHighlighted  = v.versiculo == highlightVersiculo
        val highlightColor = highlightColors[v.versiculo]
        val isSeleccionado = v.versiculo in seleccionados
        (holder as VersiculoViewHolder).bind(v, isHighlighted, highlightColor, isSeleccionado)

        holder.itemView.setOnLongClickListener {
            onLongPress?.invoke(v)
            true
        }
        holder.itemView.setOnClickListener {
            if (modoSeleccion) {
                if (v.versiculo in seleccionados) seleccionados.remove(v.versiculo)
                else seleccionados.add(v.versiculo)
                notifyItemChanged(position)
                onPress?.invoke(v)
            }
        }
    }

    // ── ViewHolders ───────────────────────────────────────────────────────

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvHeader: TextView = view.findViewById(R.id.tvCapituloHeader)
        fun bind(titulo: String) { tvHeader.text = titulo }
    }

    inner class VersiculoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNumero: TextView = view.findViewById(R.id.tvVersiculoNumero)
        private val tvTexto: TextView  = view.findViewById(R.id.tvVersiculoTexto)

        fun bind(v: Versiculo, highlighted: Boolean, highlightColor: Int?, isSeleccionado: Boolean) {
            tvNumero.text = v.versiculo.toString()
            itemView.setBackgroundColor(Color.TRANSPARENT)

            when {
                isSeleccionado -> {
                    val spannable = SpannableString(v.texto)
                    spannable.setSpan(BackgroundColorSpan(0x6690CAF9.toInt()), 0, v.texto.length, 0)
                    tvTexto.text = spannable
                }
                highlightColor != null -> {
                    val spannable = SpannableString(v.texto)
                    spannable.setSpan(HighlighterSpan(itemView.context, highlightColor), 0, v.texto.length, 0)
                    tvTexto.text = spannable
                }
                highlighted -> {
                    val spannable = SpannableString(v.texto)
                    spannable.setSpan(
                        BackgroundColorSpan(ContextCompat.getColor(itemView.context, R.color.highlight_color)),
                        0, v.texto.length, 0
                    )
                    tvTexto.text = spannable
                }
                else -> tvTexto.text = v.texto
            }
        }
    }

    class VersiculoDiff : DiffUtil.ItemCallback<Versiculo>() {
        override fun areItemsTheSame(a: Versiculo, b: Versiculo) = a.id == b.id
        override fun areContentsTheSame(a: Versiculo, b: Versiculo) = a == b
    }
}