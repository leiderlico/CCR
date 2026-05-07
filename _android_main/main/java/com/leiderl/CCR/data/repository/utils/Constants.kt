package com.leiderl.CCR.data.repository.utils

object Constants {
    const val ANTIGUO_TESTAMENTO = "Antiguo"
    const val NUEVO_TESTAMENTO = "Nuevo"

    // Voice recognition
    const val VOICE_DEBOUNCE_MS = 1500L
    const val VOICE_RESTART_DELAY_MS = 500L
    const val SUGGESTION_TIMEOUT_MS = 8000L

    // DB
    const val DB_NAME = "biblia_rv1960.db"
    const val ASSETS_JSON = "biblia_rv1960.json"

    // Navigation args
    const val ARG_LIBRO_ID = "libroId"
    const val ARG_LIBRO_NOMBRE = "libroNombre"
    const val ARG_CAPITULO = "capitulo"
    const val ARG_CAPITULOS_TOTAL = "capitulosTotal"
    const val ARG_VERSICULO_HIGHLIGHT = "versiculoHighlight"
const val ARG_LIBRO_ABREVIACION = "libroAbreviacion"

    // Videos — Google Sheets CSV
    const val SHEET_CSV_URL = "https://docs.google.com/spreadsheets/d/e/" +
            "2PACX-1vTdak_SUYm9hTc1h4GWLDzgi-VVmcbPRV2mKr5xMAr8hNlb1jsvUpAiBIepMqiDAYX04NOmoGK18Vft" +
            "/pub?gid=0&single=true&output=csv"
}