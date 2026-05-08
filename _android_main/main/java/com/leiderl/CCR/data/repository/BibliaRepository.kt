package com.leiderl.CCR.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.leiderl.CCR.data.database.BibliaDatabase
import com.leiderl.CCR.data.database.entities.Libro
import com.leiderl.CCR.data.database.entities.Versiculo
import com.leiderl.CCR.data.database.entities.VersiculoFamoso
import com.leiderl.CCR.utils.BibliaParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BibliaRepository(private val context: Context) {

    private val db = BibliaDatabase.getInstance(context)
    private val libroDao = db.libroDao()
    private val versiculoDao = db.versiculoDao()
    private val versiculoFamosoDao = db.versiculoFamosoDao()

    // ============ INICIALIZACIÓN ============

    suspend fun isDatabaseInitialized(): Boolean = withContext(Dispatchers.IO) {
        libroDao.count() > 0 && versiculoDao.count() > 0
    }

    suspend fun initializeDatabase() = withContext(Dispatchers.IO) {
        Log.d("BibliaRepo", "Inicializando base de datos...")
        try {
            val json = context.assets.open("biblia_rv1960.json").bufferedReader().use { it.readText() }
            val gson = Gson()
            val type = object : TypeToken<BibliaJsonData>() {}.type
            val data: BibliaJsonData = gson.fromJson(json, type)
            Log.d("BibliaRepo", "JSON cargado: ${data.libros.size} libros, ${data.versiculos.size} versículos")
            val libros = data.libros.map { Libro(it.id, it.nombre, it.abreviacion, it.testamento, it.orden, it.capitulos) }
            libroDao.insertAll(libros)
            Log.d("BibliaRepo", "Libros insertados")
            val versiculos = data.versiculos.map {
                val textoOriginal = it.texto.replace("/n", " ").replace(Regex("  +"), " ").trim()
                Versiculo(
                    id = it.id,
                    libro_id = it.libro_id,
                    capitulo = it.capitulo,
                    versiculo = it.versiculo,
                    texto = textoOriginal,                          // ✅ Original con tildes → mostrar
                    textoBusqueda = BibliaParser.normalizar(textoOriginal) // ✅ Sin tildes → buscar
                )
            }
            versiculos.chunked(500).forEach { chunk ->
                versiculoDao.insertAll(chunk)
            }
            Log.d("BibliaRepo", "Versículos insertados: ${versiculos.size}")
            insertFamosos()
            insertFamososBatch6()
            Log.d("BibliaRepo", "Versículos famosos insertados")
        } catch (e: Exception) {
            Log.e("BibliaRepo", "Error inicializando DB: ${e.message}", e)
            throw e
        }
    }
    private suspend fun insertFamosos() {
        // 742 versículos famosos generados automáticamente desde RV1960
        // Insertados en lotes para no sobrecargar memoria
        val batch1 = listOf(
            VersiculoFamoso(frase_inicial = "porque de tal manera amo dios al", libro_id = 43, capitulo = 3, versiculo = 16, relevancia = 10),
            VersiculoFamoso(frase_inicial = "y sabemos que a los que aman", libro_id = 45, capitulo = 8, versiculo = 28, relevancia = 10),
            VersiculoFamoso(frase_inicial = "todo lo puedo en cristo que me", libro_id = 50, capitulo = 4, versiculo = 13, relevancia = 10),
            VersiculoFamoso(frase_inicial = "jehova es mi pastor nada me faltara", libro_id = 19, capitulo = 23, versiculo = 1, relevancia = 10),
            VersiculoFamoso(frase_inicial = "vosotros pues orareis asi padre nuestro que", libro_id = 40, capitulo = 6, versiculo = 9, relevancia = 10),
            VersiculoFamoso(frase_inicial = "fiate de jehova de todo tu corazon", libro_id = 20, capitulo = 3, versiculo = 5, relevancia = 10),
            VersiculoFamoso(frase_inicial = "porque yo se los pensamientos que tengo", libro_id = 24, capitulo = 29, versiculo = 11, relevancia = 10),
            VersiculoFamoso(frase_inicial = "por tanto id y haced discipulos a", libro_id = 40, capitulo = 28, versiculo = 19, relevancia = 10),
            VersiculoFamoso(frase_inicial = "el amor es sufrido es benigno el", libro_id = 46, capitulo = 13, versiculo = 4, relevancia = 10),
            VersiculoFamoso(frase_inicial = "pero los que esperan a jehova tendran", libro_id = 23, capitulo = 40, versiculo = 31, relevancia = 10),
            VersiculoFamoso(frase_inicial = "mira que te mando que te esfuerces", libro_id = 6, capitulo = 1, versiculo = 9, relevancia = 10),
            VersiculoFamoso(frase_inicial = "el que habita al abrigo del altisimo", libro_id = 19, capitulo = 91, versiculo = 1, relevancia = 10),
            VersiculoFamoso(frase_inicial = "lampara es a mis pies tu palabra", libro_id = 19, capitulo = 119, versiculo = 105, relevancia = 10),
            VersiculoFamoso(frase_inicial = "he aqui yo estoy a la puerta", libro_id = 66, capitulo = 3, versiculo = 20, relevancia = 10),
            VersiculoFamoso(frase_inicial = "jesus lloro", libro_id = 43, capitulo = 11, versiculo = 35, relevancia = 10),
            VersiculoFamoso(frase_inicial = "instruye al nino en su camino ny", libro_id = 20, capitulo = 22, versiculo = 6, relevancia = 9),
            VersiculoFamoso(frase_inicial = "porque por gracia sois salvos por medio", libro_id = 49, capitulo = 2, versiculo = 8, relevancia = 9),
            VersiculoFamoso(frase_inicial = "porque no nos ha dado dios espiritu", libro_id = 55, capitulo = 1, versiculo = 7, relevancia = 9),
            VersiculoFamoso(frase_inicial = "no os conformeis a este siglo sino", libro_id = 45, capitulo = 12, versiculo = 2, relevancia = 9),
            VersiculoFamoso(frase_inicial = "mas buscad primeramente el reino de dios", libro_id = 40, capitulo = 6, versiculo = 33, relevancia = 9),
            VersiculoFamoso(frase_inicial = "es pues la fe la certeza de", libro_id = 58, capitulo = 11, versiculo = 1, relevancia = 9),
            VersiculoFamoso(frase_inicial = "nosotros le amamos a el porque el", libro_id = 62, capitulo = 4, versiculo = 19, relevancia = 9),
            VersiculoFamoso(frase_inicial = "por cuanto todos pecaron y estan destituidos", libro_id = 45, capitulo = 3, versiculo = 23, relevancia = 9),
            VersiculoFamoso(frase_inicial = "porque la paga del pecado es muerte", libro_id = 45, capitulo = 6, versiculo = 23, relevancia = 9),
            VersiculoFamoso(frase_inicial = "jesus le dijo yo soy el camino", libro_id = 43, capitulo = 14, versiculo = 6, relevancia = 9),
            VersiculoFamoso(frase_inicial = "dios es nuestro amparo y fortaleza nnuestro", libro_id = 19, capitulo = 46, versiculo = 1, relevancia = 9),
            VersiculoFamoso(frase_inicial = "venid a mi todos los que estais", libro_id = 40, capitulo = 11, versiculo = 28, relevancia = 9),
            VersiculoFamoso(frase_inicial = "y conocereis la verdad y la verdad", libro_id = 43, capitulo = 8, versiculo = 32, relevancia = 9),
            VersiculoFamoso(frase_inicial = "deleitate asimismo en jehova ny el te", libro_id = 19, capitulo = 37, versiculo = 4, relevancia = 9),
            VersiculoFamoso(frase_inicial = "si confesamos nuestros pecados el es fiel", libro_id = 62, capitulo = 1, versiculo = 9, relevancia = 9),
            VersiculoFamoso(frase_inicial = "jehova es mi luz y mi salvacion", libro_id = 19, capitulo = 27, versiculo = 1, relevancia = 9),
            VersiculoFamoso(frase_inicial = "bienaventurados los pobres en espiritu porque de", libro_id = 40, capitulo = 5, versiculo = 3, relevancia = 9),
            VersiculoFamoso(frase_inicial = "bienaventurados los que lloran porque ellos recibiran", libro_id = 40, capitulo = 5, versiculo = 4, relevancia = 9),
            VersiculoFamoso(frase_inicial = "bienaventurados los mansos porque ellos recibiran la", libro_id = 40, capitulo = 5, versiculo = 5, relevancia = 9),
            VersiculoFamoso(frase_inicial = "bienaventurados los que tienen hambre y sed", libro_id = 40, capitulo = 5, versiculo = 6, relevancia = 9),
            VersiculoFamoso(frase_inicial = "bienaventurados los misericordiosos porque ellos alcanzaran misericordia", libro_id = 40, capitulo = 5, versiculo = 7, relevancia = 9),
            VersiculoFamoso(frase_inicial = "bienaventurados los de limpio corazon porque ellos", libro_id = 40, capitulo = 5, versiculo = 8, relevancia = 9),
            VersiculoFamoso(frase_inicial = "bienaventurados los pacificadores porque ellos seran llamados", libro_id = 40, capitulo = 5, versiculo = 9, relevancia = 9),
            VersiculoFamoso(frase_inicial = "gustad y ved que es bueno jehova", libro_id = 19, capitulo = 34, versiculo = 8, relevancia = 9),
            VersiculoFamoso(frase_inicial = "porque no envio dios a su hijo", libro_id = 43, capitulo = 3, versiculo = 17, relevancia = 9),
            VersiculoFamoso(frase_inicial = "no temas porque yo estoy contigo no", libro_id = 23, capitulo = 41, versiculo = 10, relevancia = 9),
            VersiculoFamoso(frase_inicial = "no se turbe vuestro corazon creeis en", libro_id = 43, capitulo = 14, versiculo = 1, relevancia = 9),
            VersiculoFamoso(frase_inicial = "estas cosas os he hablado para que", libro_id = 43, capitulo = 16, versiculo = 33, relevancia = 9),
            VersiculoFamoso(frase_inicial = "ahora pues ninguna condenacion hay para los", libro_id = 45, capitulo = 8, versiculo = 1, relevancia = 9),
            VersiculoFamoso(frase_inicial = "que si confesares con tu boca que", libro_id = 45, capitulo = 10, versiculo = 9, relevancia = 9),
            VersiculoFamoso(frase_inicial = "ellos dijeron cree en el senor jesucristo", libro_id = 44, capitulo = 16, versiculo = 31, relevancia = 9),
            VersiculoFamoso(frase_inicial = "nadie tiene mayor amor que este que", libro_id = 43, capitulo = 15, versiculo = 13, relevancia = 9),
            VersiculoFamoso(frase_inicial = "jesus le dijo amaras al senor tu", libro_id = 40, capitulo = 22, versiculo = 37, relevancia = 9),
            VersiculoFamoso(frase_inicial = "este es el dia que hizo jehova", libro_id = 19, capitulo = 118, versiculo = 24, relevancia = 9),
            VersiculoFamoso(frase_inicial = "mas el fruto del espiritu es amor", libro_id = 48, capitulo = 5, versiculo = 22, relevancia = 9),
            VersiculoFamoso(frase_inicial = "en el principio creo dios los cielos", libro_id = 1, capitulo = 1, versiculo = 1, relevancia = 8),
            VersiculoFamoso(frase_inicial = "bienaventurado el varon que no anduvo en", libro_id = 19, capitulo = 1, versiculo = 1, relevancia = 8),
            VersiculoFamoso(frase_inicial = "cantad alegres a dios habitantes de toda", libro_id = 19, capitulo = 100, versiculo = 1, relevancia = 8),
            VersiculoFamoso(frase_inicial = "alzare mis ojos a los montes n", libro_id = 19, capitulo = 121, versiculo = 1, relevancia = 8),
            VersiculoFamoso(frase_inicial = "te alabare porque formidables maravillosas son tus", libro_id = 19, capitulo = 139, versiculo = 14, relevancia = 8),
            VersiculoFamoso(frase_inicial = "sobre toda cosa guardada guarda tu corazon", libro_id = 20, capitulo = 4, versiculo = 23, relevancia = 8),
            VersiculoFamoso(frase_inicial = "torre fuerte es el nombre de jehova", libro_id = 20, capitulo = 18, versiculo = 10, relevancia = 8),
            VersiculoFamoso(frase_inicial = "mas el herido fue por nuestras rebeliones", libro_id = 23, capitulo = 53, versiculo = 5, relevancia = 8),
            VersiculoFamoso(frase_inicial = "saname oh jehova y sere sano salvame", libro_id = 24, capitulo = 17, versiculo = 14, relevancia = 8),
            VersiculoFamoso(frase_inicial = "pero yo os digo amad a vuestros", libro_id = 40, capitulo = 5, versiculo = 44, relevancia = 8),
            VersiculoFamoso(frase_inicial = "no os hagais tesoros en la tierra", libro_id = 40, capitulo = 6, versiculo = 19, relevancia = 8),
            VersiculoFamoso(frase_inicial = "pedid y se os dara buscad y", libro_id = 40, capitulo = 7, versiculo = 7, relevancia = 8),
            VersiculoFamoso(frase_inicial = "y el segundo es semejante amaras a", libro_id = 40, capitulo = 22, versiculo = 39, relevancia = 8),
            VersiculoFamoso(frase_inicial = "en el principio era el verbo y", libro_id = 43, capitulo = 1, versiculo = 1, relevancia = 8),
            VersiculoFamoso(frase_inicial = "el que cree en el hijo tiene", libro_id = 43, capitulo = 3, versiculo = 36, relevancia = 8),
            VersiculoFamoso(frase_inicial = "el ladron no viene sino para hurtar", libro_id = 43, capitulo = 10, versiculo = 10, relevancia = 8),
            VersiculoFamoso(frase_inicial = "yo soy la vid vosotros los pampanos", libro_id = 43, capitulo = 15, versiculo = 5, relevancia = 8),
            VersiculoFamoso(frase_inicial = "pero recibireis poder cuando haya venido sobre", libro_id = 44, capitulo = 1, versiculo = 8, relevancia = 8),
            VersiculoFamoso(frase_inicial = "pedro les dijo arrepentios y bauticese cada", libro_id = 44, capitulo = 2, versiculo = 38, relevancia = 8),
            VersiculoFamoso(frase_inicial = "porque no me averguenzo del evangelio porque", libro_id = 45, capitulo = 1, versiculo = 16, relevancia = 8),
            VersiculoFamoso(frase_inicial = "mas dios muestra su amor para con", libro_id = 45, capitulo = 5, versiculo = 8, relevancia = 8),
            VersiculoFamoso(frase_inicial = "asi que hermanos os ruego por las", libro_id = 45, capitulo = 12, versiculo = 1, relevancia = 8),
            VersiculoFamoso(frase_inicial = "no os ha sobrevenido ninguna tentacion que", libro_id = 46, capitulo = 10, versiculo = 13, relevancia = 8),
            VersiculoFamoso(frase_inicial = "y ahora permanecen la fe la esperanza", libro_id = 46, capitulo = 13, versiculo = 13, relevancia = 8),
            VersiculoFamoso(frase_inicial = "de modo que si alguno esta en", libro_id = 47, capitulo = 5, versiculo = 17, relevancia = 8),
            VersiculoFamoso(frase_inicial = "y me ha dicho bastate mi gracia", libro_id = 47, capitulo = 12, versiculo = 9, relevancia = 8),
            VersiculoFamoso(frase_inicial = "con cristo estoy juntamente crucificado y ya", libro_id = 48, capitulo = 2, versiculo = 20, relevancia = 8),
            VersiculoFamoso(frase_inicial = "y a aquel que es poderoso para", libro_id = 49, capitulo = 3, versiculo = 20, relevancia = 8),
            VersiculoFamoso(frase_inicial = "un dios y padre de todos el", libro_id = 49, capitulo = 4, versiculo = 6, relevancia = 8),
            VersiculoFamoso(frase_inicial = "por lo demas hermanos mios fortaleceos en", libro_id = 49, capitulo = 6, versiculo = 10, relevancia = 8),
            VersiculoFamoso(frase_inicial = "vestios de toda la armadura de dios", libro_id = 49, capitulo = 6, versiculo = 11, relevancia = 8),
            VersiculoFamoso(frase_inicial = "nada hagais por contienda o por vanagloria", libro_id = 50, capitulo = 2, versiculo = 3, relevancia = 8),
            VersiculoFamoso(frase_inicial = "regocijaos en el senor siempre otra vez", libro_id = 50, capitulo = 4, versiculo = 4, relevancia = 8),
            VersiculoFamoso(frase_inicial = "por nada esteis afanosos sino sean conocidas", libro_id = 50, capitulo = 4, versiculo = 6, relevancia = 8),
            VersiculoFamoso(frase_inicial = "y la paz de dios que sobrepasa", libro_id = 50, capitulo = 4, versiculo = 7, relevancia = 8),
            VersiculoFamoso(frase_inicial = "y todo lo que hagais hacedlo de", libro_id = 51, capitulo = 3, versiculo = 23, relevancia = 8),
            VersiculoFamoso(frase_inicial = "pelea la buena batalla de la fe", libro_id = 54, capitulo = 6, versiculo = 12, relevancia = 8),
            VersiculoFamoso(frase_inicial = "toda la escritura es inspirada por dios", libro_id = 55, capitulo = 3, versiculo = 16, relevancia = 8),
            VersiculoFamoso(frase_inicial = "porque la palabra de dios es viva", libro_id = 58, capitulo = 4, versiculo = 12, relevancia = 8),
            VersiculoFamoso(frase_inicial = "jesucristo es el mismo ayer y hoy", libro_id = 58, capitulo = 13, versiculo = 8, relevancia = 8),
            VersiculoFamoso(frase_inicial = "y si alguno de vosotros tiene falta", libro_id = 59, capitulo = 1, versiculo = 5, relevancia = 8),
            VersiculoFamoso(frase_inicial = "someteos pues a dios resistid al diablo", libro_id = 59, capitulo = 4, versiculo = 7, relevancia = 8),
            VersiculoFamoso(frase_inicial = "echando toda vuestra ansiedad sobre el porque", libro_id = 60, capitulo = 5, versiculo = 7, relevancia = 8),
            VersiculoFamoso(frase_inicial = "el que no ama no ha conocido", libro_id = 62, capitulo = 4, versiculo = 8, relevancia = 8),
            VersiculoFamoso(frase_inicial = "y esta es la confianza que tenemos", libro_id = 62, capitulo = 5, versiculo = 14, relevancia = 8),
            VersiculoFamoso(frase_inicial = "enjugara dios toda lagrima de los ojos", libro_id = 66, capitulo = 21, versiculo = 4, relevancia = 8),
            VersiculoFamoso(frase_inicial = "jesus les dijo por vuestra poca fe", libro_id = 40, capitulo = 17, versiculo = 20, relevancia = 8),
            VersiculoFamoso(frase_inicial = "yo soy el buen pastor el buen", libro_id = 43, capitulo = 10, versiculo = 11, relevancia = 8),
            VersiculoFamoso(frase_inicial = "un mandamiento nuevo os doy que os", libro_id = 43, capitulo = 13, versiculo = 34, relevancia = 8),
            VersiculoFamoso(frase_inicial = "en esto conoceran todos que sois mis", libro_id = 43, capitulo = 13, versiculo = 35, relevancia = 8),
            VersiculoFamoso(frase_inicial = "en la casa de mi padre muchas", libro_id = 43, capitulo = 14, versiculo = 2, relevancia = 8),
            VersiculoFamoso(frase_inicial = "y si me fuere y os preparare", libro_id = 43, capitulo = 14, versiculo = 3, relevancia = 8),
            VersiculoFamoso(frase_inicial = "la paz os dejo mi paz os", libro_id = 43, capitulo = 14, versiculo = 27, relevancia = 8),
            VersiculoFamoso(frase_inicial = "y en ningun otro hay salvacion porque", libro_id = 44, capitulo = 4, versiculo = 12, relevancia = 8),
            VersiculoFamoso(frase_inicial = "que pues diremos a esto si dios", libro_id = 45, capitulo = 8, versiculo = 31, relevancia = 8),
            VersiculoFamoso(frase_inicial = "antes en todas estas cosas somos mas", libro_id = 45, capitulo = 8, versiculo = 37, relevancia = 8),
            VersiculoFamoso(frase_inicial = "por lo cual estoy seguro de que", libro_id = 45, capitulo = 8, versiculo = 38, relevancia = 8),
            VersiculoFamoso(frase_inicial = "ni lo alto ni lo profundo ni", libro_id = 45, capitulo = 8, versiculo = 39, relevancia = 8),
            VersiculoFamoso(frase_inicial = "sobrellevad los unos las cargas de los", libro_id = 48, capitulo = 6, versiculo = 2, relevancia = 8),
            VersiculoFamoso(frase_inicial = "no os enganeis dios no puede ser", libro_id = 48, capitulo = 6, versiculo = 7, relevancia = 8),
            VersiculoFamoso(frase_inicial = "porque el que siembra para su carne", libro_id = 48, capitulo = 6, versiculo = 8, relevancia = 8),
            VersiculoFamoso(frase_inicial = "porque somos hechura suya creados en cristo", libro_id = 49, capitulo = 2, versiculo = 10, relevancia = 8),
            VersiculoFamoso(frase_inicial = "estando persuadido de esto que el que", libro_id = 50, capitulo = 1, versiculo = 6, relevancia = 8),
            VersiculoFamoso(frase_inicial = "la palabra de cristo more en abundancia", libro_id = 51, capitulo = 3, versiculo = 16, relevancia = 8),
            VersiculoFamoso(frase_inicial = "y todo lo que haceis sea de", libro_id = 51, capitulo = 3, versiculo = 17, relevancia = 8),
            VersiculoFamoso(frase_inicial = "estad siempre gozosos", libro_id = 52, capitulo = 5, versiculo = 16, relevancia = 8),
            VersiculoFamoso(frase_inicial = "orad sin cesar", libro_id = 52, capitulo = 5, versiculo = 17, relevancia = 8),
            VersiculoFamoso(frase_inicial = "dad gracias en todo porque esta es", libro_id = 52, capitulo = 5, versiculo = 18, relevancia = 8),
            VersiculoFamoso(frase_inicial = "pero sin fe es imposible agradar a", libro_id = 58, capitulo = 11, versiculo = 6, relevancia = 8),
            VersiculoFamoso(frase_inicial = "por tanto nosotros tambien teniendo en derredor", libro_id = 58, capitulo = 12, versiculo = 1, relevancia = 8),
            VersiculoFamoso(frase_inicial = "puestos los ojos en jesus el autor", libro_id = 58, capitulo = 12, versiculo = 2, relevancia = 8),
            VersiculoFamoso(frase_inicial = "hermanos mios tened por sumo gozo cuando", libro_id = 59, capitulo = 1, versiculo = 2, relevancia = 8),
            VersiculoFamoso(frase_inicial = "sabiendo que la prueba de vuestra fe", libro_id = 59, capitulo = 1, versiculo = 3, relevancia = 8),
            VersiculoFamoso(frase_inicial = "mas vosotros sois linaje escogido real sacerdocio", libro_id = 60, capitulo = 2, versiculo = 9, relevancia = 8),
            VersiculoFamoso(frase_inicial = "en esto hemos conocido el amor en", libro_id = 62, capitulo = 3, versiculo = 16, relevancia = 8),
            VersiculoFamoso(frase_inicial = "hijitos vosotros sois de dios y los", libro_id = 62, capitulo = 4, versiculo = 4, relevancia = 8),
            VersiculoFamoso(frase_inicial = "yo soy el alfa y la omega", libro_id = 66, capitulo = 1, versiculo = 8, relevancia = 8),
            VersiculoFamoso(frase_inicial = "y el espiritu y la esposa dicen", libro_id = 66, capitulo = 22, versiculo = 17, relevancia = 8),
            VersiculoFamoso(frase_inicial = "bendice alma mia a jehova ny bendiga", libro_id = 19, capitulo = 103, versiculo = 1, relevancia = 8),
            VersiculoFamoso(frase_inicial = "crea en mi oh dios un corazon", libro_id = 19, capitulo = 51, versiculo = 10, relevancia = 8),
            VersiculoFamoso(frase_inicial = "por que se amotinan las gentes", libro_id = 19, capitulo = 2, versiculo = 1, relevancia = 7),
            VersiculoFamoso(frase_inicial = "a jehova he puesto siempre delante de", libro_id = 19, capitulo = 16, versiculo = 8, relevancia = 7),
            VersiculoFamoso(frase_inicial = "jehova roca mia y castillo mio y", libro_id = 19, capitulo = 18, versiculo = 2, relevancia = 7),
            VersiculoFamoso(frase_inicial = "sean gratos los dichos de mi boca", libro_id = 19, capitulo = 19, versiculo = 14, relevancia = 7),
            VersiculoFamoso(frase_inicial = "dios mio dios mio por que me", libro_id = 19, capitulo = 22, versiculo = 1, relevancia = 7),
            VersiculoFamoso(frase_inicial = "aunque ande en valle de sombra de", libro_id = 19, capitulo = 23, versiculo = 4, relevancia = 7),
            VersiculoFamoso(frase_inicial = "ciertamente el bien y la misericordia me", libro_id = 19, capitulo = 23, versiculo = 6, relevancia = 7),
            VersiculoFamoso(frase_inicial = "te hare entender y te ensenare el", libro_id = 19, capitulo = 32, versiculo = 8, relevancia = 7),
            VersiculoFamoso(frase_inicial = "echa sobre jehova tu carga y el", libro_id = 19, capitulo = 55, versiculo = 22, relevancia = 7),
            VersiculoFamoso(frase_inicial = "en dios solamente esta acallada mi alma", libro_id = 19, capitulo = 62, versiculo = 1, relevancia = 7),
            VersiculoFamoso(frase_inicial = "senor tu nos has sido refugio nde", libro_id = 19, capitulo = 90, versiculo = 1, relevancia = 7),
            VersiculoFamoso(frase_inicial = "alabad a jehova porque el es bueno", libro_id = 19, capitulo = 107, versiculo = 1, relevancia = 7),
            VersiculoFamoso(frase_inicial = "alabad a jehova porque el es bueno", libro_id = 19, capitulo = 136, versiculo = 1, relevancia = 7),
            VersiculoFamoso(frase_inicial = "el principio de la sabiduria es el", libro_id = 20, capitulo = 1, versiculo = 7, relevancia = 7),
            VersiculoFamoso(frase_inicial = "reconocelo en todos tus caminos ny el", libro_id = 20, capitulo = 3, versiculo = 6, relevancia = 7),
            VersiculoFamoso(frase_inicial = "cuando viene la soberbia viene tambien la", libro_id = 20, capitulo = 11, versiculo = 2, relevancia = 7),
            VersiculoFamoso(frase_inicial = "hay camino que al hombre le parece", libro_id = 20, capitulo = 14, versiculo = 12, relevancia = 7),
            VersiculoFamoso(frase_inicial = "antes del quebrantamiento es la soberbia ny", libro_id = 20, capitulo = 16, versiculo = 18, relevancia = 7),
            VersiculoFamoso(frase_inicial = "en todo tiempo ama el amigo ny", libro_id = 20, capitulo = 17, versiculo = 17, relevancia = 7),
            VersiculoFamoso(frase_inicial = "no te jactes del dia de manana", libro_id = 20, capitulo = 27, versiculo = 1, relevancia = 7),
        )
        versiculoFamosoDao.insertAll(batch1)

        val batch2 = listOf(
            VersiculoFamoso(frase_inicial = "huye el impio sin que nadie lo", libro_id = 20, capitulo = 28, versiculo = 1, relevancia = 7),
            VersiculoFamoso(frase_inicial = "el necio da rienda suelta a toda", libro_id = 20, capitulo = 29, versiculo = 11, relevancia = 7),
            VersiculoFamoso(frase_inicial = "porque un nino nos es nacido hijo", libro_id = 23, capitulo = 9, versiculo = 6, relevancia = 7),
            VersiculoFamoso(frase_inicial = "tu guardaras en completa paz a aquel", libro_id = 23, capitulo = 26, versiculo = 3, relevancia = 7),
            VersiculoFamoso(frase_inicial = "no has sabido no has oido que", libro_id = 23, capitulo = 40, versiculo = 28, relevancia = 7),
            VersiculoFamoso(frase_inicial = "el da esfuerzo al cansado y multiplica", libro_id = 23, capitulo = 40, versiculo = 29, relevancia = 7),
            VersiculoFamoso(frase_inicial = "ahora asi dice jehova creador tuyo oh", libro_id = 23, capitulo = 43, versiculo = 1, relevancia = 7),
            VersiculoFamoso(frase_inicial = "cuando pases por las aguas yo estare", libro_id = 23, capitulo = 43, versiculo = 2, relevancia = 7),
            VersiculoFamoso(frase_inicial = "porque mis pensamientos no son vuestros pensamientos", libro_id = 23, capitulo = 55, versiculo = 8, relevancia = 7),
            VersiculoFamoso(frase_inicial = "como son mas altos los cielos que", libro_id = 23, capitulo = 55, versiculo = 9, relevancia = 7),
            VersiculoFamoso(frase_inicial = "el respondio y dijo escrito esta no", libro_id = 40, capitulo = 4, versiculo = 4, relevancia = 7),
            VersiculoFamoso(frase_inicial = "vosotros sois la sal de la tierra", libro_id = 40, capitulo = 5, versiculo = 13, relevancia = 7),
            VersiculoFamoso(frase_inicial = "vosotros sois la luz del mundo una", libro_id = 40, capitulo = 5, versiculo = 14, relevancia = 7),
            VersiculoFamoso(frase_inicial = "asi alumbre vuestra luz delante de los", libro_id = 40, capitulo = 5, versiculo = 16, relevancia = 7),
            VersiculoFamoso(frase_inicial = "por tanto os digo no os afaneis", libro_id = 40, capitulo = 6, versiculo = 25, relevancia = 7),
            VersiculoFamoso(frase_inicial = "asi que no os afaneis por el", libro_id = 40, capitulo = 6, versiculo = 34, relevancia = 7),
            VersiculoFamoso(frase_inicial = "asi que todas las cosas que querais", libro_id = 40, capitulo = 7, versiculo = 12, relevancia = 7),
            VersiculoFamoso(frase_inicial = "y mirandolos jesus les dijo para los", libro_id = 40, capitulo = 19, versiculo = 26, relevancia = 7),
            VersiculoFamoso(frase_inicial = "y su senor le dijo bien buen", libro_id = 40, capitulo = 25, versiculo = 21, relevancia = 7),
            VersiculoFamoso(frase_inicial = "ensenandoles que guarden todas las cosas que", libro_id = 40, capitulo = 28, versiculo = 20, relevancia = 7),
            VersiculoFamoso(frase_inicial = "por tanto os digo que todo lo", libro_id = 41, capitulo = 11, versiculo = 24, relevancia = 7),
            VersiculoFamoso(frase_inicial = "porque nada hay imposible para dios", libro_id = 42, capitulo = 1, versiculo = 37, relevancia = 7),
            VersiculoFamoso(frase_inicial = "dad y se os dara medida buena", libro_id = 42, capitulo = 6, versiculo = 38, relevancia = 7),
            VersiculoFamoso(frase_inicial = "y yo os digo pedid y se", libro_id = 42, capitulo = 11, versiculo = 9, relevancia = 7),
            VersiculoFamoso(frase_inicial = "respondio jesus y le dijo de cierto", libro_id = 43, capitulo = 3, versiculo = 3, relevancia = 7),
            VersiculoFamoso(frase_inicial = "de cierto de cierto os digo el", libro_id = 43, capitulo = 5, versiculo = 24, relevancia = 7),
            VersiculoFamoso(frase_inicial = "jesus les dijo yo soy el pan", libro_id = 43, capitulo = 6, versiculo = 35, relevancia = 7),
            VersiculoFamoso(frase_inicial = "todo lo que el padre me da", libro_id = 43, capitulo = 6, versiculo = 37, relevancia = 7),
            VersiculoFamoso(frase_inicial = "de cierto de cierto os digo el", libro_id = 43, capitulo = 6, versiculo = 47, relevancia = 7),
            VersiculoFamoso(frase_inicial = "yo soy el pan de vida", libro_id = 43, capitulo = 6, versiculo = 48, relevancia = 7),
            VersiculoFamoso(frase_inicial = "otra vez jesus les hablo diciendo yo", libro_id = 43, capitulo = 8, versiculo = 12, relevancia = 7),
            VersiculoFamoso(frase_inicial = "mis ovejas oyen mi voz y yo", libro_id = 43, capitulo = 10, versiculo = 27, relevancia = 7),
            VersiculoFamoso(frase_inicial = "y yo les doy vida eterna y", libro_id = 43, capitulo = 10, versiculo = 28, relevancia = 7),
            VersiculoFamoso(frase_inicial = "le dijo jesus yo soy la resurreccion", libro_id = 43, capitulo = 11, versiculo = 25, relevancia = 7),
            VersiculoFamoso(frase_inicial = "y todo lo que pidiereis al padre", libro_id = 43, capitulo = 14, versiculo = 13, relevancia = 7),
            VersiculoFamoso(frase_inicial = "si algo pidiereis en mi nombre yo", libro_id = 43, capitulo = 14, versiculo = 14, relevancia = 7),
            VersiculoFamoso(frase_inicial = "si me amais guardad mis mandamientos", libro_id = 43, capitulo = 14, versiculo = 15, relevancia = 7),
            VersiculoFamoso(frase_inicial = "y yo rogare al padre y os", libro_id = 43, capitulo = 14, versiculo = 16, relevancia = 7),
            VersiculoFamoso(frase_inicial = "el que no escatimo ni a su", libro_id = 45, capitulo = 8, versiculo = 32, relevancia = 7),
            VersiculoFamoso(frase_inicial = "antes bien como esta escrito cosas que", libro_id = 46, capitulo = 2, versiculo = 9, relevancia = 7),
            VersiculoFamoso(frase_inicial = "porque esta leve tribulacion momentanea produce en", libro_id = 47, capitulo = 4, versiculo = 17, relevancia = 7),
            VersiculoFamoso(frase_inicial = "no mirando nosotros las cosas que se", libro_id = 47, capitulo = 4, versiculo = 18, relevancia = 7),
            VersiculoFamoso(frase_inicial = "gracia y paz sean a vosotros de", libro_id = 48, capitulo = 1, versiculo = 3, relevancia = 7),
            VersiculoFamoso(frase_inicial = "prosigo a la meta al premio del", libro_id = 50, capitulo = 3, versiculo = 14, relevancia = 7),
            VersiculoFamoso(frase_inicial = "pero se salvara engendrando hijos si permaneciere", libro_id = 54, capitulo = 2, versiculo = 15, relevancia = 7),
            VersiculoFamoso(frase_inicial = "porque la gracia de dios se ha", libro_id = 56, capitulo = 2, versiculo = 11, relevancia = 7),
            VersiculoFamoso(frase_inicial = "ensenandonos que renunciando a la impiedad y", libro_id = 56, capitulo = 2, versiculo = 12, relevancia = 7),
            VersiculoFamoso(frase_inicial = "para que sometida a prueba vuestra fe", libro_id = 60, capitulo = 1, versiculo = 7, relevancia = 7),
            VersiculoFamoso(frase_inicial = "lo que era desde el principio lo", libro_id = 62, capitulo = 1, versiculo = 1, relevancia = 7),
            VersiculoFamoso(frase_inicial = "y los cuatro seres vivientes tenian cada", libro_id = 66, capitulo = 4, versiculo = 8, relevancia = 7),
            VersiculoFamoso(frase_inicial = "despues de esto mire y he aqui", libro_id = 66, capitulo = 7, versiculo = 9, relevancia = 7),
            VersiculoFamoso(frase_inicial = "porque el cordero que esta en medio", libro_id = 66, capitulo = 7, versiculo = 17, relevancia = 7),
            VersiculoFamoso(frase_inicial = "y la tierra estaba desordenada y vacia", libro_id = 1, capitulo = 1, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y creo dios al hombre a su", libro_id = 1, capitulo = 1, versiculo = 27, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por tanto dejara el hombre a su", libro_id = 1, capitulo = 2, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y dijo jehova dios no es bueno", libro_id = 1, capitulo = 2, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "entonces isaac llamo a jacob y lo", libro_id = 1, capitulo = 28, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y hablo dios todas estas palabras diciendo", libro_id = 2, capitulo = 20, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no tendras dioses ajenos delante de mi", libro_id = 2, capitulo = 20, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "honra a tu padre y a tu", libro_id = 2, capitulo = 20, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no mataras", libro_id = 2, capitulo = 20, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no cometeras adulterio", libro_id = 2, capitulo = 20, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no hurtaras", libro_id = 2, capitulo = 20, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no hablaras contra tu projimo falso testimonio", libro_id = 2, capitulo = 20, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no codiciaras la casa de tu projimo", libro_id = 2, capitulo = 20, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "oye israel jehova nuestro dios jehova uno", libro_id = 5, capitulo = 6, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y amaras a jehova tu dios de", libro_id = 5, capitulo = 6, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y te afligio y te hizo tener", libro_id = 5, capitulo = 8, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "esforzaos y cobrad animo no temais ni", libro_id = 5, capitulo = 31, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y si mal os parece servir a", libro_id = 6, capitulo = 24, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y dijo desnudo sali del vientre de", libro_id = 18, capitulo = 1, versiculo = 21, relevancia = 6),
            VersiculoFamoso(frase_inicial = "donde estabas tu cuando yo fundaba la", libro_id = 18, capitulo = 38, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y bendijo jehova el postrer estado de", libro_id = 18, capitulo = 42, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "ncuando veo tus cielos obra de tus", libro_id = 19, capitulo = 8, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "digo que es el hombre para que", libro_id = 19, capitulo = 8, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "n oh jehova senor nuestro ncuan grande", libro_id = 19, capitulo = 8, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "dice el necio en su corazon nno", libro_id = 19, capitulo = 14, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "los cielos cuentan la gloria de dios", libro_id = 19, capitulo = 19, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "de jehova es la tierra y su", libro_id = 19, capitulo = 24, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "quien subira al monte de jehova", libro_id = 19, capitulo = 24, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "nen tu mano estan mis tiempos nlibrame", libro_id = 19, capitulo = 31, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque contigo esta el manantial de la", libro_id = 19, capitulo = 36, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pacientemente espere a jehova ny se inclino", libro_id = 19, capitulo = 40, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "puso luego en mi boca cantico nuevo", libro_id = 19, capitulo = 40, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "como el ciervo brama por las corrientes", libro_id = 19, capitulo = 42, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por que te abates oh alma", libro_id = 19, capitulo = 42, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "envia tu luz y tu verdad estas", libro_id = 19, capitulo = 43, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "estad quietos y conoced que yo soy", libro_id = 19, capitulo = 46, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "dios dios mio eres tu nde madrugada", libro_id = 19, capitulo = 63, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque mejor es tu misericordia que la", libro_id = 19, capitulo = 63, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cuan amables son tus moradas oh", libro_id = 19, capitulo = 84, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque mejor es un dia en tus", libro_id = 19, capitulo = 84, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque tu senor eres bueno y perdonador", libro_id = 19, capitulo = 86, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mas tu senor dios misericordioso y clemente", libro_id = 19, capitulo = 86, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "venid aclamemos alegremente a jehova ncantemos con", libro_id = 19, capitulo = 95, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cantad a jehova cantico nuevo ncantad a", libro_id = 19, capitulo = 96, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "jehova reina regocijese la tierra nalegrense las", libro_id = 19, capitulo = 97, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cantad a jehova cantico nuevo nporque ha", libro_id = 19, capitulo = 98, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "oh jehova tu me has examinado y", libro_id = 19, capitulo = 139, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "examiname oh dios y conoce mi corazon", libro_id = 19, capitulo = 139, versiculo = 23, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y ve si hay en mi camino", libro_id = 19, capitulo = 139, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "grande es jehova y digno de suprema", libro_id = 19, capitulo = 145, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el sana a los quebrantados de corazon", libro_id = 19, capitulo = 147, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque el mandamiento es lampara y la", libro_id = 20, capitulo = 6, versiculo = 23, relevancia = 6),
            VersiculoFamoso(frase_inicial = "la bendicion de jehova es la que", libro_id = 20, capitulo = 10, versiculo = 22, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el que ama la instruccion ama la", libro_id = 20, capitulo = 12, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "la esperanza que se demora es tormento", libro_id = 20, capitulo = 13, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "la blanda respuesta quita la ira nmas", libro_id = 20, capitulo = 15, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "los ojos de jehova estan en todo", libro_id = 20, capitulo = 15, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el corazon del hombre piensa su camino", libro_id = 20, capitulo = 16, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "camina en su integridad el justo nsus", libro_id = 20, capitulo = 20, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "aplica tu corazon a la ensenanza ny", libro_id = 20, capitulo = 23, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque siete veces cae el justo y", libro_id = 20, capitulo = 24, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "fuerza y honor son su vestidura ny", libro_id = 20, capitulo = 31, versiculo = 25, relevancia = 6),
            VersiculoFamoso(frase_inicial = "enganosa es la gracia y vana la", libro_id = 20, capitulo = 31, versiculo = 30, relevancia = 6),
            VersiculoFamoso(frase_inicial = "todo tiene su tiempo y todo lo", libro_id = 21, capitulo = 3, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "todo lo hizo hermoso en su tiempo", libro_id = 21, capitulo = 3, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "he entendido que todo lo que dios", libro_id = 21, capitulo = 3, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el fin de todo el discurso oido", libro_id = 21, capitulo = 12, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque dios traera toda obra a juicio", libro_id = 21, capitulo = 12, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "venid luego dice jehova y estemos a", libro_id = 23, capitulo = 1, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el uno al otro daba voces", libro_id = 23, capitulo = 6, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "he aqui dios es salvacion mia me", libro_id = 23, capitulo = 12, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "consolaos consolaos pueblo mio dice vuestro dios", libro_id = 23, capitulo = 40, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mirad a mi y sed salvos todos", libro_id = 23, capitulo = 45, versiculo = 22, relevancia = 6),
            VersiculoFamoso(frase_inicial = "se olvidara la mujer de lo que", libro_id = 23, capitulo = 49, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "he aqui que en las palmas de", libro_id = 23, capitulo = 49, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "ninguna arma forjada contra ti prosperara y", libro_id = 23, capitulo = 54, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "asi sera mi palabra que sale de", libro_id = 23, capitulo = 55, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no es mas bien el ayuno que", libro_id = 23, capitulo = 58, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no es que partas tu pan con", libro_id = 23, capitulo = 58, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el espiritu de jehova el senor esta", libro_id = 23, capitulo = 61, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "a proclamar el ano de la buena", libro_id = 23, capitulo = 61, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "a ordenar que a los afligidos de", libro_id = 23, capitulo = 61, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "ahora pues jehova tu eres nuestro padre", libro_id = 23, capitulo = 64, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y antes que clamen respondere yo mientras", libro_id = 23, capitulo = 65, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "jehova se manifesto a mi hace ya", libro_id = 24, capitulo = 31, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "clama a mi y yo te respondere", libro_id = 24, capitulo = 33, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y daniel propuso en su corazon no", libro_id = 27, capitulo = 1, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "he aqui nuestro dios a quien servimos", libro_id = 27, capitulo = 3, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y si no sepas oh rey que", libro_id = 27, capitulo = 3, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mi dios envio su angel el cual", libro_id = 27, capitulo = 6, versiculo = 22, relevancia = 6),
            VersiculoFamoso(frase_inicial = "traed todos los diezmos al alfoli y", libro_id = 39, capitulo = 3, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no penseis que he venido para abrogar", libro_id = 40, capitulo = 5, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque de cierto os digo que hasta", libro_id = 40, capitulo = 5, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "de manera que cualquiera que quebrante uno", libro_id = 40, capitulo = 5, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque os digo que si vuestra justicia", libro_id = 40, capitulo = 5, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "oisteis que fue dicho ojo por ojo", libro_id = 40, capitulo = 5, versiculo = 38, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero yo os digo no resistais al", libro_id = 40, capitulo = 5, versiculo = 39, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y al que quiera ponerte a pleito", libro_id = 40, capitulo = 5, versiculo = 40, relevancia = 6),
        )
        versiculoFamosoDao.insertAll(batch2)

        val batch3 = listOf(
            VersiculoFamoso(frase_inicial = "y a cualquiera que te obligue a", libro_id = 40, capitulo = 5, versiculo = 41, relevancia = 6),
            VersiculoFamoso(frase_inicial = "al que te pida dale y al", libro_id = 40, capitulo = 5, versiculo = 42, relevancia = 6),
            VersiculoFamoso(frase_inicial = "oisteis que fue dicho amaras a tu", libro_id = 40, capitulo = 5, versiculo = 43, relevancia = 6),
            VersiculoFamoso(frase_inicial = "para que seais hijos de vuestro padre", libro_id = 40, capitulo = 5, versiculo = 45, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque si amais a los que os", libro_id = 40, capitulo = 5, versiculo = 46, relevancia = 6),
            VersiculoFamoso(frase_inicial = "guardaos de hacer vuestra justicia delante de", libro_id = 40, capitulo = 6, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cuando pues des limosna no hagas tocar", libro_id = 40, capitulo = 6, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mas cuando tu des limosna no sepa", libro_id = 40, capitulo = 6, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "para que sea tu limosna en secreto", libro_id = 40, capitulo = 6, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y cuando ores no seas como los", libro_id = 40, capitulo = 6, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mas tu cuando ores entra en tu", libro_id = 40, capitulo = 6, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y orando no useis vanas repeticiones como", libro_id = 40, capitulo = 6, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no os hagais pues semejantes a ellos", libro_id = 40, capitulo = 6, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "venga tu reino hagase tu voluntad como", libro_id = 40, capitulo = 6, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el pan nuestro de cada dia danoslo", libro_id = 40, capitulo = 6, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y perdonanos nuestras deudas como tambien nosotros", libro_id = 40, capitulo = 6, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y no nos metas en tentacion mas", libro_id = 40, capitulo = 6, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque si perdonais a los hombres sus", libro_id = 40, capitulo = 6, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mas si no perdonais a los hombres", libro_id = 40, capitulo = 6, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cuando ayuneis no seais austeros como los", libro_id = 40, capitulo = 6, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero tu cuando ayunes unge tu cabeza", libro_id = 40, capitulo = 6, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "para no mostrar a los hombres que", libro_id = 40, capitulo = 6, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "sino haceos tesoros en el cielo donde", libro_id = 40, capitulo = 6, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque donde este vuestro tesoro alli estara", libro_id = 40, capitulo = 6, versiculo = 21, relevancia = 6),
            VersiculoFamoso(frase_inicial = "la lampara del cuerpo es el ojo", libro_id = 40, capitulo = 6, versiculo = 22, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero si tu ojo es maligno todo", libro_id = 40, capitulo = 6, versiculo = 23, relevancia = 6),
            VersiculoFamoso(frase_inicial = "ninguno puede servir a dos senores porque", libro_id = 40, capitulo = 6, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mirad las aves del cielo que no", libro_id = 40, capitulo = 6, versiculo = 26, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y quien de vosotros podra por mucho", libro_id = 40, capitulo = 6, versiculo = 27, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y por el vestido por que os", libro_id = 40, capitulo = 6, versiculo = 28, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero os digo que ni aun salomon", libro_id = 40, capitulo = 6, versiculo = 29, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y si la hierba del campo que", libro_id = 40, capitulo = 6, versiculo = 30, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no os afaneis pues diciendo que comeremos", libro_id = 40, capitulo = 6, versiculo = 31, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque los gentiles buscan todas estas cosas", libro_id = 40, capitulo = 6, versiculo = 32, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no juzgueis para que no seais juzgados", libro_id = 40, capitulo = 7, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque con el juicio con que juzgais", libro_id = 40, capitulo = 7, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y por que miras la paja que", libro_id = 40, capitulo = 7, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "o como diras a tu hermano dejame", libro_id = 40, capitulo = 7, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hipocrita saca primero la viga de tu", libro_id = 40, capitulo = 7, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no deis lo santo a los perros", libro_id = 40, capitulo = 7, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque todo aquel que pide recibe y", libro_id = 40, capitulo = 7, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "que hombre hay de vosotros que si", libro_id = 40, capitulo = 7, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "entrad por la puerta estrecha porque ancha", libro_id = 40, capitulo = 7, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque estrecha es la puerta y angosto", libro_id = 40, capitulo = 7, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no todo el que me dice senor", libro_id = 40, capitulo = 7, versiculo = 21, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cualquiera pues que me oye estas palabras", libro_id = 40, capitulo = 7, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "descendio lluvia y vinieron rios y soplaron", libro_id = 40, capitulo = 7, versiculo = 25, relevancia = 6),
            VersiculoFamoso(frase_inicial = "entonces jesus dijo a sus discipulos si", libro_id = 40, capitulo = 16, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque todo el que quiera salvar su", libro_id = 40, capitulo = 16, versiculo = 25, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque que aprovechara al hombre si ganare", libro_id = 40, capitulo = 16, versiculo = 26, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y dijo de cierto os digo que", libro_id = 40, capitulo = 18, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "asi que cualquiera que se humille como", libro_id = 40, capitulo = 18, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "otra vez os digo que si dos", libro_id = 40, capitulo = 18, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque donde estan dos o tres congregados", libro_id = 40, capitulo = 18, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "diciendo el tiempo se ha cumplido y", libro_id = 41, capitulo = 1, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y les dijo jesus venid en pos", libro_id = 41, capitulo = 1, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "entonces jesus mirandolos dijo para los hombres", libro_id = 41, capitulo = 10, versiculo = 27, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y amaras al senor tu dios con", libro_id = 41, capitulo = 12, versiculo = 30, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el segundo es semejante amaras a", libro_id = 41, capitulo = 12, versiculo = 31, relevancia = 6),
            VersiculoFamoso(frase_inicial = "respondiendo jesus le dijo vete de mi", libro_id = 42, capitulo = 4, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y decia a todos si alguno quiere", libro_id = 42, capitulo = 9, versiculo = 23, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque todo el que quiera salvar su", libro_id = 42, capitulo = 9, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "tambien dijo un hombre tenia dos hijos", libro_id = 42, capitulo = 15, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y levantandose vino a su padre y", libro_id = 42, capitulo = 15, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mas a todos los que le recibieron", libro_id = 43, capitulo = 1, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y aquel verbo fue hecho carne y", libro_id = 43, capitulo = 1, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "respondio jesus de cierto de cierto te", libro_id = 43, capitulo = 3, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "lo que es nacido de la carne", libro_id = 43, capitulo = 3, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no te maravilles de que te dije", libro_id = 43, capitulo = 3, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el viento sopla de donde quiere y", libro_id = 43, capitulo = 3, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mas la hora viene y ahora es", libro_id = 43, capitulo = 4, versiculo = 23, relevancia = 6),
            VersiculoFamoso(frase_inicial = "dios es espiritu y los que le", libro_id = 43, capitulo = 4, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el espiritu es el que da vida", libro_id = 43, capitulo = 6, versiculo = 63, relevancia = 6),
            VersiculoFamoso(frase_inicial = "en el ultimo y gran dia de", libro_id = 43, capitulo = 7, versiculo = 37, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el que cree en mi como dice", libro_id = 43, capitulo = 7, versiculo = 38, relevancia = 6),
            VersiculoFamoso(frase_inicial = "respondio jesus no es que peco este", libro_id = 43, capitulo = 9, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "me es necesario hacer las obras del", libro_id = 43, capitulo = 9, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y habiendo dicho esto clamo a gran", libro_id = 43, capitulo = 11, versiculo = 43, relevancia = 6),
            VersiculoFamoso(frase_inicial = "de cierto de cierto os digo que", libro_id = 43, capitulo = 12, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "yo la luz he venido al mundo", libro_id = 43, capitulo = 12, versiculo = 46, relevancia = 6),
            VersiculoFamoso(frase_inicial = "antes de la fiesta de la pascua", libro_id = 43, capitulo = 13, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "santificalos en tu verdad tu palabra es", libro_id = 43, capitulo = 17, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "jesus le dijo porque me has visto", libro_id = 43, capitulo = 20, versiculo = 29, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero estas se han escrito para que", libro_id = 43, capitulo = 20, versiculo = 31, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cuando hubieron comido jesus dijo a simon", libro_id = 43, capitulo = 21, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "volvio a decirle la segunda vez simon", libro_id = 43, capitulo = 21, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "le dijo la tercera vez simon hijo", libro_id = 43, capitulo = 21, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cuando llego el dia de pentecostes estaban", libro_id = 44, capitulo = 2, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y de repente vino del cielo un", libro_id = 44, capitulo = 2, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y se les aparecieron lenguas repartidas como", libro_id = 44, capitulo = 2, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y fueron todos llenos del espiritu santo", libro_id = 44, capitulo = 2, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y perseveraban en la doctrina de los", libro_id = 44, capitulo = 2, versiculo = 42, relevancia = 6),
            VersiculoFamoso(frase_inicial = "todos los que habian creido estaban juntos", libro_id = 44, capitulo = 2, versiculo = 44, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y perseverando unanimes cada dia en el", libro_id = 44, capitulo = 2, versiculo = 46, relevancia = 6),
            VersiculoFamoso(frase_inicial = "alabando a dios y teniendo favor con", libro_id = 44, capitulo = 2, versiculo = 47, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque en el vivimos y nos movemos", libro_id = 44, capitulo = 17, versiculo = 28, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque en el evangelio la justicia de", libro_id = 45, capitulo = 1, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "o menosprecias las riquezas de su benignidad", libro_id = 45, capitulo = 2, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "de ninguna manera antes bien sea dios", libro_id = 45, capitulo = 3, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "la justicia de dios por medio de", libro_id = 45, capitulo = 3, versiculo = 22, relevancia = 6),
            VersiculoFamoso(frase_inicial = "siendo justificados gratuitamente por su gracia mediante", libro_id = 45, capitulo = 3, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "a quien dios puso como propiciacion por", libro_id = 45, capitulo = 3, versiculo = 25, relevancia = 6),
            VersiculoFamoso(frase_inicial = "diciendo bienaventurados aquellos cuyas iniquidades son perdonadas", libro_id = 45, capitulo = 4, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "bienaventurado el varon a quien el senor", libro_id = 45, capitulo = 4, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "justificados pues por la fe tenemos paz", libro_id = 45, capitulo = 5, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por quien tambien tenemos entrada por la", libro_id = 45, capitulo = 5, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y no solo esto sino que tambien", libro_id = 45, capitulo = 5, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y la paciencia prueba y la prueba", libro_id = 45, capitulo = 5, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y la esperanza no averguenza porque el", libro_id = 45, capitulo = 5, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque cristo cuando aun eramos debiles a", libro_id = 45, capitulo = 5, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "ciertamente apenas morira alguno por un justo", libro_id = 45, capitulo = 5, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pues mucho mas estando ya justificados en", libro_id = 45, capitulo = 5, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque si siendo enemigos fuimos reconciliados con", libro_id = 45, capitulo = 5, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y no solo esto sino que tambien", libro_id = 45, capitulo = 5, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "oh profundidad de las riquezas de la", libro_id = 45, capitulo = 11, versiculo = 33, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque de el y por el y", libro_id = 45, capitulo = 11, versiculo = 36, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no debais a nadie nada sino el", libro_id = 45, capitulo = 13, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque no adulteraras no mataras no hurtaras", libro_id = 45, capitulo = 13, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el amor no hace mal al projimo", libro_id = 45, capitulo = 13, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y esto conociendo el tiempo que es", libro_id = 45, capitulo = 13, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "la noche esta avanzada y se acerca", libro_id = 45, capitulo = 13, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "andemos como de dia honestamente no en", libro_id = 45, capitulo = 13, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "sino vestios del senor jesucristo y no", libro_id = 45, capitulo = 13, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque ninguno de nosotros vive para si", libro_id = 45, capitulo = 14, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pues si vivimos para el senor vivimos", libro_id = 45, capitulo = 14, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "asi que los que somos fuertes debemos", libro_id = 45, capitulo = 15, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por tanto recibios los unos a los", libro_id = 45, capitulo = 15, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque la palabra de la cruz es", libro_id = 46, capitulo = 1, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque lo insensato de dios es mas", libro_id = 46, capitulo = 1, versiculo = 25, relevancia = 6),
            VersiculoFamoso(frase_inicial = "asi que hermanos cuando fui a vosotros", libro_id = 46, capitulo = 2, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pues me propuse no saber entre vosotros", libro_id = 46, capitulo = 2, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no sabeis que sois templo de dios", libro_id = 46, capitulo = 3, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "o ignorais que vuestro cuerpo es templo", libro_id = 46, capitulo = 6, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque habeis sido comprados por precio glorificad", libro_id = 46, capitulo = 6, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no sabeis que los que corren en", libro_id = 46, capitulo = 9, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "todo aquel que lucha de todo se", libro_id = 46, capitulo = 9, versiculo = 25, relevancia = 6),
            VersiculoFamoso(frase_inicial = "ahora bien hay diversidad de dones pero", libro_id = 46, capitulo = 12, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque asi como el cuerpo es uno", libro_id = 46, capitulo = 12, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "de manera que si un miembro padece", libro_id = 46, capitulo = 12, versiculo = 26, relevancia = 6),
            VersiculoFamoso(frase_inicial = "vosotros pues sois el cuerpo de cristo", libro_id = 46, capitulo = 12, versiculo = 27, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si yo hablase lenguas humanas y angelicas", libro_id = 46, capitulo = 13, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y si tuviese profecia y entendiese todos", libro_id = 46, capitulo = 13, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y si repartiese todos mis bienes para", libro_id = 46, capitulo = 13, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no hace nada indebido no busca lo", libro_id = 46, capitulo = 13, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no se goza de la injusticia mas", libro_id = 46, capitulo = 13, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "todo lo sufre todo lo cree todo", libro_id = 46, capitulo = 13, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el amor nunca deja de ser pero", libro_id = 46, capitulo = 13, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cuando yo era nino hablaba como nino", libro_id = 46, capitulo = 13, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "ahora vemos por espejo oscuramente mas entonces", libro_id = 46, capitulo = 13, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero por la gracia de dios soy", libro_id = 46, capitulo = 15, versiculo = 10, relevancia = 6),
        )
        versiculoFamosoDao.insertAll(batch3)

        val batch4 = listOf(
            VersiculoFamoso(frase_inicial = "asi que hermanos mios amados estad firmes", libro_id = 46, capitulo = 15, versiculo = 58, relevancia = 6),
            VersiculoFamoso(frase_inicial = "bendito sea el dios y padre de", libro_id = 47, capitulo = 1, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el cual nos consuela en todas nuestras", libro_id = 47, capitulo = 1, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no que seamos competentes por nosotros mismos", libro_id = 47, capitulo = 3, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque es necesario que todos nosotros comparezcamos", libro_id = 47, capitulo = 5, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y poderoso es dios para hacer que", libro_id = 47, capitulo = 9, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "la gracia del senor jesucristo el amor", libro_id = 47, capitulo = 13, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "sabiendo que el hombre no es justificado", libro_id = 48, capitulo = 2, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cristo nos redimio de la maldicion de", libro_id = 48, capitulo = 3, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "estad pues firmes en la libertad con", libro_id = 48, capitulo = 5, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque vosotros hermanos a libertad fuisteis llamados", libro_id = 48, capitulo = 5, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque toda la ley en esta sola", libro_id = 48, capitulo = 5, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "digo pues andad en el espiritu y", libro_id = 48, capitulo = 5, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque el deseo de la carne es", libro_id = 48, capitulo = 5, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero si sois guiados por el espiritu", libro_id = 48, capitulo = 5, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y manifiestas son las obras de la", libro_id = 48, capitulo = 5, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "idolatria hechicerias enemistades pleitos celos iras contiendas", libro_id = 48, capitulo = 5, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "envidias homicidios borracheras orgias y cosas semejantes", libro_id = 48, capitulo = 5, versiculo = 21, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mansedumbre templanza contra tales cosas no hay", libro_id = 48, capitulo = 5, versiculo = 23, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero los que son de cristo han", libro_id = 48, capitulo = 5, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si vivimos por el espiritu andemos tambien", libro_id = 48, capitulo = 5, versiculo = 25, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no nos hagamos vanagloriosos irritandonos unos a", libro_id = 48, capitulo = 5, versiculo = 26, relevancia = 6),
            VersiculoFamoso(frase_inicial = "segun nos escogio en el antes de", libro_id = 49, capitulo = 1, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "en quien tenemos redencion por su sangre", libro_id = 49, capitulo = 1, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero dios que es rico en misericordia", libro_id = 49, capitulo = 2, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "aun estando nosotros muertos en pecados nos", libro_id = 49, capitulo = 2, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y juntamente con el nos resucito y", libro_id = 49, capitulo = 2, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque el es nuestra paz que de", libro_id = 49, capitulo = 2, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "asi que ya no sois extranjeros ni", libro_id = 49, capitulo = 2, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por esta causa doblo mis rodillas ante", libro_id = 49, capitulo = 3, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "para que os de conforme a las", libro_id = 49, capitulo = 3, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "para que habite cristo por la fe", libro_id = 49, capitulo = 3, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "seais plenamente capaces de comprender con todos", libro_id = 49, capitulo = 3, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y de conocer el amor de cristo", libro_id = 49, capitulo = 3, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "yo pues preso en el senor os", libro_id = 49, capitulo = 4, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "con toda humildad y mansedumbre soportandoos con", libro_id = 49, capitulo = 4, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "solicitos en guardar la unidad del espiritu", libro_id = 49, capitulo = 4, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "un cuerpo y un espiritu como fuisteis", libro_id = 49, capitulo = 4, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "un senor una fe un bautismo", libro_id = 49, capitulo = 4, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el mismo constituyo a unos apostoles", libro_id = 49, capitulo = 4, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "a fin de perfeccionar a los santos", libro_id = 49, capitulo = 4, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hasta que todos lleguemos a la unidad", libro_id = 49, capitulo = 4, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "sino que siguiendo la verdad en amor", libro_id = 49, capitulo = 4, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "de quien todo el cuerpo bien concertado", libro_id = 49, capitulo = 4, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "sed pues imitadores de dios como hijos", libro_id = 49, capitulo = 5, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y andad en amor como tambien cristo", libro_id = 49, capitulo = 5, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no os embriagueis con vino en lo", libro_id = 49, capitulo = 5, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hablando entre vosotros con salmos con himnos", libro_id = 49, capitulo = 5, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "dando siempre gracias por todo al dios", libro_id = 49, capitulo = 5, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "maridos amad a vuestras mujeres asi como", libro_id = 49, capitulo = 5, versiculo = 25, relevancia = 6),
            VersiculoFamoso(frase_inicial = "estad pues firmes cenidos vuestros lomos con", libro_id = 49, capitulo = 6, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y tomad el yelmo de la salvacion", libro_id = 49, capitulo = 6, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "orando en todo tiempo con toda oracion", libro_id = 49, capitulo = 6, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "conforme a mi anhelo y esperanza de", libro_id = 50, capitulo = 1, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque para mi el vivir es cristo", libro_id = 50, capitulo = 1, versiculo = 21, relevancia = 6),
            VersiculoFamoso(frase_inicial = "haya pues en vosotros este sentir que", libro_id = 50, capitulo = 2, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el cual siendo en forma de dios", libro_id = 50, capitulo = 2, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "sino que se despojo a si mismo", libro_id = 50, capitulo = 2, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y estando en la condicion de hombre", libro_id = 50, capitulo = 2, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por lo cual dios tambien le exalto", libro_id = 50, capitulo = 2, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "para que en el nombre de jesus", libro_id = 50, capitulo = 2, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y toda lengua confiese que jesucristo es", libro_id = 50, capitulo = 2, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero cuantas cosas eran para mi ganancia", libro_id = 50, capitulo = 3, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y ciertamente aun estimo todas las cosas", libro_id = 50, capitulo = 3, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y ser hallado en el no teniendo", libro_id = 50, capitulo = 3, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "a fin de conocerle y el poder", libro_id = 50, capitulo = 3, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hermanos yo mismo no pretendo haberlo ya", libro_id = 50, capitulo = 3, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por lo demas hermanos todo lo que", libro_id = 50, capitulo = 4, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "lo que aprendisteis y recibisteis y oisteis", libro_id = 50, capitulo = 4, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no lo digo porque tenga escasez pues", libro_id = 50, capitulo = 4, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "se vivir humildemente y se tener abundancia", libro_id = 50, capitulo = 4, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por lo cual tambien nosotros desde el", libro_id = 51, capitulo = 1, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el cual nos ha librado de la", libro_id = 51, capitulo = 1, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque en el fueron creadas todas las", libro_id = 51, capitulo = 1, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el es antes de todas las", libro_id = 51, capitulo = 1, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el es la cabeza del cuerpo", libro_id = 51, capitulo = 1, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "para que sean consolados sus corazones unidos", libro_id = 51, capitulo = 2, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "en quien estan escondidos todos los tesoros", libro_id = 51, capitulo = 2, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por tanto de la manera que habeis", libro_id = 51, capitulo = 2, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "arraigados y sobreedificados en el y confirmados", libro_id = 51, capitulo = 2, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mirad que nadie os engane por medio", libro_id = 51, capitulo = 2, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque en el habita corporalmente toda la", libro_id = 51, capitulo = 2, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y vosotros estais completos en el que", libro_id = 51, capitulo = 2, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "en el tambien fuisteis circuncidados con circuncision", libro_id = 51, capitulo = 2, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "sepultados con el en el bautismo en", libro_id = 51, capitulo = 2, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y a vosotros estando muertos en pecados", libro_id = 51, capitulo = 2, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "anulando el acta de los decretos que", libro_id = 51, capitulo = 2, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y despojando a los principados y a", libro_id = 51, capitulo = 2, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si pues habeis resucitado con cristo buscad", libro_id = 51, capitulo = 3, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "poned la mira en las cosas de", libro_id = 51, capitulo = 3, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque habeis muerto y vuestra vida esta", libro_id = 51, capitulo = 3, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cuando cristo vuestra vida se manifieste entonces", libro_id = 51, capitulo = 3, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "vestios pues como escogidos de dios santos", libro_id = 51, capitulo = 3, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "soportandoos unos a otros y perdonandoos unos", libro_id = 51, capitulo = 3, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y sobre todas estas cosas vestios de", libro_id = 51, capitulo = 3, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y la paz de dios gobierne en", libro_id = 51, capitulo = 3, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "casadas estad sujetas a vuestros maridos como", libro_id = 51, capitulo = 3, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "maridos amad a vuestras mujeres y no", libro_id = 51, capitulo = 3, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hijos obedeced a vuestros padres en todo", libro_id = 51, capitulo = 3, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "padres no exaspereis a vuestros hijos para", libro_id = 51, capitulo = 3, versiculo = 21, relevancia = 6),
            VersiculoFamoso(frase_inicial = "siervos obedeced en todo a vuestros amos", libro_id = 51, capitulo = 3, versiculo = 22, relevancia = 6),
            VersiculoFamoso(frase_inicial = "perseverad en la oracion velando en ella", libro_id = 51, capitulo = 4, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "damos siempre gracias a dios por todos", libro_id = 52, capitulo = 1, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "acordandonos sin cesar delante del dios y", libro_id = 52, capitulo = 1, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "tampoco queremos hermanos que ignoreis acerca de", libro_id = 52, capitulo = 4, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque si creemos que jesus murio y", libro_id = 52, capitulo = 4, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque el senor mismo con voz de", libro_id = 52, capitulo = 4, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "luego nosotros los que vivimos los que", libro_id = 52, capitulo = 4, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero nosotros debemos dar siempre gracias a", libro_id = 53, capitulo = 2, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero fiel es el senor que os", libro_id = 53, capitulo = 3, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "palabra fiel y digna de ser recibida", libro_id = 54, capitulo = 1, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "exhorto ante todo a que se hagan", libro_id = 54, capitulo = 2, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por los reyes y por todos los", libro_id = 54, capitulo = 2, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque esto es bueno y agradable delante", libro_id = 54, capitulo = 2, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "desecha las fabulas profanas y de viejas", libro_id = 54, capitulo = 4, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque el ejercicio corporal para poco es", libro_id = 54, capitulo = 4, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero gran ganancia es la piedad acompanada", libro_id = 54, capitulo = 6, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque nada hemos traido a este mundo", libro_id = 54, capitulo = 6, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "asi que teniendo sustento y abrigo estemos", libro_id = 54, capitulo = 6, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por lo cual asimismo padezco esto pero", libro_id = 55, capitulo = 1, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "tu pues hijo mio esfuerzate en la", libro_id = 55, capitulo = 2, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "lo que has oido de mi ante", libro_id = 55, capitulo = 2, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "tu pues sufre penalidades como buen soldado", libro_id = 55, capitulo = 2, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "palabra fiel es esta si somos muertos", libro_id = 55, capitulo = 2, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si sufrimos tambien reinaremos con el si", libro_id = 55, capitulo = 2, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si fueremos infieles el permanece fiel el", libro_id = 55, capitulo = 2, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "tambien debes saber esto que en los", libro_id = 55, capitulo = 3, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque habra hombres amadores de si mismos", libro_id = 55, capitulo = 3, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero tu se sobrio en todo soporta", libro_id = 55, capitulo = 4, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque yo ya estoy para ser sacrificado", libro_id = 55, capitulo = 4, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "he peleado la buena batalla he acabado", libro_id = 55, capitulo = 4, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por lo demas me esta guardada la", libro_id = 55, capitulo = 4, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "en la esperanza de la vida eterna", libro_id = 56, capitulo = 1, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y a su debido tiempo manifesto su", libro_id = 56, capitulo = 1, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero cuando se manifesto la bondad de", libro_id = 56, capitulo = 3, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "nos salvo no por obras de justicia", libro_id = 56, capitulo = 3, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "doy gracias a mi dios haciendo siempre", libro_id = 57, capitulo = 1, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "para que la participacion de tu fe", libro_id = 57, capitulo = 1, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "dios habiendo hablado muchas veces y de", libro_id = 58, capitulo = 1, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "en estos postreros dias nos ha hablado", libro_id = 58, capitulo = 1, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el cual siendo el resplandor de su", libro_id = 58, capitulo = 1, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "asi que por cuanto los hijos participaron", libro_id = 58, capitulo = 2, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por lo cual debia ser en todo", libro_id = 58, capitulo = 2, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pues en cuanto el mismo padecio siendo", libro_id = 58, capitulo = 2, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "entre tanto que se dice si oyereis", libro_id = 58, capitulo = 3, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque no tenemos un sumo sacerdote que", libro_id = 58, capitulo = 4, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "acerquemonos pues confiadamente al trono de la", libro_id = 58, capitulo = 4, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por lo cual puede tambien salvar perpetuamente", libro_id = 58, capitulo = 7, versiculo = 25, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y de la manera que esta establecido", libro_id = 58, capitulo = 9, versiculo = 27, relevancia = 6),
            VersiculoFamoso(frase_inicial = "asi tambien cristo fue ofrecido una sola", libro_id = 58, capitulo = 9, versiculo = 28, relevancia = 6),
        )
        versiculoFamosoDao.insertAll(batch4)

        val batch5 = listOf(
            VersiculoFamoso(frase_inicial = "mantengamos firme sin fluctuar la profesion de", libro_id = 58, capitulo = 10, versiculo = 23, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y consideremonos unos a otros para estimularnos", libro_id = 58, capitulo = 10, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no dejando de congregarnos como algunos tienen", libro_id = 58, capitulo = 10, versiculo = 25, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por la fe noe cuando fue advertido", libro_id = 58, capitulo = 11, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por la fe abraham siendo llamado obedecio", libro_id = 58, capitulo = 11, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y que mas digo porque el tiempo", libro_id = 58, capitulo = 11, versiculo = 32, relevancia = 6),
            VersiculoFamoso(frase_inicial = "que por fe conquistaron reinos hicieron justicia", libro_id = 58, capitulo = 11, versiculo = 33, relevancia = 6),
            VersiculoFamoso(frase_inicial = "apagaron fuegos impetuosos evitaron filo de espada", libro_id = 58, capitulo = 11, versiculo = 34, relevancia = 6),
            VersiculoFamoso(frase_inicial = "las mujeres recibieron sus muertos mediante resurreccion", libro_id = 58, capitulo = 11, versiculo = 35, relevancia = 6),
            VersiculoFamoso(frase_inicial = "otros experimentaron vituperios y azotes y a", libro_id = 58, capitulo = 11, versiculo = 36, relevancia = 6),
            VersiculoFamoso(frase_inicial = "fueron apedreados aserrados puestos a prueba muertos", libro_id = 58, capitulo = 11, versiculo = 37, relevancia = 6),
            VersiculoFamoso(frase_inicial = "de los cuales el mundo no era", libro_id = 58, capitulo = 11, versiculo = 38, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y todos estos aunque alcanzaron buen testimonio", libro_id = 58, capitulo = 11, versiculo = 39, relevancia = 6),
            VersiculoFamoso(frase_inicial = "proveyendo dios alguna cosa mejor para nosotros", libro_id = 58, capitulo = 11, versiculo = 40, relevancia = 6),
            VersiculoFamoso(frase_inicial = "permanezca el amor fraternal", libro_id = 58, capitulo = 13, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no os olvideis de la hospitalidad porque", libro_id = 58, capitulo = 13, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "honroso sea en todos el matrimonio y", libro_id = 58, capitulo = 13, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "sean vuestras costumbres sin avaricia contentos con", libro_id = 58, capitulo = 13, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "de manera que podemos decir confiadamente el", libro_id = 58, capitulo = 13, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el dios de paz que resucito", libro_id = 58, capitulo = 13, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "os haga aptos en toda obra buena", libro_id = 58, capitulo = 13, versiculo = 21, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por esto mis amados hermanos todo hombre", libro_id = 59, capitulo = 1, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque la ira del hombre no obra", libro_id = 59, capitulo = 1, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por lo cual desechando toda inmundicia y", libro_id = 59, capitulo = 1, versiculo = 21, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero sed hacedores de la palabra y", libro_id = 59, capitulo = 1, versiculo = 22, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque si alguno es oidor de la", libro_id = 59, capitulo = 1, versiculo = 23, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hermanos mios de que aprovechara si alguno", libro_id = 59, capitulo = 2, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "asi tambien la fe si no tiene", libro_id = 59, capitulo = 2, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero alguno dira tu tienes fe y", libro_id = 59, capitulo = 2, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "tu crees que dios es uno bien", libro_id = 59, capitulo = 2, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mas quieres saber hombre vano que la", libro_id = 59, capitulo = 2, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "quien es sabio y entendido entre vosotros", libro_id = 59, capitulo = 3, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero si teneis celos amargos y contencion", libro_id = 59, capitulo = 3, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque esta sabiduria no es la que", libro_id = 59, capitulo = 3, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque donde hay celos y contencion alli", libro_id = 59, capitulo = 3, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero la sabiduria que es de lo", libro_id = 59, capitulo = 3, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero el da mayor gracia por esto", libro_id = 59, capitulo = 4, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "acercaos a dios y el se acercara", libro_id = 59, capitulo = 4, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "humillaos delante del senor y el os", libro_id = 59, capitulo = 4, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y la oracion de fe salvara al", libro_id = 59, capitulo = 5, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "confesaos vuestras ofensas unos a otros y", libro_id = 59, capitulo = 5, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "bendito el dios y padre de nuestro", libro_id = 60, capitulo = 1, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "para una herencia incorruptible incontaminada e inmarcesible", libro_id = 60, capitulo = 1, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "que sois guardados por el poder de", libro_id = 60, capitulo = 1, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "en lo cual vosotros os alegrais aunque", libro_id = 60, capitulo = 1, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "siendo renacidos no de simiente corruptible sino", libro_id = 60, capitulo = 1, versiculo = 23, relevancia = 6),
            VersiculoFamoso(frase_inicial = "desead como ninos recien nacidos la leche", libro_id = 60, capitulo = 2, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "quien llevo el mismo nuestros pecados en", libro_id = 60, capitulo = 2, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque vosotros erais como ovejas descarriadas pero", libro_id = 60, capitulo = 2, versiculo = 25, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no devolviendo mal por mal ni maldicion", libro_id = 60, capitulo = 3, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "sino santificad a dios el senor en", libro_id = 60, capitulo = 3, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y ante todo tened entre vosotros ferviente", libro_id = 60, capitulo = 4, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cada uno segun el don que ha", libro_id = 60, capitulo = 4, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si alguno habla hable conforme a las", libro_id = 60, capitulo = 4, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "humillaos pues bajo la poderosa mano de", libro_id = 60, capitulo = 5, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "sed sobrios y velad porque vuestro adversario", libro_id = 60, capitulo = 5, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "al cual resistid firmes en la fe", libro_id = 60, capitulo = 5, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mas el dios de toda gracia que", libro_id = 60, capitulo = 5, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "saludaos unos a otros con osculo de", libro_id = 60, capitulo = 5, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "como todas las cosas que pertenecen a", libro_id = 61, capitulo = 1, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por medio de las cuales nos ha", libro_id = 61, capitulo = 1, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el senor no retarda su promesa segun", libro_id = 61, capitulo = 3, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "antes bien creced en la gracia y", libro_id = 61, capitulo = 3, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "este es el mensaje que hemos oido", libro_id = 62, capitulo = 1, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si decimos que tenemos comunion con el", libro_id = 62, capitulo = 1, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero si andamos en luz como el", libro_id = 62, capitulo = 1, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si decimos que no tenemos pecado nos", libro_id = 62, capitulo = 1, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hijitos mios estas cosas os escribo para", libro_id = 62, capitulo = 2, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el es la propiciacion por nuestros", libro_id = 62, capitulo = 2, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y en esto sabemos que nosotros le", libro_id = 62, capitulo = 2, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no ameis al mundo ni las cosas", libro_id = 62, capitulo = 2, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque todo lo que hay en el", libro_id = 62, capitulo = 2, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el mundo pasa y sus deseos", libro_id = 62, capitulo = 2, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mirad cual amor nos ha dado el", libro_id = 62, capitulo = 3, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "amados ahora somos hijos de dios y", libro_id = 62, capitulo = 3, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y todo aquel que tiene esta esperanza", libro_id = 62, capitulo = 3, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "todo aquel que comete pecado infringe tambien", libro_id = 62, capitulo = 3, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hijitos mios no amemos de palabra ni", libro_id = 62, capitulo = 3, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y en esto conocemos que somos de", libro_id = 62, capitulo = 3, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pues si nuestro corazon nos reprende mayor", libro_id = 62, capitulo = 3, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "amados si nuestro corazon no nos reprende", libro_id = 62, capitulo = 3, versiculo = 21, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y cualquiera cosa que pidieremos la recibiremos", libro_id = 62, capitulo = 3, versiculo = 22, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y este es su mandamiento que creamos", libro_id = 62, capitulo = 3, versiculo = 23, relevancia = 6),
            VersiculoFamoso(frase_inicial = "amados amemonos unos a otros porque el", libro_id = 62, capitulo = 4, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "en esto consiste el amor no en", libro_id = 62, capitulo = 4, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "amados si dios nos ha amado asi", libro_id = 62, capitulo = 4, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "nadie ha visto jamas a dios si", libro_id = 62, capitulo = 4, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "en esto conocemos que permanecemos en el", libro_id = 62, capitulo = 4, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y nosotros hemos visto y testificamos que", libro_id = 62, capitulo = 4, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "todo aquel que confiese que jesus es", libro_id = 62, capitulo = 4, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y nosotros hemos conocido y creido el", libro_id = 62, capitulo = 4, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "en esto se ha perfeccionado el amor", libro_id = 62, capitulo = 4, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "en el amor no hay temor sino", libro_id = 62, capitulo = 4, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "todo aquel que cree que jesus es", libro_id = 62, capitulo = 5, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "en esto conocemos que amamos a los", libro_id = 62, capitulo = 5, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pues este es el amor a dios", libro_id = 62, capitulo = 5, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque todo lo que es nacido de", libro_id = 62, capitulo = 5, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "quien es el que vence al mundo", libro_id = 62, capitulo = 5, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y este es el testimonio que dios", libro_id = 62, capitulo = 5, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el que tiene al hijo tiene la", libro_id = 62, capitulo = 5, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "estas cosas os he escrito a vosotros", libro_id = 62, capitulo = 5, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "sabemos que todo aquel que ha nacido", libro_id = 62, capitulo = 5, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "sabemos que somos de dios y el", libro_id = 62, capitulo = 5, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero sabemos que el hijo de dios", libro_id = 62, capitulo = 5, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hijitos guardaos de los idolos amen", libro_id = 62, capitulo = 5, versiculo = 21, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero vosotros amados edificandoos sobre vuestra santisima", libro_id = 65, capitulo = 1, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "conservaos en el amor de dios esperando", libro_id = 65, capitulo = 1, versiculo = 21, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y a aquel que es poderoso para", libro_id = 65, capitulo = 1, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "al unico y sabio dios nuestro salvador", libro_id = 65, capitulo = 1, versiculo = 25, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el que tiene oido oiga lo que", libro_id = 66, capitulo = 2, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no temas en nada lo que vas", libro_id = 66, capitulo = 2, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el que tiene oido oiga lo que", libro_id = 66, capitulo = 2, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el que venciere sera vestido de vestiduras", libro_id = 66, capitulo = 3, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por cuanto has guardado la palabra de", libro_id = 66, capitulo = 3, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "he aqui yo vengo pronto reten lo", libro_id = 66, capitulo = 3, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "al que venciere yo lo hare columna", libro_id = 66, capitulo = 3, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "que decian a gran voz el cordero", libro_id = 66, capitulo = 5, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y a todo lo creado que esta", libro_id = 66, capitulo = 5, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "entonces oi una gran voz en el", libro_id = 66, capitulo = 12, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y ellos le han vencido por medio", libro_id = 66, capitulo = 12, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "oi una voz que desde el cielo", libro_id = 66, capitulo = 14, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y oi como la voz de una", libro_id = 66, capitulo = 19, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "gocemonos y alegremonos y demosle gloria porque", libro_id = 66, capitulo = 19, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y a ella se le ha concedido", libro_id = 66, capitulo = 19, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el angel me dijo escribe bienaventurados", libro_id = 66, capitulo = 19, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y vi un gran trono blanco y", libro_id = 66, capitulo = 20, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y vi a los muertos grandes y", libro_id = 66, capitulo = 20, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el que no se hallo inscrito", libro_id = 66, capitulo = 20, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "vi un cielo nuevo y una tierra", libro_id = 66, capitulo = 21, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y yo juan vi la santa ciudad", libro_id = 66, capitulo = 21, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y oi una gran voz del cielo", libro_id = 66, capitulo = 21, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el que estaba sentado en el", libro_id = 66, capitulo = 21, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y me dijo hecho esta yo soy", libro_id = 66, capitulo = 21, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "despues me mostro un rio limpio de", libro_id = 66, capitulo = 22, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "en medio de la calle de la", libro_id = 66, capitulo = 22, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y no habra mas maldicion y el", libro_id = 66, capitulo = 22, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y veran su rostro y su nombre", libro_id = 66, capitulo = 22, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no habra alli mas noche y no", libro_id = 66, capitulo = 22, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "he aqui yo vengo pronto y mi", libro_id = 66, capitulo = 22, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "yo soy el alfa y la omega", libro_id = 66, capitulo = 22, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el que da testimonio de estas cosas", libro_id = 66, capitulo = 22, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "la gracia de nuestro senor jesucristo sea", libro_id = 66, capitulo = 22, versiculo = 21, relevancia = 6),        )
        versiculoFamosoDao.insertAll(batch5)
    }

    private suspend fun insertFamososBatch6() {
        val batch6 = listOf(
            VersiculoFamoso(frase_inicial = "llamo jehova a moises y hablo con", libro_id = 3, capitulo = 1, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "habla a los hijos de israel y", libro_id = 3, capitulo = 1, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si su ofrenda fuere holocausto vacuno macho", libro_id = 3, capitulo = 1, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y pondra su mano sobre la cabeza", libro_id = 3, capitulo = 1, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "entonces degollara el becerro en la presencia", libro_id = 3, capitulo = 1, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y desollara el holocausto y lo dividira", libro_id = 3, capitulo = 1, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y los hijos del sacerdote aaron pondran", libro_id = 3, capitulo = 1, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "luego los sacerdotes hijos de aaron acomodaran", libro_id = 3, capitulo = 1, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y lavara con agua los intestinos y", libro_id = 3, capitulo = 1, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si su ofrenda para holocausto fuere del", libro_id = 3, capitulo = 1, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y lo degollara al lado norte del", libro_id = 3, capitulo = 1, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "lo dividira en sus piezas con su", libro_id = 3, capitulo = 1, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y lavara las entranas y las piernas", libro_id = 3, capitulo = 1, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si la ofrenda para jehova fuere holocausto", libro_id = 3, capitulo = 1, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el sacerdote la ofrecera sobre el", libro_id = 3, capitulo = 1, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y le quitara el buche y las", libro_id = 3, capitulo = 1, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y la hendera por sus alas pero", libro_id = 3, capitulo = 1, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cuando alguna persona ofreciere oblacion a jehova", libro_id = 3, capitulo = 2, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y la traera a los sacerdotes hijos", libro_id = 3, capitulo = 2, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y lo que resta de la ofrenda", libro_id = 3, capitulo = 2, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cuando ofrecieres ofrenda cocida en horno sera", libro_id = 3, capitulo = 2, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mas si ofrecieres ofrenda de sarten sera", libro_id = 3, capitulo = 2, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "la cual partiras en piezas y echaras", libro_id = 3, capitulo = 2, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si ofrecieres ofrenda cocida en cazuela se", libro_id = 3, capitulo = 2, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y traeras a jehova la ofrenda que", libro_id = 3, capitulo = 2, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y tomara el sacerdote de aquella ofrenda", libro_id = 3, capitulo = 2, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y lo que resta de la ofrenda", libro_id = 3, capitulo = 2, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "ninguna ofrenda que ofreciereis a jehova sera", libro_id = 3, capitulo = 2, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "como ofrenda de primicias las ofrecereis a", libro_id = 3, capitulo = 2, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y sazonaras con sal toda ofrenda que", libro_id = 3, capitulo = 2, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si ofrecieres a jehova ofrenda de primicias", libro_id = 3, capitulo = 2, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y pondras sobre ella aceite y pondras", libro_id = 3, capitulo = 2, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el sacerdote hara arder el memorial", libro_id = 3, capitulo = 2, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si su ofrenda fuere sacrificio de paz", libro_id = 3, capitulo = 3, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pondra su mano sobre la cabeza de", libro_id = 3, capitulo = 3, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "luego ofrecera del sacrificio de paz como", libro_id = 3, capitulo = 3, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y los dos rinones y la grosura", libro_id = 3, capitulo = 3, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y los hijos de aaron haran arder", libro_id = 3, capitulo = 3, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mas si de ovejas fuere su ofrenda", libro_id = 3, capitulo = 3, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si ofreciere cordero por su ofrenda lo", libro_id = 3, capitulo = 3, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pondra su mano sobre la cabeza de", libro_id = 3, capitulo = 3, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y del sacrificio de paz ofrecera por", libro_id = 3, capitulo = 3, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "asimismo los dos rinones y la grosura", libro_id = 3, capitulo = 3, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el sacerdote hara arder esto sobre", libro_id = 3, capitulo = 3, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si fuere cabra su ofrenda la ofrecera", libro_id = 3, capitulo = 3, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pondra su mano sobre la cabeza de", libro_id = 3, capitulo = 3, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "despues ofrecera de ella su ofrenda encendida", libro_id = 3, capitulo = 3, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "los dos rinones la grosura que esta", libro_id = 3, capitulo = 3, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el sacerdote hara arder esto sobre", libro_id = 3, capitulo = 3, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "estatuto perpetuo sera por vuestras edades dondequiera", libro_id = 3, capitulo = 3, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hablo jehova a moises diciendo", libro_id = 3, capitulo = 4, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "habla a los hijos de israel y", libro_id = 3, capitulo = 4, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si el sacerdote ungido pecare segun el", libro_id = 3, capitulo = 4, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "traera el becerro a la puerta del", libro_id = 3, capitulo = 4, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el sacerdote ungido tomara de la", libro_id = 3, capitulo = 4, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y mojara el sacerdote su dedo en", libro_id = 3, capitulo = 4, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el sacerdote pondra de esa sangre", libro_id = 3, capitulo = 4, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y tomara del becerro para la expiacion", libro_id = 3, capitulo = 4, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "los dos rinones la grosura que esta", libro_id = 3, capitulo = 4, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "de la manera que se quita del", libro_id = 3, capitulo = 4, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y la piel del becerro y toda", libro_id = 3, capitulo = 4, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "en fin todo el becerro sacara fuera", libro_id = 3, capitulo = 4, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si toda la congregacion de israel hubiere", libro_id = 3, capitulo = 4, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "luego que llegue a ser conocido el", libro_id = 3, capitulo = 4, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y los ancianos de la congregacion pondran", libro_id = 3, capitulo = 4, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el sacerdote ungido metera de la", libro_id = 3, capitulo = 4, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y mojara el sacerdote su dedo en", libro_id = 3, capitulo = 4, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y de aquella sangre pondra sobre los", libro_id = 3, capitulo = 4, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y le quitara toda la grosura y", libro_id = 3, capitulo = 4, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y hara de aquel becerro como hizo", libro_id = 3, capitulo = 4, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y sacara el becerro fuera del campamento", libro_id = 3, capitulo = 4, versiculo = 21, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cuando pecare un jefe e hiciere por", libro_id = 3, capitulo = 4, versiculo = 22, relevancia = 6),
            VersiculoFamoso(frase_inicial = "luego que conociere su pecado que cometio", libro_id = 3, capitulo = 4, versiculo = 23, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y pondra su mano sobre la cabeza", libro_id = 3, capitulo = 4, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y con su dedo el sacerdote tomara", libro_id = 3, capitulo = 4, versiculo = 25, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y quemara toda su grosura sobre el", libro_id = 3, capitulo = 4, versiculo = 26, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si alguna persona del pueblo pecare por", libro_id = 3, capitulo = 4, versiculo = 27, relevancia = 6),
            VersiculoFamoso(frase_inicial = "luego que conociere su pecado que cometio", libro_id = 3, capitulo = 4, versiculo = 28, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y pondra su mano sobre la cabeza", libro_id = 3, capitulo = 4, versiculo = 29, relevancia = 6),
            VersiculoFamoso(frase_inicial = "luego con su dedo el sacerdote tomara", libro_id = 3, capitulo = 4, versiculo = 30, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y le quitara toda su grosura de", libro_id = 3, capitulo = 4, versiculo = 31, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y si por su ofrenda por el", libro_id = 3, capitulo = 4, versiculo = 32, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y pondra su mano sobre la cabeza", libro_id = 3, capitulo = 4, versiculo = 33, relevancia = 6),
            VersiculoFamoso(frase_inicial = "despues con su dedo el sacerdote tomara", libro_id = 3, capitulo = 4, versiculo = 34, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y le quitara toda su grosura como", libro_id = 3, capitulo = 4, versiculo = 35, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si alguno pecare por haber sido llamado", libro_id = 3, capitulo = 5, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "asimismo la persona que hubiere tocado cualquiera", libro_id = 3, capitulo = 5, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "o si tocare inmundicia de hombre cualquiera", libro_id = 3, capitulo = 5, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "o si alguno jurare a la ligera", libro_id = 3, capitulo = 5, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cuando pecare en alguna de estas cosas", libro_id = 3, capitulo = 5, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y para su expiacion traera a jehova", libro_id = 3, capitulo = 5, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y si no tuviere lo suficiente para", libro_id = 3, capitulo = 5, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y los traera al sacerdote el cual", libro_id = 3, capitulo = 5, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y rociara de la sangre de la", libro_id = 3, capitulo = 5, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y del otro hara holocausto conforme al", libro_id = 3, capitulo = 5, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mas si no tuviere lo suficiente para", libro_id = 3, capitulo = 5, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "la traera pues al sacerdote y el", libro_id = 3, capitulo = 5, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y hara el sacerdote expiacion por el", libro_id = 3, capitulo = 5, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hablo mas jehova a moises diciendo", libro_id = 3, capitulo = 5, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cuando alguna persona cometiere falta y pecare", libro_id = 3, capitulo = 5, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y pagara lo que hubiere defraudado de", libro_id = 3, capitulo = 5, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "finalmente si una persona pecare o hiciere", libro_id = 3, capitulo = 5, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "traera pues al sacerdote para expiacion segun", libro_id = 3, capitulo = 5, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "es infraccion y ciertamente delinquio contra jehova", libro_id = 3, capitulo = 5, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hablo jehova a moises diciendo", libro_id = 3, capitulo = 6, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cuando una persona pecare e hiciere prevaricacion", libro_id = 3, capitulo = 6, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "o habiendo hallado lo perdido despues lo", libro_id = 3, capitulo = 6, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "entonces habiendo pecado y ofendido restituira aquello", libro_id = 3, capitulo = 6, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "o todo aquello sobre que hubiere jurado", libro_id = 3, capitulo = 6, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y para expiacion de su culpa traera", libro_id = 3, capitulo = 6, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el sacerdote hara expiacion por el", libro_id = 3, capitulo = 6, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hablo aun jehova a moises diciendo", libro_id = 3, capitulo = 6, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "manda a aaron y a sus hijos", libro_id = 3, capitulo = 6, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el sacerdote se pondra su vestidura", libro_id = 3, capitulo = 6, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "despues se quitara sus vestiduras y se", libro_id = 3, capitulo = 6, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el fuego encendido sobre el altar", libro_id = 3, capitulo = 6, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el fuego ardera continuamente en el altar", libro_id = 3, capitulo = 6, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "esta es la ley de la ofrenda", libro_id = 3, capitulo = 6, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y tomara de ella un punado de", libro_id = 3, capitulo = 6, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el sobrante de ella lo comeran", libro_id = 3, capitulo = 6, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "no se cocera con levadura la he", libro_id = 3, capitulo = 6, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "todos los varones de los hijos de", libro_id = 3, capitulo = 6, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hablo tambien jehova a moises diciendo", libro_id = 3, capitulo = 6, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "esta es la ofrenda de aaron y", libro_id = 3, capitulo = 6, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "en sarten se preparara con aceite frita", libro_id = 3, capitulo = 6, versiculo = 21, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el sacerdote que en lugar de", libro_id = 3, capitulo = 6, versiculo = 22, relevancia = 6),
            VersiculoFamoso(frase_inicial = "toda ofrenda de sacerdote sera enteramente quemada", libro_id = 3, capitulo = 6, versiculo = 23, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y hablo jehova a moises diciendo", libro_id = 3, capitulo = 6, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "habla a aaron y a sus hijos", libro_id = 3, capitulo = 6, versiculo = 25, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el sacerdote que la ofreciere por el", libro_id = 3, capitulo = 6, versiculo = 26, relevancia = 6),
            VersiculoFamoso(frase_inicial = "todo lo que tocare su carne sera", libro_id = 3, capitulo = 6, versiculo = 27, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y la vasija de barro en que", libro_id = 3, capitulo = 6, versiculo = 28, relevancia = 6),
            VersiculoFamoso(frase_inicial = "todo varon de entre los sacerdotes la", libro_id = 3, capitulo = 6, versiculo = 29, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mas no se comera ninguna ofrenda de", libro_id = 3, capitulo = 6, versiculo = 30, relevancia = 6),
            VersiculoFamoso(frase_inicial = "asimismo esta es la ley del sacrificio", libro_id = 3, capitulo = 7, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "en el lugar donde deguellan el holocausto", libro_id = 3, capitulo = 7, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y de ella ofrecera toda su grosura", libro_id = 3, capitulo = 7, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "los dos rinones la grosura que esta", libro_id = 3, capitulo = 7, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el sacerdote lo hara arder sobre", libro_id = 3, capitulo = 7, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "todo varon de entre los sacerdotes la", libro_id = 3, capitulo = 7, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "como el sacrificio por el pecado asi", libro_id = 3, capitulo = 7, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y el sacerdote que ofreciere holocausto de", libro_id = 3, capitulo = 7, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "asimismo toda ofrenda que se cociere en", libro_id = 3, capitulo = 7, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y toda ofrenda amasada con aceite o", libro_id = 3, capitulo = 7, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y esta es la ley del sacrificio", libro_id = 3, capitulo = 7, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si se ofreciere en accion de gracias", libro_id = 3, capitulo = 7, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "con tortas de pan leudo presentara su", libro_id = 3, capitulo = 7, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y de toda la ofrenda presentara una", libro_id = 3, capitulo = 7, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y la carne del sacrificio de paz", libro_id = 3, capitulo = 7, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mas si el sacrificio de su ofrenda", libro_id = 3, capitulo = 7, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y lo que quedare de la carne", libro_id = 3, capitulo = 7, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "si se comiere de la carne del", libro_id = 3, capitulo = 7, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y la carne que tocare alguna cosa", libro_id = 3, capitulo = 7, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero la persona que comiere la carne", libro_id = 3, capitulo = 7, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "ademas la persona que tocare alguna cosa", libro_id = 3, capitulo = 7, versiculo = 21, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hablo mas jehova a moises diciendo", libro_id = 3, capitulo = 7, versiculo = 22, relevancia = 6),
            VersiculoFamoso(frase_inicial = "habla a los hijos de israel diciendo", libro_id = 3, capitulo = 7, versiculo = 23, relevancia = 6),
            VersiculoFamoso(frase_inicial = "la grosura de animal muerto y la", libro_id = 3, capitulo = 7, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque cualquiera que comiere grosura de animal", libro_id = 3, capitulo = 7, versiculo = 25, relevancia = 6),
            VersiculoFamoso(frase_inicial = "ademas ninguna sangre comereis en ningun lugar", libro_id = 3, capitulo = 7, versiculo = 26, relevancia = 6),
            VersiculoFamoso(frase_inicial = "cualquiera persona que comiere de alguna sangre", libro_id = 3, capitulo = 7, versiculo = 27, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hablo mas jehova a moises diciendo", libro_id = 3, capitulo = 7, versiculo = 28, relevancia = 6),
            VersiculoFamoso(frase_inicial = "habla a los hijos de israel y", libro_id = 3, capitulo = 7, versiculo = 29, relevancia = 6),
            VersiculoFamoso(frase_inicial = "sus manos traeran las ofrendas que se", libro_id = 3, capitulo = 7, versiculo = 30, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y la grosura la hara arder el", libro_id = 3, capitulo = 7, versiculo = 31, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y dareis al sacerdote para ser elevada", libro_id = 3, capitulo = 7, versiculo = 32, relevancia = 6),
            VersiculoFamoso(frase_inicial = "el que de los hijos de aaron", libro_id = 3, capitulo = 7, versiculo = 33, relevancia = 6),
            VersiculoFamoso(frase_inicial = "porque he tomado de los sacrificios de", libro_id = 3, capitulo = 7, versiculo = 34, relevancia = 6),
            VersiculoFamoso(frase_inicial = "esta es la porcion de aaron y", libro_id = 3, capitulo = 7, versiculo = 35, relevancia = 6),
            VersiculoFamoso(frase_inicial = "la cual mando jehova que les diesen", libro_id = 3, capitulo = 7, versiculo = 36, relevancia = 6),
            VersiculoFamoso(frase_inicial = "esta es la ley del holocausto de", libro_id = 3, capitulo = 7, versiculo = 37, relevancia = 6),
            VersiculoFamoso(frase_inicial = "la cual mando jehova a moises en", libro_id = 3, capitulo = 7, versiculo = 38, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hablo jehova a moises diciendo", libro_id = 3, capitulo = 8, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "toma a aaron y a sus hijos", libro_id = 3, capitulo = 8, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y reune toda la congregacion a la", libro_id = 3, capitulo = 8, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hizo pues moises como jehova le mando", libro_id = 3, capitulo = 8, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y dijo moises a la congregacion esto", libro_id = 3, capitulo = 8, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "entonces moises hizo acercarse a aaron y", libro_id = 3, capitulo = 8, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y puso sobre el la tunica y", libro_id = 3, capitulo = 8, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "luego le puso encima el pectoral y", libro_id = 3, capitulo = 8, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "despues puso la mitra sobre su cabeza", libro_id = 3, capitulo = 8, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y tomo moises el aceite de la", libro_id = 3, capitulo = 8, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y rocio de el sobre el altar", libro_id = 3, capitulo = 8, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y derramo del aceite de la uncion", libro_id = 3, capitulo = 8, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "despues moises hizo acercarse los hijos de", libro_id = 3, capitulo = 8, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "luego hizo traer el becerro de la", libro_id = 3, capitulo = 8, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y lo degollo y moises tomo la", libro_id = 3, capitulo = 8, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "despues tomo toda la grosura que estaba", libro_id = 3, capitulo = 8, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mas el becerro su piel su carne", libro_id = 3, capitulo = 8, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "despues hizo que trajeran el carnero del", libro_id = 3, capitulo = 8, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y lo degollo y rocio moises la", libro_id = 3, capitulo = 8, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y corto el carnero en trozos y", libro_id = 3, capitulo = 8, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "lavo luego con agua los intestinos y", libro_id = 3, capitulo = 8, versiculo = 21, relevancia = 6),
            VersiculoFamoso(frase_inicial = "despues hizo que trajeran el otro carnero", libro_id = 3, capitulo = 8, versiculo = 22, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y lo degollo y tomo moises de", libro_id = 3, capitulo = 8, versiculo = 23, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hizo acercarse luego los hijos de aaron", libro_id = 3, capitulo = 8, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "despues tomo la grosura la cola toda", libro_id = 3, capitulo = 8, versiculo = 25, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y del canastillo de los panes sin", libro_id = 3, capitulo = 8, versiculo = 26, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y lo puso todo en las manos", libro_id = 3, capitulo = 8, versiculo = 27, relevancia = 6),
            VersiculoFamoso(frase_inicial = "despues tomo aquellas cosas moises de las", libro_id = 3, capitulo = 8, versiculo = 28, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y tomo moises el pecho y lo", libro_id = 3, capitulo = 8, versiculo = 29, relevancia = 6),
            VersiculoFamoso(frase_inicial = "luego tomo moises del aceite de la", libro_id = 3, capitulo = 8, versiculo = 30, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y dijo moises a aaron y a", libro_id = 3, capitulo = 8, versiculo = 31, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y lo que sobre de la carne", libro_id = 3, capitulo = 8, versiculo = 32, relevancia = 6),
            VersiculoFamoso(frase_inicial = "de la puerta del tabernaculo de reunion", libro_id = 3, capitulo = 8, versiculo = 33, relevancia = 6),
            VersiculoFamoso(frase_inicial = "de la manera que hoy se ha", libro_id = 3, capitulo = 8, versiculo = 34, relevancia = 6),
            VersiculoFamoso(frase_inicial = "a la puerta pues del tabernaculo de", libro_id = 3, capitulo = 8, versiculo = 35, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y aaron y sus hijos hicieron todas", libro_id = 3, capitulo = 8, versiculo = 36, relevancia = 6),
            VersiculoFamoso(frase_inicial = "en el dia octavo moises llamo a", libro_id = 3, capitulo = 9, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y dijo a aaron toma de la", libro_id = 3, capitulo = 9, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y a los hijos de israel hablaras", libro_id = 3, capitulo = 9, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "asimismo un buey y un carnero para", libro_id = 3, capitulo = 9, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y llevaron lo que mando moises delante", libro_id = 3, capitulo = 9, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "entonces moises dijo esto es lo que", libro_id = 3, capitulo = 9, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y dijo moises a aaron acercate al", libro_id = 3, capitulo = 9, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "entonces se acerco aaron al altar y", libro_id = 3, capitulo = 9, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y los hijos de aaron le trajeron", libro_id = 3, capitulo = 9, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "e hizo arder sobre el altar la", libro_id = 3, capitulo = 9, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "mas la carne y la piel las", libro_id = 3, capitulo = 9, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "degollo asimismo el holocausto y los hijos", libro_id = 3, capitulo = 9, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "despues le presentaron el holocausto pieza por", libro_id = 3, capitulo = 9, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "luego lavo los intestinos y las piernas", libro_id = 3, capitulo = 9, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "ofrecio tambien la ofrenda del pueblo y", libro_id = 3, capitulo = 9, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y ofrecio el holocausto e hizo segun", libro_id = 3, capitulo = 9, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "ofrecio asimismo la ofrenda y lleno de", libro_id = 3, capitulo = 9, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "degollo tambien el buey y el carnero", libro_id = 3, capitulo = 9, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y las grosuras del buey y del", libro_id = 3, capitulo = 9, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y pusieron las grosuras sobre los pechos", libro_id = 3, capitulo = 9, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero los pechos con la espaldilla derecha", libro_id = 3, capitulo = 9, versiculo = 21, relevancia = 6),
            VersiculoFamoso(frase_inicial = "despues alzo aaron sus manos hacia el", libro_id = 3, capitulo = 9, versiculo = 22, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y entraron moises y aaron en el", libro_id = 3, capitulo = 9, versiculo = 23, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y salio fuego de delante de jehova", libro_id = 3, capitulo = 9, versiculo = 24, relevancia = 6),
            VersiculoFamoso(frase_inicial = "adab y abiu hijos de aaron tomaron", libro_id = 3, capitulo = 10, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y salio fuego de delante de jehova", libro_id = 3, capitulo = 10, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "entonces dijo moises a aaron esto es", libro_id = 3, capitulo = 10, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y llamo moises a misael y a", libro_id = 3, capitulo = 10, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y ellos se acercaron y los sacaron", libro_id = 3, capitulo = 10, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "entonces moises dijo a aaron y a", libro_id = 3, capitulo = 10, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "ni saldreis de la puerta del tabernaculo", libro_id = 3, capitulo = 10, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y jehova hablo a aaron diciendo", libro_id = 3, capitulo = 10, versiculo = 8, relevancia = 6),
            VersiculoFamoso(frase_inicial = "tu y tus hijos contigo no bebereis", libro_id = 3, capitulo = 10, versiculo = 9, relevancia = 6),
            VersiculoFamoso(frase_inicial = "para poder discernir entre lo santo y", libro_id = 3, capitulo = 10, versiculo = 10, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y para ensenar a los hijos de", libro_id = 3, capitulo = 10, versiculo = 11, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y moises dijo a aaron y a", libro_id = 3, capitulo = 10, versiculo = 12, relevancia = 6),
            VersiculoFamoso(frase_inicial = "la comereis pues en lugar santo porque", libro_id = 3, capitulo = 10, versiculo = 13, relevancia = 6),
            VersiculoFamoso(frase_inicial = "comereis asimismo en lugar limpio tu y", libro_id = 3, capitulo = 10, versiculo = 14, relevancia = 6),
            VersiculoFamoso(frase_inicial = "con las ofrendas de las grosuras que", libro_id = 3, capitulo = 10, versiculo = 15, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y moises pregunto por el macho cabrio", libro_id = 3, capitulo = 10, versiculo = 16, relevancia = 6),
            VersiculoFamoso(frase_inicial = "por que no comisteis la expiacion en", libro_id = 3, capitulo = 10, versiculo = 17, relevancia = 6),
            VersiculoFamoso(frase_inicial = "ved que la sangre no fue llevada", libro_id = 3, capitulo = 10, versiculo = 18, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y respondio aaron a moises he aqui", libro_id = 3, capitulo = 10, versiculo = 19, relevancia = 6),
            VersiculoFamoso(frase_inicial = "y cuando moises oyo esto se dio", libro_id = 3, capitulo = 10, versiculo = 20, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hablo jehova a moises y a aaron", libro_id = 3, capitulo = 11, versiculo = 1, relevancia = 6),
            VersiculoFamoso(frase_inicial = "hablad a los hijos de israel y", libro_id = 3, capitulo = 11, versiculo = 2, relevancia = 6),
            VersiculoFamoso(frase_inicial = "de entre los animales todo el que", libro_id = 3, capitulo = 11, versiculo = 3, relevancia = 6),
            VersiculoFamoso(frase_inicial = "pero de los que rumian o que", libro_id = 3, capitulo = 11, versiculo = 4, relevancia = 6),
            VersiculoFamoso(frase_inicial = "tambien el conejo porque rumia pero no", libro_id = 3, capitulo = 11, versiculo = 5, relevancia = 6),
            VersiculoFamoso(frase_inicial = "asimismo la liebre porque rumia pero no", libro_id = 3, capitulo = 11, versiculo = 6, relevancia = 6),
            VersiculoFamoso(frase_inicial = "tambien el cerdo porque tiene pezunas y", libro_id = 3, capitulo = 11, versiculo = 7, relevancia = 6),
            VersiculoFamoso(frase_inicial = "de la carne de ellos no comereis", libro_id = 3, capitulo = 11, versiculo = 8, relevancia = 6),
        )
        versiculoFamosoDao.insertAll(batch6)
    }

    suspend fun getVersiculosDesdeJson(
        archivoJson: String,
        libroId: Int,
        capitulo: Int
    ): List<Versiculo> = withContext(Dispatchers.IO) {
        try {
            val json = context.assets.open(archivoJson).bufferedReader().use { it.readText() }
            val gson = Gson()
            val type = object : TypeToken<BibliaJsonData>() {}.type
            val data: BibliaJsonData = gson.fromJson(json, type)

            data.versiculos
                .filter { it.libro_id == libroId && it.capitulo == capitulo }
                .sortedBy { it.versiculo }
                .map { v ->
                    val textoLimpio = v.texto.replace("/n", " ").replace(Regex("  +"), " ").trim()
                    Versiculo(
                        id           = v.id,
                        libro_id     = v.libro_id,
                        capitulo     = v.capitulo,
                        versiculo    = v.versiculo,
                        texto        = textoLimpio,
                        textoBusqueda = BibliaParser.normalizar(textoLimpio)
                    )
                }
        } catch (e: Exception) {
            Log.e("BibliaRepo", "Error leyendo $archivoJson: ${e.message}", e)
            throw e
        }
    }





    // ============ QUERIES ============

    fun getAllLibros(): Flow<List<Libro>> = libroDao.getAllLibros()

    suspend fun getLibroById(id: Int): Libro? = withContext(Dispatchers.IO) {
        libroDao.getLibroById(id)
    }

    fun getVersiculos(libroId: Int, capitulo: Int): Flow<List<Versiculo>> =
        versiculoDao.getVersiculos(libroId, capitulo)

    suspend fun getVersiculo(libroId: Int, capitulo: Int, versiculoNum: Int): Versiculo? =
        withContext(Dispatchers.IO) {
            versiculoDao.getVersiculo(libroId, capitulo, versiculoNum)
        }

    suspend fun getPrimerVersiculo(libroId: Int): Versiculo? = withContext(Dispatchers.IO) {
        versiculoDao.getPrimerVersiculoDeLibro(libroId)
    }

    suspend fun getCapitulosByLibro(libroId: Int): List<Int> = withContext(Dispatchers.IO) {
        versiculoDao.getCapitulosByLibro(libroId)
    }

    // ============ BÚSQUEDA DE VOZ ============

    suspend fun buscarPorReferencia(libroId: Int, capitulo: Int, versiculoNum: Int): Versiculo? =
        withContext(Dispatchers.IO) {
            versiculoDao.getVersiculo(libroId, capitulo, versiculoNum)
        }

    suspend fun buscarVersiculoFamoso(texto: String): List<Pair<VersiculoFamoso, Versiculo?>> =
        withContext(Dispatchers.IO) {

            // ── Palabras del texto hablado (ignorar palabras muy cortas: artículos, etc.) ──
            val palabrasTexto = texto.trim()
                .split("\\s+".toRegex())
                .map { it.trim() }
                .filter { it.length >= 3 }  // ignorar "de", "a", "el", "al", etc.

            if (palabrasTexto.isEmpty()) return@withContext emptyList()

            val todosFamosos = versiculoFamosoDao.getAll()

            // ── Puntuar cada versículo famoso según cuántas palabras del texto coinciden ──
            val conPuntaje = todosFamosos.mapNotNull { famoso ->
                val palabrasFrase = famoso.frase_inicial.trim()
                    .split("\\s+".toRegex())
                    .map { it.trim() }
                    .filter { it.length >= 3 }

                if (palabrasFrase.isEmpty()) return@mapNotNull null

                // Cuántas palabras del texto hablado están en la frase inicial
                val coincidencias = palabrasTexto.count { palabraHablada ->
                    palabrasFrase.any { palabraFrase ->
                        palabraFrase == palabraHablada ||
                                palabraFrase.startsWith(palabraHablada) ||
                                palabraHablada.startsWith(palabraFrase)
                    }
                }

                // Porcentaje de palabras del texto que coincidieron
                val porcentaje = coincidencias.toFloat() / palabrasTexto.size.toFloat()

                // Umbral: al menos 60% de las palabras habladas deben coincidir
                // Ej: texto = "porque de tal manera" (4 palabras útiles = 2 tras filtro cortas)
                //     frase  = "porque de tal manera amo dios al"
                //     coincide "porque", "tal", "manera" → 75% → pasa
                if (porcentaje >= 0.60f) {
                    Triple(famoso, coincidencias, porcentaje)
                } else {
                    null
                }
            }

            // Ordenar: primero más coincidencias, luego por relevancia del versículo
            val mejores = conPuntaje
                .sortedWith(compareByDescending<Triple<VersiculoFamoso, Int, Float>> { it.third }
                    .thenByDescending { it.first.relevancia })
                .take(3)
                .map { it.first }

            Log.d("BibliaRepo", "Famosos encontrados: ${mejores.size} para '$texto'")

            mejores.map { famoso ->
                val versiculo = versiculoDao.getVersiculo(famoso.libro_id, famoso.capitulo, famoso.versiculo)
                Pair(famoso, versiculo)
            }
        }

    suspend fun buscarPorContenido(query: String): List<Versiculo> = withContext(Dispatchers.IO) {
        try {
            val palabras = query.trim()
                .split(" ")
                .map { it.trim() }
                .filter { it.length >= 3 }

            if (palabras.isEmpty()) return@withContext emptyList()

            // Paso 1: frase completa (hasta 5 palabras) en texto_busqueda
            val fraseCorta = palabras.take(5).joinToString(" ")
            val resultadosFrase = versiculoDao.searchByContent(fraseCorta, 5)
            if (resultadosFrase.isNotEmpty()) {
                Log.d("BibliaRepo", "Encontrado por frase: '$fraseCorta'")
                return@withContext resultadosFrase
            }

            // Ordenar por longitud descendente (palabras más específicas primero)
            val palabrasOrdenadas = palabras.sortedByDescending { it.length }

            // Paso 2: 3 palabras más largas con AND
            if (palabrasOrdenadas.size >= 3) {
                val r = versiculoDao.searchByThreeWords(
                    palabrasOrdenadas[0], palabrasOrdenadas[1], palabrasOrdenadas[2], 5
                )
                if (r.isNotEmpty()) {
                    Log.d("BibliaRepo", "Encontrado con 3 palabras")
                    return@withContext r
                }
            }

            // Paso 3: 2 palabras más largas con AND
            if (palabrasOrdenadas.size >= 2) {
                val r = versiculoDao.searchByTwoWords(
                    palabrasOrdenadas[0], palabrasOrdenadas[1], 5
                )
                if (r.isNotEmpty()) {
                    Log.d("BibliaRepo", "Encontrado con 2 palabras")
                    return@withContext r
                }
            }

            // Paso 4: solo la palabra más larga
            Log.d("BibliaRepo", "Buscando con 1 palabra: \${palabrasOrdenadas[0]}")
            versiculoDao.searchByOneWord(palabrasOrdenadas[0], 5)

        } catch (e: Exception) {
            Log.e("BibliaRepo", "Error búsqueda contenido: \${e.message}")
            emptyList()
        }
    }

    suspend fun searchLibros(query: String): List<Libro> = withContext(Dispatchers.IO) {
        libroDao.searchLibros("%$query%")
    }

    // Igual que getVersiculosDesdeJson pero recibe el JSON ya leído como String
    // Usado cuando la versión fue descargada (no está en assets)
    suspend fun getVersiculosDesdeJsonString(
        jsonString: String,
        libroId: Int,
        capitulo: Int
    ): List<Versiculo> = withContext(Dispatchers.IO) {
        try {
            val gson = Gson()
            val type = object : TypeToken<BibliaJsonData>() {}.type
            val data: BibliaJsonData = gson.fromJson(jsonString, type)

            data.versiculos
                .filter { it.libro_id == libroId && it.capitulo == capitulo }
                .sortedBy { it.versiculo }
                .map { v ->
                    val textoLimpio = v.texto.replace("/n", " ").replace(Regex("  +"), " ").trim()
                    Versiculo(
                        id            = v.id,
                        libro_id      = v.libro_id,
                        capitulo      = v.capitulo,
                        versiculo     = v.versiculo,
                        texto         = textoLimpio,
                        textoBusqueda = BibliaParser.normalizar(textoLimpio)
                    )
                }
        } catch (e: Exception) {
            Log.e("BibliaRepo", "Error parseando JSON: ${e.message}", e)
            throw e
        }
    }

}

// Data classes para parsear el JSON
data class BibliaJsonData(
    val libros: List<LibroJson>,
    val versiculos: List<VersiculoJson>
)

data class LibroJson(
    val id: Int,
    val nombre: String,
    val abreviacion: String,
    val testamento: String,
    val orden: Int,
    val capitulos: Int
)

data class VersiculoJson(
    val id: Int,
    val libro_id: Int,
    val capitulo: Int,
    val versiculo: Int,
    val texto: String
)