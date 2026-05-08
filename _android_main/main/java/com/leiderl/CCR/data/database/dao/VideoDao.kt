package com.leiderl.CCR.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leiderl.CCR.data.database.entities.Video

@Dao
interface VideoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<Video>)

    @Query("DELETE FROM videos")
    suspend fun deleteAll()

    // Todos los videos ordenados por fecha descendente
    @Query("SELECT * FROM videos ORDER BY fechaOrden DESC")
    suspend fun getAll(): List<Video>

    // Grupos únicos disponibles
    @Query("SELECT DISTINCT grupo FROM videos ORDER BY grupo ASC")
    suspend fun getGrupos(): List<String>

    // Predicadores únicos disponibles
    @Query("SELECT DISTINCT predicador FROM videos ORDER BY predicador ASC")
    suspend fun getPredicadores(): List<String>

    // Videos de un grupo específico
    @Query("SELECT * FROM videos WHERE grupo = :grupo ORDER BY fechaOrden DESC")
    suspend fun getByGrupo(grupo: String): List<Video>

    // Videos de un predicador específico
    @Query("SELECT * FROM videos WHERE predicador = :predicador ORDER BY fechaOrden DESC")
    suspend fun getByPredicador(predicador: String): List<Video>

    // Videos asociados a un libro+capítulo (para el botón en VersiculosFragment)
    @Query("SELECT * FROM videos WHERE libroId = :libroId AND capitulo = :capitulo ORDER BY fechaOrden DESC")
    suspend fun getByLibroCapitulo(libroId: Int, capitulo: Int): List<Video>

    // Videos asociados a un libro (cualquier capítulo)
    @Query("SELECT * FROM videos WHERE libroId = :libroId ORDER BY fechaOrden DESC")
    suspend fun getByLibro(libroId: Int): List<Video>

    // Búsqueda por texto en título, predicador o descripción
    @Query("""
        SELECT * FROM videos 
        WHERE titulo LIKE '%' || :query || '%'
           OR predicador LIKE '%' || :query || '%'
        ORDER BY fechaOrden DESC
    """)
    suspend fun search(query: String): List<Video>

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun count(): Int
}