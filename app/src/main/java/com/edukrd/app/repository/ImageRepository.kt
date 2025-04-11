package com.edukrd.app.repository

import android.util.Log
import com.edukrd.app.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepository @Inject constructor(
    private val httpClient: HttpClient
) {
    private val projectRef = BuildConfig.SUPABASE_PROJECT_REF  // Ejemplo: "riizanzcweznaxcmavpy"
    private val supabaseKey = BuildConfig.SUPABASE_PUBLIC_ANON_KEY
    // Variable para cachear la lista de URLs ya obtenida
    private var cachedImageUrls: List<String>? = null

    /**
     * Obtiene la lista de URLs de las imágenes del banner que existen en la carpeta
     * "resources/carrucel", siguiendo la nomenclatura incremental (fondo1.png, fondo2.png, ...).
     * Se detiene cuando la respuesta HTTP no es exitosa o se alcanza un límite máximo de intentos.
     */
    suspend fun getIncrementalImageUrls(): List<String> {
        // Si ya se obtuvo la lista, la retornamos (cache)
        cachedImageUrls?.let { return it }

        val result = mutableListOf<String>()
        var index = 1
        val maxAttempts = 100  // Límite de seguridad para evitar bucles infinitos
        while (index <= maxAttempts) {
            val imageUrl =
                "https://$projectRef.supabase.co/storage/v1/object/public/edukrd-resources/resources/carrucel/fondo${index}.png"
            // Intentamos validar la existencia del archivo usando una solicitud HEAD.
            val exists = withTimeoutOrNull(3000L) {  // Timeout de 3 segundos para cada intento
                try {
                    val response: HttpResponse = httpClient.head(imageUrl) {
                        header("apikey", supabaseKey)
                        header("Authorization", "Bearer $supabaseKey")
                    }
                    // Si el código de estado es 2xx, se asume que el archivo existe.
                    response.status.value in 200..299
                } catch (e: Exception) {
                    Log.e("ImageRepository", "Error verificando $imageUrl: ${e.message}")
                    false
                }
            } ?: false

            if (exists) {
                result.add(imageUrl)
                Log.d("ImageRepository", "Archivo encontrado: $imageUrl")
                index++
            } else {
                Log.d("ImageRepository", "No se encontró archivo con fondo$index.png; se detiene el bucle.")
                break
            }
        }
        cachedImageUrls = result
        return result
    }
}





/*package com.edukrd.app.repository

import android.util.Log
import com.edukrd.app.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class StorageObject(val name: String)

@Singleton
class ImageRepository @Inject constructor(
    private val httpClient: HttpClient
) {
    private val projectRef = BuildConfig.SUPABASE_PROJECT_REF
    private val supabaseKey = BuildConfig.SUPABASE_PUBLIC_ANON_KEY

    // URL para listar objetos usando el parámetro 'prefix'
    private val listUrl = "https://$projectRef.supabase.co/storage/v1/object/list/edukrd-resources?prefix=resources/carrucel/"

    /**
     * Obtiene la lista de URLs públicas de las imágenes alojadas en Supabase Storage.
     * Se parsea directamente la respuesta como una lista de StorageObject.
     */
    suspend fun getImageUrls(): List<String> {
        return try {
            Log.d("ImageRepository", "Project Ref: $projectRef")
            // Para depuración: mostramos la respuesta cruda
            val responseText: String = httpClient.get(listUrl) {
                header("apikey", supabaseKey)
            }.bodyAsText()
            Log.d("ImageRepository", "Respuesta cruda: $responseText")

            // Parseamos la respuesta a una lista de StorageObject
            val response: List<StorageObject> = httpClient.get(listUrl) {
                header("apikey", supabaseKey)
            }.body()

            response.map { obj ->
                "https://$projectRef.supabase.co/storage/v1/object/public/edukrd-resources/resources/carrucel/${obj.name}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

 */
