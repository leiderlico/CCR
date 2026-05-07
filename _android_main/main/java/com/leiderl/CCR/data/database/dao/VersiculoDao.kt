package com.leiderl.CCR.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leiderl.CCR.data.database.entities.Versiculo
import kotlinx.coroutines.flow.Flow

@Dao
interface VersiculoDao {
    @Query("SELECT * FROM versiculos WHERE libro_id = :libroId AND capitulo = :capitulo ORDER BY versiculo ASC")
    fun getVersiculos(libroId: Int, capitulo: Int): Flow<List<Versiculo>>

    @Query("SELECT * FROM versiculos WHERE libro_id = :libroId AND capitulo = :capitulo ORDER BY versiculo ASC")
    suspend fun getVersiculosList(libroId: Int, capitulo: Int): List<Versiculo>

    @Query("SELECT * FROM versiculos WHERE libro_id = :libroId AND capitulo = :capitulo AND versiculo = :versiculoNum LIMIT 1")
    suspend fun getVersiculo(libroId: Int, capitulo: Int, versiculoNum: Int): Versiculo?

    @Query("SELECT * FROM versiculos WHERE libro_id = :libroId AND capitulo = 1 AND versiculo = 1 LIMIT 1")
    suspend fun getPrimerVersiculoDeLibro(libroId: Int): Versiculo?

    @Query("SELECT COUNT(*) FROM versiculos")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(versiculos: List<Versiculo>)

    // ✅ Búsqueda en texto_busqueda (normalizado sin tildes) → más preciso con voz
    @Query("SELECT * FROM versiculos WHERE texto_busqueda LIKE ('%' || :query || '%') LIMIT :limit")
    suspend fun searchByContent(query: String, limit: Int = 10): List<Versiculo>

    @Query("SELECT * FROM versiculos WHERE texto_busqueda LIKE ('%' || :word1 || '%') AND texto_busqueda LIKE ('%' || :word2 || '%') AND texto_busqueda LIKE ('%' || :word3 || '%') LIMIT :limit")
    suspend fun searchByThreeWords(word1: String, word2: String, word3: String, limit: Int = 5): List<Versiculo>

    @Query("SELECT * FROM versiculos WHERE texto_busqueda LIKE ('%' || :word1 || '%') AND texto_busqueda LIKE ('%' || :word2 || '%') LIMIT :limit")
    suspend fun searchByTwoWords(word1: String, word2: String, limit: Int = 5): List<Versiculo>

    @Query("SELECT * FROM versiculos WHERE texto_busqueda LIKE ('%' || :word || '%') LIMIT :limit")
    suspend fun searchByOneWord(word: String, limit: Int = 5): List<Versiculo>

    @Query("SELECT * FROM versiculos WHERE texto_busqueda LIKE (:phrase || '%') LIMIT 5")
    suspend fun searchByInitialPhrase(phrase: String): List<Versiculo>

    @Query("SELECT DISTINCT capitulo FROM versiculos WHERE libro_id = :libroId ORDER BY capitulo ASC")
    suspend fun getCapitulosByLibro(libroId: Int): List<Int>
}