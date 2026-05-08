package com.leiderl.CCR.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leiderl.CCR.data.database.entities.VersiculoFamoso

@Dao
interface VersiculoFamosoDao {

    @Query("SELECT * FROM versiculos_famosos ORDER BY relevancia DESC")
    suspend fun getAll(): List<VersiculoFamoso>

    // Ya no usamos searchByFrase con LIKE — el match por palabras se hace en Kotlin
    // para tolerar palabras saltadas. Dejamos esta query solo como fallback exacto.
    @Query("""
        SELECT * FROM versiculos_famosos 
        WHERE LOWER(:text) LIKE LOWER('%' || frase_inicial || '%') 
           OR LOWER(frase_inicial) LIKE LOWER('%' || :text || '%')
        ORDER BY relevancia DESC
        LIMIT 3
    """)
    suspend fun searchByFraseExacta(text: String): List<VersiculoFamoso>

    @Query("SELECT COUNT(*) FROM versiculos_famosos")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(famosos: List<VersiculoFamoso>)
}