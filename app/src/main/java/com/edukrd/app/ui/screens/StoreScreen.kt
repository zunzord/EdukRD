package com.edukrd.app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.edukrd.app.models.StoreItem
import com.edukrd.app.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid

    // Colores principales
    val dominicanBlue = Color(0xFF1565C0)
    val dominicanRed = Color(0xFFD32F2F)

    // Estados para la UI
    var userCoins by remember { mutableStateOf(0) }
    var availableItems by remember { mutableStateOf<List<StoreItem>>(emptyList()) }
    var redeemedItems by remember { mutableStateOf<List<StoreItem>>(emptyList()) }
    var receivedItems by remember { mutableStateOf<List<StoreItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Estado de la pestaña seleccionada
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Artículos Disponibles", "Artículos Canjeados", "Artículos Recibidos")

    // Corutina para funciones suspend
    val scope = rememberCoroutineScope()

    // Cargar datos de usuario y artículos
    LaunchedEffect(userId) {
        if (userId == null) {
            errorMessage = "Usuario no autenticado."
            loading = false
            return@LaunchedEffect
        }
        try {
            // Leer monedas del usuario
            val userDoc = db.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                userCoins = userDoc.getLong("coins")?.toInt() ?: 0
            }

            // Leer artículos disponibles
            val snapAvailable = db.collection("store")
                .whereEqualTo("available", true)
                .get().await()
            val listAvail = snapAvailable.documents.mapNotNull { doc ->
                doc.toObject(StoreItem::class.java)?.copy(id = doc.id)
            }.filter { it.stock > 0 }
            availableItems = listAvail

            // Leer artículos canjeados (subcolección "redeemed")
            val snapRedeemed = db.collection("users")
                .document(userId)
                .collection("redeemed")
                .get().await()
            val listRedeemed = snapRedeemed.documents.mapNotNull { doc ->
                doc.toObject(StoreItem::class.java)?.copy(id = doc.id)
            }
            redeemedItems = listRedeemed

            // Leer artículos recibidos (subcolección "received")
            val snapReceived = db.collection("users")
                .document(userId)
                .collection("received")
                .get().await()
            val listReceived = snapReceived.documents.mapNotNull { doc ->
                doc.toObject(StoreItem::class.java)?.copy(id = doc.id)
            }
            receivedItems = listReceived

        } catch (e: Exception) {
            errorMessage = "Error al cargar datos: ${e.message}"
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tienda", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = topAppBarColors(containerColor = dominicanBlue)
            )

        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // TabRow con 3 secciones
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = (selectedTabIndex == index),
                                onClick = { selectedTabIndex = index },
                                text = { Text(title, fontSize = 14.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    when (selectedTabIndex) {
                        0 -> {
                            // Sección de artículos disponibles
                            if (availableItems.isEmpty()) {
                                Text("No hay artículos disponibles", modifier = Modifier.padding(16.dp))
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(availableItems) { item ->
                                        StoreItemCard(
                                            item = item,
                                            showRedeemButton = true,
                                            onRedeemClick = {
                                                // Llamar a la corutina para canjear
                                                scope.launch {
                                                    redeemItem(
                                                        userId = userId!!,
                                                        item = item,
                                                        db = db
                                                    ) { success, msg, newCoins, updatedItem ->
                                                        if (success) {
                                                            // Actualizar monedas
                                                            userCoins = newCoins
                                                            // Actualizar la lista local
                                                            if (!updatedItem.available || updatedItem.stock <= 0) {
                                                                // Quitar el artículo de la lista
                                                                availableItems = availableItems.filter { it.id != item.id }
                                                            } else {
                                                                // Actualizar stock local
                                                                availableItems = availableItems.map {
                                                                    if (it.id == item.id) updatedItem else it
                                                                }
                                                            }
                                                            Log.d("StoreScreen", "Canje exitoso: $msg")
                                                        } else {
                                                            Log.e("StoreScreen", "Error al canjear: $msg")
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Sección de artículos canjeados
                            if (redeemedItems.isEmpty()) {
                                Text("No has canjeado artículos", modifier = Modifier.padding(16.dp))
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(redeemedItems) { item ->
                                        StoreItemCard(
                                            item = item,
                                            showRedeemButton = false,
                                            onRedeemClick = {}
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Sección de artículos recibidos
                            if (receivedItems.isEmpty()) {
                                Text("No tienes artículos recibidos", modifier = Modifier.padding(16.dp))
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(receivedItems) { item ->
                                        StoreItemCard(
                                            item = item,
                                            showRedeemButton = false,
                                            onRedeemClick = {}
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Burbuja de monedero
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = dominicanRed),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = "Monedero: $userCoins",
                        color = Color.White,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

/**
 * Muestra la tarjeta de un artículo. Si showRedeemButton = true, aparece el botón "Canjear".
 */
@Composable
fun StoreItemCard(
    item: StoreItem,
    showRedeemButton: Boolean,
    onRedeemClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = item.description, style = MaterialTheme.typography.bodySmall)
                Text(text = "Precio: ${item.price} monedas", style = MaterialTheme.typography.bodySmall)
                if (item.stock > 0) {
                    Text(text = "Stock: ${item.stock}", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(text = "Agotado", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                }
            }
            if (showRedeemButton && item.available && item.stock > 0) {
                Button(onClick = onRedeemClick) {
                    Text("Canjear")
                }
            }
        }
    }
}

/**
 * Función suspend que realiza la transacción de canje en Firestore.
 */
suspend fun redeemItem(
    userId: String,
    item: StoreItem,
    db: FirebaseFirestore,
    onResult: (success: Boolean, message: String, newCoins: Int, updatedItem: StoreItem) -> Unit
) {
    try {
        db.runTransaction { transaction ->
            val itemRef = db.collection("store").document(item.id)
            val userRef = db.collection("users").document(userId)
            val redeemedRef = userRef.collection("redeemed").document(item.id)

            // Leer artículo
            val snapshotItem = transaction.get(itemRef)
            val currentStock = snapshotItem.getLong("stock")?.toInt() ?: 0
            val currentAvailable = snapshotItem.getBoolean("available") ?: false
            val currentPrice = snapshotItem.getLong("price")?.toInt() ?: 0

            if (!currentAvailable || currentStock <= 0) {
                throw Exception("Artículo agotado o no disponible.")
            }

            // Leer usuario
            val snapshotUser = transaction.get(userRef)
            val userCoins = snapshotUser.getLong("coins")?.toInt() ?: 0
            if (userCoins < currentPrice) {
                throw Exception("No tienes suficientes monedas para canjear este artículo.")
            }

            // Decrementar stock
            val newStock = currentStock - 1
            transaction.update(itemRef, "stock", newStock)
            // Si llega a 0, marcar available = false
            if (newStock <= 0) {
                transaction.update(itemRef, "available", false)
            }

            // Descontar monedas
            val newCoins = userCoins - currentPrice
            transaction.update(userRef, "coins", newCoins)

            // Registrar en subcolección "redeemed"
            transaction.set(
                redeemedRef,
                mapOf(
                    "title" to item.title,
                    "description" to item.description,
                    "imageUrl" to item.imageUrl,
                    "price" to currentPrice,
                    "available" to (newStock > 0),
                    "stock" to newStock,
                    "redeemedAt" to FieldValue.serverTimestamp()
                )
            )

            // Retornar el resultado
            onResult(
                true,
                "Artículo canjeado exitosamente.",
                newCoins,
                item.copy(stock = newStock, available = (newStock > 0))
            )
        }.await()
    } catch (e: Exception) {
        onResult(false, e.message ?: "Error desconocido", 0, item)
    }
}
