package com.leiderl.CCR.utils

import java.text.Normalizer

object BibliaParser {

    // ─────────────────────────────────────────────────────────────────────────
    // MAPA DE LIBROS
    // Criterios de expansión:
    //   • Libros con H inicial: el micrófono suele omitirla (hechos→echos, hebreos→ebreos, etc.)
    //   • Libros que terminan en consonante rara: rut, job, nahum, etc. → variantes de cierre
    //   • Libros numerados: cubrir "primero/a", "segundo/a", "uno/a", "dos", "1", "2", "3"
    //   • Variantes fonéticas comunes del reconocedor español
    // ─────────────────────────────────────────────────────────────────────────
    private val LIBROS_MAP = mapOf(

        // ── GÉNESIS ──────────────────────────────────────────────────────────
        "genesis" to 1, "gen" to 1, "gn" to 1,
        "jenesis" to 1, "henesis" to 1,            // variante fonética j/g

        // ── ÉXODO ────────────────────────────────────────────────────────────
        "exodo" to 2, "ex" to 2,
        "exo" to 2, "esodo" to 2,                  // micrófono a veces corta

        // ── LEVÍTICO ─────────────────────────────────────────────────────────
        "levitico" to 3, "lev" to 3, "lv" to 3,
        "lebitiko" to 3, "lebitico" to 3,

        // ── NÚMEROS ──────────────────────────────────────────────────────────
        "numeros" to 4, "num" to 4, "nm" to 4,
        "numero" to 4,

        // ── DEUTERONOMIO ─────────────────────────────────────────────────────
        "deuteronomio" to 5, "deut" to 5, "dt" to 5,
        "deuterenomio" to 5, "deuteronimo" to 5,   // errores fonéticos comunes

        // ── JOSUÉ ────────────────────────────────────────────────────────────
        "josue" to 6, "jos" to 6,
        "josué" to 6, "hosue" to 6, "josua" to 6,

        // ── JUECES ───────────────────────────────────────────────────────────
        "jueces" to 7, "jue" to 7,
        "hueces" to 7, "juece" to 7,               // H inicial omitida / sin S final

        // ── RUT ──────────────────────────────────────────────────────────────
        // Consonante final difícil → el micrófono suele añadir vocal
        "rut" to 8, "rt" to 8,
        "rut" to 8, "ruth" to 8,
        "ruts" to 8, "rus" to 8, "ruz" to 8,       // cierres alternativos
        "rute" to 8, "ru" to 8,                   // con vocal añadida

        // ── 1 SAMUEL ─────────────────────────────────────────────────────────
        "1samuel" to 9, "1sam" to 9, "1s" to 9, "samuel" to 9,
        "primersamuel" to 9, "primerosamuel" to 9, "primerasamuel" to 9,
        "primer de samuel" to 9, "primero de samuel" to 9, "primera de samuel" to 9,
        "unosamuel" to 9, "uno de samuel" to 9,

        // ── 2 SAMUEL ─────────────────────────────────────────────────────────
        "2samuel" to 10, "2sam" to 10, "2s" to 10,
        "segundosamuel" to 10, "segundasamuel" to 10,
        "segundo de samuel" to 10, "segunda de samuel" to 10,
        "dossamuel" to 10, "dos de samuel" to 10,

        // ── 1 REYES ──────────────────────────────────────────────────────────
        "1reyes" to 11, "1re" to 11, "1r" to 11, "reyes" to 11,
        "primerreyes" to 11, "primeroreyes" to 11,
        "primer de reyes" to 11, "primero de reyes" to 11, "primera de reyes" to 11,
        "uno de reyes" to 11, "unoreyes" to 11,

        // ── 2 REYES ──────────────────────────────────────────────────────────
        "2reyes" to 12, "2re" to 12, "2r" to 12,
        "segundoreyes" to 12, "segundo de reyes" to 12, "segunda de reyes" to 12,
        "dosreyes" to 12, "dos de reyes" to 12,

        // ── 1 CRÓNICAS ───────────────────────────────────────────────────────
        "1cronicas" to 13, "1cr" to 13, "cronicas" to 13,
        "primercronicas" to 13, "primerocronicas" to 13,
        "primer de cronicas" to 13, "primero de cronicas" to 13, "primera de cronicas" to 13,
        "uno de cronicas" to 13,

        // ── 2 CRÓNICAS ───────────────────────────────────────────────────────
        "2cronicas" to 14, "2cr" to 14,
        "segundocronicas" to 14, "segundo de cronicas" to 14, "segunda de cronicas" to 14,
        "dos de cronicas" to 14,

        // ── ESDRAS ───────────────────────────────────────────────────────────
        "esdras" to 15, "esd" to 15,
        "esdra" to 15, "ezdras" to 15,

        // ── NEHEMÍAS ─────────────────────────────────────────────────────────
        "nehemias" to 16, "neh" to 16,
        "neemias" to 16, "nehemia" to 16,           // H media omitida

        // ── ESTER ────────────────────────────────────────────────────────────
        "ester" to 17, "est" to 17,
        "esther" to 17, "hester" to 17,

        // ── JOB ──────────────────────────────────────────────────────────────
        // Termina en consonante — micrófono puede añadir vocal o confundir
        "job" to 18,  "jo" to 18,
        "jov" to 18, "joe" to 18, "yob" to 18,     // variantes fonéticas
        "jobe" to 18, "jobs" to 18,

        // ── SALMOS ───────────────────────────────────────────────────────────
        "salmos" to 19, "salmo" to 19, "sal" to 19, "sl" to 19,
        "psalm" to 19, "psalmos" to 19,             // forma inglesa/latina

        // ── PROVERBIOS ───────────────────────────────────────────────────────
        "proverbios" to 20, "prov" to 20, "pr" to 20,
        "proverbio" to 20, "proberbios" to 20,

        // ── ECLESIASTÉS ──────────────────────────────────────────────────────
        "eclesiastes" to 21, "ecl" to 21, "ec" to 21,
        "eclesiastes" to 21, "eclesiastés" to 21,
        "ecclesiastes" to 21,                       // forma inglesa

        // ── CANTARES ─────────────────────────────────────────────────────────
        "cantares" to 22, "cnt" to 22,
        "cantar" to 22, "cantardelos cantares" to 22,
        "cantico" to 22, "canticos" to 22,

        // ── ISAÍAS ───────────────────────────────────────────────────────────
        "isaias" to 23, "is" to 23,
        "esaias" to 23, "ysaias" to 23,

        // ── JEREMÍAS ─────────────────────────────────────────────────────────
        "jeremias" to 24, "jer" to 24,
        "jeremía" to 24, "heremias" to 24,

        // ── LAMENTACIONES ────────────────────────────────────────────────────
        "lamentaciones" to 25, "lam" to 25, "lm" to 25,
        "lamentacion" to 25,

        // ── EZEQUIEL ─────────────────────────────────────────────────────────
        "ezequiel" to 26, "ez" to 26,
        "ezekiel" to 26, "hesekiel" to 26,

        // ── DANIEL ───────────────────────────────────────────────────────────
        "daniel" to 27, "dn" to 27,
        "daniyel" to 27,

        // ── OSEAS ────────────────────────────────────────────────────────────
        "oseas" to 28, "os" to 28,
        "hoseas" to 28, "osea" to 28,               // H inicial y sin S final

        // ── JOEL ─────────────────────────────────────────────────────────────
        "joel" to 29, "jl" to 29,
        "yoel" to 29, "juel" to 29,

        // ── AMÓS ─────────────────────────────────────────────────────────────
        "amos" to 30, "am" to 30,
        "amós" to 30,

        // ── ABDÍAS ───────────────────────────────────────────────────────────
        "abdias" to 31, "abd" to 31,
        "obadias" to 31, "abdía" to 31,

        // ── JONÁS ────────────────────────────────────────────────────────────
        "jonas" to 32, "jon" to 32,
        "jonás" to 32, "yonas" to 32,

        // ── MIQUEAS ──────────────────────────────────────────────────────────
        "miqueas" to 33, "mi" to 33,
        "miquea" to 33, "mikeas" to 33,

        // ── NAHÚM ────────────────────────────────────────────────────────────
        // Termina en M — micrófono puede cortar o añadir vocal
        "nahum" to 34, "nah" to 34,
        "naum" to 34, "nahun" to 34,                // N en lugar de M final
        "nahume" to 34, "nahúm" to 34,

        // ── HABACUC ──────────────────────────────────────────────────────────
        // Termina en C — rara en español, muchas variantes
        "habacuc" to 35, "hab" to 35,
        "habakuk" to 35, "habacuk" to 35,
        "habacús" to 35, "abacuc" to 35,            // H inicial omitida
        "abacuk" to 35, "habacuq" to 35,

        // ── SOFONÍAS ─────────────────────────────────────────────────────────
        "sofonias" to 36, "sof" to 36,
        "sofonía" to 36, "sofonia" to 36,

        // ── HAGEO ────────────────────────────────────────────────────────────
        // H inicial → micrófono frecuentemente la omite
        "hageo" to 37, "hag" to 37,
        "ageo" to 37, "ajeo" to 37,                 // sin H / con J
        "hageo" to 37, "hago" to 37,

        // ── ZACARÍAS ─────────────────────────────────────────────────────────
        "zacarias" to 38, "zac" to 38,
        "zacarías" to 38, "sacarias" to 38,         // Z→S frecuente en español

        // ── MALAQUÍAS ────────────────────────────────────────────────────────
        "malaquias" to 39, "mal" to 39,
        "malaquía" to 39, "malachias" to 39,

        // ── MATEO ────────────────────────────────────────────────────────────
        "mateo" to 40, "mt" to 40,
        "matteo" to 40,

        // ── MARCOS ───────────────────────────────────────────────────────────
        "marcos" to 41, "mr" to 41, "mc" to 41,
        "marco" to 41, "markos" to 41,

        // ── LUCAS ────────────────────────────────────────────────────────────
        "lucas" to 42, "lc" to 42,
        "luca" to 42, "lukas" to 42,

        // ── JUAN ─────────────────────────────────────────────────────────────
        "juan" to 43, "jn" to 43,
        "jhuan" to 43, "huan" to 43,                // H añadida / confusión

        // ── HECHOS ───────────────────────────────────────────────────────────
        // H inicial → muy frecuente que el micrófono la omita
        "hechos" to 44, "hch" to 44,
        "echos" to 44, "echo" to 44,                // sin H
        "hechos delos apostoles" to 44,
        "actos" to 44, "acto" to 44,                // forma usada por algunos hispanohablantes
        "hecho" to 44,

        // ── ROMANOS ──────────────────────────────────────────────────────────
        "romanos" to 45, "ro" to 45, "rom" to 45,
        "romano" to 45,

        // ── 1 CORINTIOS ──────────────────────────────────────────────────────
        "1corintios" to 46, "1cor" to 46, "1co" to 46, "corintios" to 46,
        "primercorintios" to 46, "primerocorintios" to 46,
        "primer de corintios" to 46, "primero de corintios" to 46, "primera de corintios" to 46,
        "uno de corintios" to 46,

        // ── 2 CORINTIOS ──────────────────────────────────────────────────────
        "2corintios" to 47, "2cor" to 47, "2co" to 47,
        "segundocorintios" to 47, "segundo de corintios" to 47, "segunda de corintios" to 47,
        "dos de corintios" to 47,

        // ── GÁLATAS ──────────────────────────────────────────────────────────
        "galatas" to 48, "gal" to 48, "ga" to 48,
        "gálatas" to 48, "galata" to 48,

        // ── EFESIOS ──────────────────────────────────────────────────────────
        "efesios" to 49, "ef" to 49,
        "efesio" to 49, "efesos" to 49,

        // ── FILIPENSES ───────────────────────────────────────────────────────
        "filipenses" to 50,
        "filipense" to 50, "filipensios" to 50,     // error fonético común

        // ── COLOSENSES ───────────────────────────────────────────────────────
        "colosenses" to 51, "col" to 51,
        "colosense" to 51, "colosenses" to 51,

        // ── 1 TESALONICENSES ─────────────────────────────────────────────────
        "1tesalonicenses" to 52, "1tes" to 52, "1ts" to 52, "tesalonicenses" to 52,
        "tesalonicenese" to 52,
        "primertesalonicenses" to 52, "primerotesalonicenses" to 52,
        "primer de tesalonicenses" to 52, "primero de tesalonicenses" to 52, "primera de tesalonicenses" to 52,
        "uno de tesalonicenses" to 52,

        // ── 2 TESALONICENSES ─────────────────────────────────────────────────
        "2tesalonicenses" to 53, "2tes" to 53, "2ts" to 53,
        "segundotesalonicenses" to 53, "segundo de tesalonicenses" to 53, "segunda de tesalonicenses" to 53,
        "dos de tesalonicenses" to 53,

        // ── 1 TIMOTEO ────────────────────────────────────────────────────────
        "1timoteo" to 54, "1tim" to 54, "1ti" to 54, "timoteo" to 54,
        "primertimoteo" to 54, "primerotimoteo" to 54,
        "primer de timoteo" to 54, "primero de timoteo" to 54, "primera de timoteo" to 54,
        "uno de timoteo" to 54,

        // ── 2 TIMOTEO ────────────────────────────────────────────────────────
        "2timoteo" to 55, "2tim" to 55, "2ti" to 55,
        "segundotimoteo" to 55, "segundo de timoteo" to 55, "segunda de timoteo" to 55,
        "dos de timoteo" to 55,

        // ── TITO ─────────────────────────────────────────────────────────────
        "tito" to 56, "tit" to 56,
        "titus" to 56,

        // ── FILEMÓN ──────────────────────────────────────────────────────────
        "filemon" to 57, "flm" to 57,
        "filemón" to 57, "filemon" to 57,

        // ── HEBREOS ──────────────────────────────────────────────────────────
        // H inicial → el micrófono la omite con frecuencia
        "hebreos" to 58, "he" to 58, "heb" to 58,
        "ebreos" to 58, "ebrero" to 58,             // sin H
        "hebreo" to 58,

        // ── SANTIAGO ─────────────────────────────────────────────────────────
        "santiago" to 59, "stg" to 59,
        "santago" to 59, "santiagos" to 59,

        // ── 1 PEDRO ──────────────────────────────────────────────────────────
        "1pedro" to 60, "1pe" to 60, "1p" to 60, "pedro" to 60,
        "primerpedro" to 60, "primeropedro" to 60, "primerapedro" to 60,
        "primer de pedro" to 60, "primero de pedro" to 60, "primera de pedro" to 60,
        "uno de pedro" to 60,

        // ── 2 PEDRO ──────────────────────────────────────────────────────────
        "2pedro" to 61, "2pe" to 61, "2p" to 61,
        "segundopedro" to 61, "segundo de pedro" to 61, "segunda de pedro" to 61,
        "dos de pedro" to 61,

        // ── 1 JUAN ───────────────────────────────────────────────────────────
        "1juan" to 62, "1jn" to 62,
        "primerjuan" to 62, "primerojuan" to 62,
        "primer de juan" to 62, "primero de juan" to 62, "primera de juan" to 62,
        "uno de juan" to 62,

        // ── 2 JUAN ───────────────────────────────────────────────────────────
        "2juan" to 63, "2jn" to 63,
        "segundojuan" to 63, "segundo de juan" to 63, "segunda de juan" to 63,
        "dos de juan" to 63,

        // ── 3 JUAN ───────────────────────────────────────────────────────────
        "3juan" to 64, "3jn" to 64,
        "tercerjuan" to 64, "tercerojuan" to 64,
        "tercer de juan" to 64, "tercero de juan" to 64, "tercera de juan" to 64,
        "tres de juan" to 64,

        // ── JUDAS ────────────────────────────────────────────────────────────
        "judas" to 65, "jud" to 65,
        "juda" to 65, "yudas" to 65,

        // ── APOCALIPSIS ──────────────────────────────────────────────────────
        "apocalipsis" to 66, "ap" to 66, "apoc" to 66,
        "apocalipsi" to 66, "apocalisis" to 66,     // variantes de cierre
        "revelacion" to 66, "revelaciones" to 66    // nombre alterno usado
    )

