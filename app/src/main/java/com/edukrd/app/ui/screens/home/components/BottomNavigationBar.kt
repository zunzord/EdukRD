package com.edukrd.app.ui.screens.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.edukrd.app.R

@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String,
    onMedalsIconPosition: (Offset) -> Unit, // Quitamos "? = null" (parámetro obligatorio)
    onStoreIconPosition: (Offset) -> Unit,
    onRankingIconPosition: (Offset) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = { if (currentRoute != "home") navController.navigate("home") },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_course),
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") },
            alwaysShowLabel = false
        )
        NavigationBarItem(
            selected = currentRoute == "medals",
            onClick = { if (currentRoute != "medals") navController.navigate("medals") },
            icon = {
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            // Obtenemos posición absoluta en la pantalla
                            val localPosition = coordinates.positionInRoot()
                            onMedalsIconPosition(coordinates.localToWindow(localPosition))
                        }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_medal),
                        contentDescription = "Medals"
                    )
                }
            },
            label = { Text("Medals") },
            alwaysShowLabel = false
        )
        NavigationBarItem(
            selected = currentRoute == "store",
            onClick = { if (currentRoute != "store") navController.navigate("store") },
            icon = {
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            val localPosition = coordinates.positionInRoot()
                            onStoreIconPosition(coordinates.localToWindow(localPosition))
                        }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_store),
                        contentDescription = "Store"
                    )
                }
            },
            label = { Text("Store") },
            alwaysShowLabel = false
        )
        NavigationBarItem(
            selected = currentRoute == "ranking",
            onClick = { if (currentRoute != "ranking") navController.navigate("ranking") },
            icon = {
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            val localPosition = coordinates.positionInRoot()
                            onRankingIconPosition(coordinates.localToWindow(localPosition))
                        }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ranking),
                        contentDescription = "Ranking"
                    )
                }
            },
            label = { Text("Ranking") },
            alwaysShowLabel = false
        )
    }
}