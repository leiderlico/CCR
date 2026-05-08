package com.leiderl.CCR.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.leiderl.CCR.data.database.dao.LibroDao
import com.leiderl.CCR.data.database.dao.VersiculoDao
import com.leiderl.CCR.data.database.dao.VersiculoFamosoDao
import com.leiderl.CCR.data.database.dao.VideoDao
import com.leiderl.CCR.data.database.entities.Libro
import com.leiderl.CCR.data.database.entities.Versiculo
import com.leiderl.CCR.data.database.entities.VersiculoFamoso
import com.leiderl.CCR.data.database.entities.Video

@Database(
    entities = [Libro::class, Versiculo::class, VersiculoFamoso::class, Video::class],
    version = 4,
    exportSchema = false
)
abstract class BibliaDatabase : RoomDatabase() {

    abstract fun libroDao(): LibroDao
    abstract fun versiculoDao(): VersiculoDao
    abstract fun versiculoFamosoDao(): VersiculoFamosoDao
    abstract fun videoDao(): VideoDao

    companion object {
        @Volatile
        private var INSTANCE: BibliaDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS videos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        titulo TEXT NOT NULL,
                        predicador TEXT NOT NULL,
                        grupo TEXT NOT NULL,
                        fecha TEXT NOT NULL,
                        fechaOrden INTEGER NOT NULL,
                        urlYoutube TEXT NOT NULL,
                        libroId INTEGER NOT NULL DEFAULT 0,
                        capitulo INTEGER NOT NULL DEFAULT 0,
                        descripcion TEXT NOT NULL DEFAULT ''
                    )
                """)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS videos_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        titulo TEXT NOT NULL,
                        predicador TEXT NOT NULL,
                        grupo TEXT NOT NULL,
                        fecha TEXT NOT NULL,
                        fechaOrden INTEGER NOT NULL,
                        urlYoutube TEXT NOT NULL,
                        libroId INTEGER NOT NULL DEFAULT 0,
                        capitulo INTEGER NOT NULL DEFAULT 0,
                        versiculo INTEGER NOT NULL DEFAULT 0
                    )
                """)
                database.execSQL("""
                    INSERT INTO videos_new (id, titulo, predicador, grupo, fecha, fechaOrden, urlYoutube, libroId, capitulo, versiculo)
                    SELECT id, titulo, predicador, grupo, fecha, fechaOrden, urlYoutube, libroId, capitulo, 0
                    FROM videos
                """)
                database.execSQL("DROP TABLE videos")
                database.execSQL("ALTER TABLE videos_new RENAME TO videos")
            }
        }

        // Migración 3→4: versiculo de INTEGER a TEXT para soportar rangos como "12-15"
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS videos_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        titulo TEXT NOT NULL,
                        predicador TEXT NOT NULL,
                        grupo TEXT NOT NULL,
                        fecha TEXT NOT NULL,
                        fechaOrden INTEGER NOT NULL,
                        urlYoutube TEXT NOT NULL,
                        libroId INTEGER NOT NULL DEFAULT 0,
                        capitulo INTEGER NOT NULL DEFAULT 0,
                        versiculo TEXT NOT NULL DEFAULT ''
                    )
                """)
                database.execSQL("""
                    INSERT INTO videos_new (id, titulo, predicador, grupo, fecha, fechaOrden, urlYoutube, libroId, capitulo, versiculo)
                    SELECT id, titulo, predicador, grupo, fecha, fechaOrden, urlYoutube, libroId, capitulo,
                           CASE WHEN versiculo = 0 THEN '' ELSE CAST(versiculo AS TEXT) END
                    FROM videos
                """)
                database.execSQL("DROP TABLE videos")
                database.execSQL("ALTER TABLE videos_new RENAME TO videos")
            }
        }

        fun getInstance(context: Context): BibliaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BibliaDatabase::class.java,
                    "biblia_rv1960.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}