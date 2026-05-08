package com.leiderl.CCR.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "versiculos",
    foreignKeys = [
        ForeignKey(
            entity = Libro::class,
            parentColumns = ["id"],
            childColumns = ["libro_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["libro_id"]),
        Index(value = ["libro_id", "capitulo"]),
        Index(value = ["libro_id", "capitulo", "versiculo"], unique = true)
    ]
)
data class Versiculo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val libro_id: Int,
    val capitulo: Int,
    val versiculo: Int,
    val texto: String,                          // Original con tildes y ñ → mostrar al usuario
    @ColumnInfo(name = "texto_busqueda")
    val textoBusqueda: String = ""              // Normalizado sin tildes/ñ → solo para buscar
)