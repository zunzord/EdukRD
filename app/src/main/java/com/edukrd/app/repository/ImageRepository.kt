package com.edukrd.app.repository

import com.edukrd.app.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
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

    // URL para listar objetos en la carpeta "resources" del bucket "edukrd-resources"
    private val listUrl = "https://$projectRef.supabase.co/storage/v1/object/list/edukrd-resources/resources/carrucel/"

    /**
     * Obtiene la lista de URLs públicas de las imágenes alojadas en Supabase Storage.
     * Se parsea directamente la respuesta como una lista de StorageObject.
     */
    suspend fun getImageUrls(): List<String> {
        return try {
            // La API devuelve directamente un arreglo JSON de StorageObject.
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
