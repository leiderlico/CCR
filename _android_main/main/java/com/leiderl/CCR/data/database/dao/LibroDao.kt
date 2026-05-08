package com.leiderl.CCR.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leiderl.CCR.data.database.entities.Libro
import kotlinx.coroutines.flow.Flow

@Dao
interface LibroDao {
    @Query("SELECT * FROM libros ORDER BY orden ASC")
    fun getAllLibros(): Flow<List<Libro>>

    @Query("SELECT * FROM libros ORDER BY orden ASC")
    suspend fun getAllLibrosList(): List<Libro>

    @Query("SELECT * FROM libros WHERE id = :id")
    suspend fun getLibroById(id: Int): Libro?

    @Query("SELECT * FROM libros WHERE testamento = :testamento ORDER BY orden ASC")
    fun getLibrosByTestamento(testamento: String): Flow<List<Libro>>

    @Query("SELECT COUNT(*) FROM libros")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(libros: List<Libro>)

    @Query("SELECT * FROM libros WHERE LOWER(nombre) LIKE LOWER(:query) OR LOWER(abreviacion) LIKE LOWER(:query)")
    suspend fun searchLibros(query: String): List<Libro>
}
