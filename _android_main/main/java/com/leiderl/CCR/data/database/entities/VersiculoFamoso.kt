package com.leiderl.CCR.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "versiculos_famosos",
    foreignKeys = [
        ForeignKey(
            entity = Libro::class,
            parentColumns = ["id"],
            childColumns = ["libro_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["libro_id"])]
)
data class VersiculoFamoso(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val frase_inicial: String,
    val libro_id: Int,
    val capitulo: Int,
    val versiculo: Int,
    val relevancia: Int = 5
)