    // Números en palabras para capítulos/versículos
    private val NUMEROS_PALABRAS = mapOf(
        "uno" to 1, "una" to 1, "primer" to 1, "primero" to 1, "primera" to 1,
        "dos" to 2, "segundo" to 2, "segunda" to 2,
        "tres" to 3, "tercero" to 3, "tercera" to 3,
        "cuatro" to 4, "cuarto" to 4,
        "cinco" to 5, "quinto" to 5,
        "seis" to 6, "sexto" to 6,
        "siete" to 7, "septimo" to 7,
        "ocho" to 8, "octavo" to 8,
        "nueve" to 9, "noveno" to 9,
        "diez" to 10, "decimo" to 10,
        "once" to 11, "doce" to 12, "trece" to 13,"13" to 13,
        "catorce" to 14, "quince" to 15, "dieciseis" to 16,
        "diecisiete" to 17, "dieciocho" to 18, "diecinueve" to 19,
        "veinte" to 20, "veintiuno" to 21, "veintidos" to 22, "veintitres" to 23,
        "veinticuatro" to 24, "veinticinco" to 25, "veintiseis" to 26,
        "veintisiete" to 27, "veintiocho" to 28, "veintinueve" to 29,
        "treinta" to 30, "cuarenta" to 40, "cincuenta" to 50,
        "sesenta" to 60, "setenta" to 70, "ochenta" to 80, "noventa" to 90,
        "cien" to 100, "ciento" to 100
    )

