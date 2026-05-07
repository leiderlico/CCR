package com.leiderl.CCR.data.model

data class Devocional(
    val año: Int,
    val dia_del_año: Int,
    val fecha: String,
    val titulo: String,
    val versiculo_texto: String,
    val versiculo_referencia: String,
    val cuerpo: String
)