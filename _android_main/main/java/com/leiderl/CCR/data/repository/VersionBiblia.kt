package com.leiderl.CCR.data.repository

enum class VersionBiblia(
    val nombreMostrar: String,
    val archivoAsset: String?,
    val urlDescarga: String?
) {
    RV1960("RV1960",  "biblia_rv1960.json", null),
    NVI("NVI",        "biblia_nvi.json",    null),
    NTV("NTV",        "biblia_ntv.json",    null),
    RVA2015("RVA2015", null, "https://github.com/TU_USUARIO/TU_REPO/releases/download/v1/biblia_rva2015.json"),
    LBLA("LBLA",       null, "https://github.com/TU_USUARIO/TU_REPO/releases/download/v1/biblia_lbla.json"),
    PESHITTA("PESHITTA", null, "https://github.com/TU_USUARIO/TU_REPO/releases/download/v1/biblia_peshitta.json"),
    BDO("BDO",         null, "https://github.com/TU_USUARIO/TU_REPO/releases/download/v1/biblia_bdo.json"),
    TLA("TLA",         null, "https://github.com/TU_USUARIO/TU_REPO/releases/download/v1/biblia_tla.json"),
}