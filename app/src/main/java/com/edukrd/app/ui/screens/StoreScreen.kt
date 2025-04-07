package com.edukrd.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.edukrd.app.models.StoreItem
import com.edukrd.app.ui.components.DotLoadingIndicator
import com.edukrd.app.ui.components.MotivationalBubble
import com.edukrd.app.viewmodel.StoreViewModel
import com.edukrd.app.viewmodel.UserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    navController: NavController
) {
    val storeViewModel: StoreViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()

    // se valida saldo monedas
    val coins by userViewModel.coins.collectAsState()

    // Observamos estados de StoreViewModel
    val availableItems by storeViewModel.availableItems.collectAsState()
    val redeemedItems by storeViewModel.redeemedItems.collectAsState()
    val receivedItems by storeViewModel.receivedItems.collectAsState()

    val loading by storeViewModel.loading.collectAsState()
    val error by storeViewModel.error.collectAsState()
    val redeemResult by storeViewModel.redeemResult.collectAsState()

    val scope = rememberCoroutineScope()

    // Tabs: 0 -> Disponibles, 1 -> Canjeados, 2 -> Recibidos
    var selectedTabIndex by remember { mutableStateOf(0) }

    // Cargar datos iniciales
    LaunchedEffect(Unit) {
        storeViewModel.loadAvailableItems()
        storeViewModel.loadRedeemedItems()
        storeViewModel.loadReceivedItems()
        userViewModel.loadCurrentUserData() // Para actualizar 'monedas'
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tienda", color = Color.White) },
                /*navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },*/
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
            MotivationalBubble(
                message = "CANJEA! \npulsa aquí para saber más!",
                detailedDescription = "Aquí podrás: verificar tus monedas, canjearlas por articulos disponibles. Además, puedes dar seguimiento a los articulos canjeados."
            )
        }

    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (loading) {
                // Indicador de carga
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    DotLoadingIndicator(modifier = Modifier.align(Alignment.Center).size(56.dp))
                }
            } else if (error != null) {
                // Mensaje de error
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = error!!, color = MaterialTheme.colorScheme.error)
                }
            } else {
                // Contenido principal
                Column(modifier = Modifier.fillMaxSize()) {
                    // Tabs para cada sección
                    val tabTitles = listOf("Disponibles", "Canjeados", "Recibidos")
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = (selectedTabIndex == index),
                                onClick = { selectedTabIndex = index },
                                text = { Text(title) }
                            )
                        }
                    }
                    // Contenido según tab
                    when (selectedTabIndex) {
                        0 -> {
                            // Artículos disponibles
                            StoreItemList(
                                items = availableItems,
                                showRedeemButton = true
                            ) { item ->
                                // Al canjear un artículo
                                scope.launch {
                                    storeViewModel.redeemItem(item)
                                    // Si se canjea, recargar userViewModel para reflejar monedas
                                    userViewModel.loadCurrentUserData()
                                }
                            }
                        }
                        1 -> {
                            // Artículos canjeados
                            StoreItemList(
                                items = redeemedItems,
                                showRedeemButton = false
                            ) {
                                // Sin acción de canjear
                            }
                        }
                        2 -> {
                            // Artículos recibidos
                            StoreItemList(
                                items = receivedItems,
                                showRedeemButton = false
                            ) {
                                // Sin acción de canjear
                            }
                        }
                    }
                }

                // Burbuja de monedero (arriba/derecha o abajo/derecha, a tu gusto)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(
                            text = "Monedero: $coins",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    // Mostrar resultado de canje si existe
    redeemResult?.let { result ->
        if (result.first) {
            // Canje exitoso
            // Podrías mostrar un Snackbar o un AlertDialog
            // A modo de ejemplo, un AlertDialog
            AlertDialog(
                onDismissRequest = { /* Podrías limpiar el estado en StoreViewModel */ },
                title = { Text("Canje Exitoso") },
                text = { Text(result.second) },
                confirmButton = {
                    Button(onClick = {
                        // Limpiar el estado de _redeemResult
                        // (Podrías exponer un método en StoreViewModel para resetearlo)
                        scope.launch {
                            // Ejemplo rápido:
                            storeViewModel.resetRedeemResult()
                        }
                    }) {
                        Text("Ok")
                    }
                }
            )
        } else if (result.first == false && result.second.isNotBlank()) {
            // Error en el canje
            AlertDialog(
                onDismissRequest = { /* Igualmente podrías resetear el estado */ },
                title = { Text("Error al canjear") },
                text = { Text(result.second) },
                confirmButton = {
                    Button(onClick = {
                        scope.launch {
                            storeViewModel.resetRedeemResult()
                        }
                    }) {
                        Text("Ok")
                    }
                }
            )
        }
    }
}

/**
 * Lista genérica de artículos de la tienda. Muestra un botón "Canjear" si showRedeemButton = true.
 */
@Composable
fun StoreItemList(
    items: List<StoreItem>,
    showRedeemButton: Boolean,
    onRedeemClick: (StoreItem) -> Unit
) {
    if (items.isEmpty()) {
        // Mensaje si la lista está vacía
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No hay artículos en esta sección", color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                StoreItemCard(item, showRedeemButton) {
                    onRedeemClick(item)
                }
            }
        }
    }
}

/**
 * Tarjeta de artículo de la tienda.
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
            // Imagen
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.imageUrl)
                    .build(),
                contentDescription = item.title,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            // Texto principal
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Precio: ${item.price} monedas",
                    style = MaterialTheme.typography.bodySmall
                )
                if (item.stock > 0) {
                    Text(
                        text = "Stock: ${item.stock}",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "Agotado",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red
                    )
                }
            }

            // Botón de canje (opcional)
            if (showRedeemButton && item.available && item.stock > 0) {
                Button(onClick = onRedeemClick) {
                    Text("Canjear")
                }
            }
        }
    }

}