    data class ReferenciaBiblica(
        val libroId: Int,
        val capitulo: Int,
        val versiculo: Int?
    )

    /**
     * Normaliza texto: minúsculas, sin acentos, sin caracteres especiales
     */
    fun normalizar(texto: String): String {
        val sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
        return sinAcentos.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun palabraANumero(palabra: String): Int? {
        return palabra.toIntOrNull() ?: NUMEROS_PALABRAS[palabra]
    }

    /**
     * Intenta parsear una referencia bíblica del texto de voz.
     *
     * Cubre casos como:
     *   "génesis cinco ocho"           → (1, 5, 8)
     *   "juan tres dieciséis"          → (43, 3, 16)
     *   "primer samuel tres"           → (9, 3, null)
     *   "segunda pedro uno cinco"      → (61, 1, 5)
     *   "echos dos cuatro"             → (44, 2, 4)  ← Hechos sin H
     *   "ageo dos nueve"               → (37, 2, 9)  ← Hageo sin H
     */
    fun parsearReferencia(textoVoz: String): ReferenciaBiblica? {
        val normalizado = normalizar(textoVoz)
        val palabras = normalizado.split(" ").filter { it.isNotBlank() }
        if (palabras.isEmpty()) return null

        var libroId: Int? = null
        var indicePostLibro = 0

        // Buscar el libro probando combinaciones de hasta 5 palabras
        // El diccionario ya incluye todas las variantes: "primera de reyes", "1reyes", etc.
        for (len in 5 downTo 1) {
            if (palabras.size < len) continue
            val candidato = palabras.take(len).joinToString(" ")
            val id = LIBROS_MAP[candidato]
            if (id != null) {
                libroId = id
                indicePostLibro = len
                break
            }
            // También intentar sin espacios (ej: "primersamuel")
            val candidatoSinEspacios = palabras.take(len).joinToString("")
            val id2 = LIBROS_MAP[candidatoSinEspacios]
            if (id2 != null) {
                libroId = id2
                indicePostLibro = len
                break
            }
        }

        if (libroId == null) return null

        val resto = palabras.drop(indicePostLibro)
        if (resto.isEmpty()) return ReferenciaBiblica(libroId, 1, 1)

        // Palabras que indican capítulo o versículo — sirven como marcadores de posición
        val PALABRAS_CAPITULO = setOf("capitulo", "capitulos", "cap")
        val PALABRAS_VERSICULO = setOf("versiculo", "versiculos", "vers", "verso", "versos")

        // Buscar si hay marcadores explícitos en el resto
        val idxCap = resto.indexOfFirst { it in PALABRAS_CAPITULO }
        val idxVers = resto.indexOfFirst { it in PALABRAS_VERSICULO }

        val capitulo: Int
        val versiculo: Int?

        when {
            // "lucas capitulo 3 versiculo 1" — ambos marcadores presentes
            idxCap >= 0 && idxVers >= 0 -> {
                capitulo = palabraANumero(resto.getOrElse(idxCap + 1) { "" }) ?: 1
                versiculo = palabraANumero(resto.getOrElse(idxVers + 1) { "" })
            }
            // "lucas capitulo 3" — solo marcador de capítulo
            idxCap >= 0 -> {
                capitulo = palabraANumero(resto.getOrElse(idxCap + 1) { "" }) ?: 1
                versiculo = palabraANumero(resto.getOrElse(idxCap + 2) { "" })
            }
            // "lucas versiculo 1" — solo marcador de versículo (raro pero posible)
            idxVers >= 0 -> {
                capitulo = palabraANumero(resto.getOrElse(0) { "" }) ?: 1
                versiculo = palabraANumero(resto.getOrElse(idxVers + 1) { "" })
            }
            // "lucas 3 1" — sin marcadores, orden directo
            else -> {
                capitulo = palabraANumero(resto.getOrElse(0) { "" }) ?: 1
                versiculo = if (resto.size > 1) palabraANumero(resto[1]) else null
            }
        }

        return ReferenciaBiblica(libroId, capitulo, versiculo)
    }

    /**
     * Detecta si el texto parece una referencia bíblica.
     * Comprueba las primeras palabras solas y combinadas, incluyendo
     * casos donde la primera palabra es un prefijo numérico.
     */
    fun esReferencia(texto: String): Boolean {
        val normalizado = normalizar(texto)
        val palabras = normalizado.split(" ").filter { it.isNotBlank() }
        if (palabras.isEmpty()) return false

        // Buscar en el diccionario con hasta 5 palabras (con y sin espacios)
        for (len in 1..minOf(5, palabras.size)) {
            val candidato = palabras.take(len).joinToString(" ")
            if (LIBROS_MAP.containsKey(candidato)) return true
            val candidatoSinEspacios = palabras.take(len).joinToString("")
            if (LIBROS_MAP.containsKey(candidatoSinEspacios)) return true
        }

        return false
    }
}