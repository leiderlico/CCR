package com.leiderl.CCR.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "libros")
data class Libro(
    @PrimaryKey val id: Int,
    val nombre: String,
    val abreviacion: String,
    val testamento: String,
    val orden: Int,
    val capitulos: Int
)
