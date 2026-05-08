package com.leiderl.CCR.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class Video(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val titulo: String,
    val predicador: String,
    val grupo: String,
    val fecha: String,
    val fechaOrden: Long,
    val urlYoutube: String,
    val libroId: Int = 0,
    val capitulo: Int = 0,
    val versiculo: String = ""   // acepta "12", "12-15", etc.
)