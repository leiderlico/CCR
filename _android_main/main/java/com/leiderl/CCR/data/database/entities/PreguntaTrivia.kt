package com.leiderl.CCR.data.database.entities

data class PreguntaTrivia(
    val id: String,
    val pregunta: String,
    val correcta: String,
    val opciones: List<String>   // ya barajadas por el script
)