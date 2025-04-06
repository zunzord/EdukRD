package com.edukrd.app.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.res.painterResource
import com.edukrd.app.R



@Composable
fun TopHeader(
    userName: String,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSettingsIconPosition: ((Offset) -> Unit)? = null,
    onLogoutIconPosition: ((Offset) -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        // Obtenemos posición ABSOLUTA (ventana) del ícono de Settings
                        val windowPosition = coordinates.localToWindow(Offset.Zero)
                        onSettingsIconPosition?.invoke(windowPosition)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EDUKRD",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            IconButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .size(40.dp)
                    .onGloballyPositioned { coordinates ->
                        // Obtenemos posición ABSOLUTA (ventana) del ícono de Logout
                        val windowPosition = coordinates.localToWindow(Offset.Zero)
                        onLogoutIconPosition?.invoke(windowPosition)
                    }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.logout), // Reemplaza "ic_logout" por el nombre de tu recurso
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
