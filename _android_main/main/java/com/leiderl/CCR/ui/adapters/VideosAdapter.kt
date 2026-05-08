package com.leiderl.CCR.ui.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.leiderl.CCR.R
import com.leiderl.CCR.data.database.entities.Video
import com.leiderl.CCR.ui.viewmodels.GrupoVideos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class VideosAdapter(
    private val onVideoClick: (Video) -> Unit,
    private val onGrupoClick: (String) -> Unit,
    private val onChipClick: (libroId: Int, capitulo: Int) -> Unit,
    private val nombreLibro: (Int) -> String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_VIDEO  = 1
        private const val MAX_PREVIEW = 3
    }

    private val items = mutableListOf<Any>()
    private var grupos: List<GrupoVideos> = emptyList()

    // Modo normal — con headers de grupo
    fun submitGrupos(nuevosGrupos: List<GrupoVideos>) {
        grupos = nuevosGrupos
        reconstruirItems()
    }

    // Modo plano — dentro de un grupo, sin headers
    fun submitVideosSinGrupos(videos: List<Video>) {
        items.clear()
        items.addAll(videos)
        notifyDataSetChanged()
    }

    private fun reconstruirItems() {
        items.clear()
        for (grupo in grupos) {
            items.add(grupo)
            val videosAMostrar = if (grupo.expandido) grupo.videos
            else grupo.videos.take(MAX_PREVIEW)
            items.addAll(videosAMostrar)
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (items[position] is GrupoVideos) TYPE_HEADER else TYPE_VIDEO

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(inflater.inflate(R.layout.item_grupo_header, parent, false))
        } else {
            VideoVH(inflater.inflate(R.layout.item_video_card, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderVH -> holder.bind(items[position] as GrupoVideos)
            is VideoVH  -> holder.bind(items[position] as Video)
        }
    }

    inner class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombre  = view.findViewById<TextView>(R.id.tvGrupoNombre)
        private val tvConteo  = view.findViewById<TextView>(R.id.tvGrupoConteo)
        private val ivChevron = view.findViewById<ImageView>(R.id.ivGrupoChevron)

        fun bind(grupo: GrupoVideos) {
            tvNombre.text = grupo.nombre
            val total = grupo.videos.size
            tvConteo.text = "$total ${if (total == 1) "prédica" else "prédicas"}"
            ivChevron.rotation = if (grupo.expandido) 180f else 0f
            itemView.setOnClickListener {
                if (grupo.videos.size > MAX_PREVIEW) onGrupoClick(grupo.nombre)
            }
        }
    }

    inner class VideoVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitulo     = view.findViewById<TextView>(R.id.tvVideoTitulo)
        private val tvPredicador = view.findViewById<TextView>(R.id.tvVideoPredicador)
        private val tvFecha      = view.findViewById<TextView>(R.id.tvVideoFecha)
        private val ivThumbnail  = view.findViewById<ImageView>(R.id.ivVideoThumbnail)
        private val chipLibro    = view.findViewById<Chip>(R.id.chipLibro)

        fun bind(video: Video) {
            tvTitulo.text = video.titulo
            tvPredicador.text = video.predicador
            tvFecha.text = video.fecha
            tvFecha.visibility = if (video.fecha.isNotEmpty()) View.VISIBLE else View.GONE

            // Miniatura desde YouTube — no requiere librerías externas
            val videoId = extraerYoutubeId(video.urlYoutube)
            if (videoId != null) {
                cargarThumbnail(ivThumbnail, "https://img.youtube.com/vi/$videoId/mqdefault.jpg")
            } else {
                ivThumbnail.setImageDrawable(null)
            }

            if (video.libroId > 0) {
                val nombre = nombreLibro(video.libroId)
                chipLibro.text = when {
                    video.capitulo > 0 && video.versiculo.isNotEmpty() -> "$nombre ${video.capitulo}: ${video.versiculo}"
                    video.capitulo > 0 -> "$nombre ${video.capitulo}"
                    else -> nombre
                }
                chipLibro.visibility = View.VISIBLE
                chipLibro.isCheckable = false
                chipLibro.setOnClickListener { onChipClick(video.libroId, video.capitulo) }
            } else {
                chipLibro.visibility = View.GONE
                chipLibro.setOnClickListener(null)
            }

            itemView.setOnClickListener { onVideoClick(video) }
        }
    }

    // Extrae el ID de video de cualquier formato de URL de YouTube
    private fun extraerYoutubeId(url: String): String? {
        val patterns = listOf(
            Regex("youtu\\.be/([\\w-]{11})"),
            Regex("youtube\\.com/watch\\?v=([\\w-]{11})"),
            Regex("youtube\\.com/shorts/([\\w-]{11})")
        )
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    // Carga la miniatura en background sin librerías externas
    private fun cargarThumbnail(imageView: ImageView, url: String) {
        imageView.setImageDrawable(null)
        MainScope().launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.connect()
                    BitmapFactory.decodeStream(connection.inputStream)
                }
                if (bitmap != null) imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // Sin imagen si falla la descarga (sin internet, etc.)
            }
        }
    }
}