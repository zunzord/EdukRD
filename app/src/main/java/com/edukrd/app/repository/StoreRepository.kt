package com.edukrd.app.repository

import com.edukrd.app.models.StoreItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    /**
     * Obtiene la lista de artículos disponibles de la tienda.
     * Se filtran por "available" = true y stock mayor que 0.
     */
    suspend fun getAvailableItems(): List<StoreItem> {
        return try {
            val snapshot = firestore.collection("store")
                .whereEqualTo("available", true)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(StoreItem::class.java)?.copy(id = doc.id)
            }.filter { it.stock > 0 }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Obtiene la lista de artículos canjeados por el usuario actual (leyendo en users/{uid}/redeemed).
     */
    suspend fun getRedeemedItems(): List<StoreItem> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("redeemed")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(StoreItem::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Obtiene la lista de artículos recibidos por el usuario actual (leyendo en users/{uid}/received).
     */
    suspend fun getReceivedItems(): List<StoreItem> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("received")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(StoreItem::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Realiza la transacción de canje de un artículo para el usuario actual.
     * No se pasa userId como parámetro; se obtiene internamente de FirebaseAuth.
     *
     * - Verifica stock y disponibilidad del artículo.
     * - Verifica monedas del usuario.
     * - Decrementa stock, descuenta monedas.
     * - Crea registro de canje en "users/{uid}/redeemed/{itemId}" y en "store/{itemId}/redeemed/{transactionId}".
     * - Retorna Triple(exito, mensaje, itemActualizado).
     */
    suspend fun redeemItemTransaction(item: StoreItem): Triple<Boolean, String, StoreItem?> {
        val uid = auth.currentUser?.uid ?: return Triple(false, "Usuario no autenticado", null)

        return try {
            val updatedItem = firestore.runTransaction { transaction ->
                val itemRef = firestore.collection("store").document(item.id)
                val userRef = firestore.collection("users").document(uid)
                val redeemedRef = userRef.collection("redeemed").document(item.id)

                // Subcolección en el artículo con un nuevo documento (transacción)
                val storeItemRedeemedRef = itemRef.collection("redeemed").document()
                val transactionId = storeItemRedeemedRef.id

                // Leer datos actuales del artículo
                val snapshotItem = transaction.get(itemRef)
                val currentStock = snapshotItem.getLong("stock")?.toInt() ?: 0
                val currentAvailable = snapshotItem.getBoolean("available") ?: false
                val currentPrice = snapshotItem.getLong("price")?.toInt() ?: 0

                if (!currentAvailable || currentStock <= 0) {
                    throw Exception("Artículo agotado o no disponible.")
                }

                // Leer datos actuales del usuario
                val snapshotUser = transaction.get(userRef)
                val userCoins = snapshotUser.getLong("coins")?.toInt() ?: 0
                if (userCoins < currentPrice) {
                    throw Exception("No tienes suficientes monedas para canjear este artículo.")
                }

                // Calcular nuevos valores
                val newStock = currentStock - 1
                val newAvailable = newStock > 0

                // Actualizar el stock y disponible en una sola operación para cumplir con las reglas de Firestore
                val updateMap = mutableMapOf<String, Any>(
                    "stock" to newStock,
                    "available" to if (!newAvailable) false else currentAvailable
                )
                transaction.update(itemRef, updateMap)

                // Actualizar las monedas del usuario (decrementar en currentPrice)
                transaction.update(
                    userRef,
                    "coins",
                    FieldValue.increment((-currentPrice).toLong())
                )

                val now = com.google.firebase.Timestamp.now()

                // Registrar el canje en la subcolección "redeemed" del usuario
                transaction.set(
                    redeemedRef,
                    mapOf(
                        "transactionId" to transactionId,
                        "title" to item.title,
                        "description" to item.description,
                        "imageUrl" to item.imageUrl,
                        "price" to currentPrice,
                        "available" to newAvailable,
                        "stock" to newStock,
                        "redeemedAt" to now
                    )
                )

                // Registrar el canje en la subcolección "redeemed" del artículo
                transaction.set(
                    storeItemRedeemedRef,
                    mapOf(
                        "transactionId" to transactionId,
                        "userId" to uid,
                        "price" to currentPrice,
                        "timestamp" to now
                    )
                )

                // Retornar el artículo actualizado
                item.copy(stock = newStock, available = newAvailable)
            }.await()

            Triple(true, "Artículo canjeado exitosamente.", updatedItem)
        } catch (e: Exception) {
            Triple(false, e.message ?: "Error desconocido", null)
        }
    }
}
