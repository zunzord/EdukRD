package com.edukrd.app.ui.screens.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.edukrd.app.viewmodel.CountdownViewModel

@Composable
fun CountdownTimer(modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    // Se inyecta el ViewModel usando Hilt. La lógica del contador reside en el CountdownViewModel.
    val viewModel: CountdownViewModel = hiltViewModel()
    val timeRemaining by viewModel.timeRemaining.collectAsState()

    Column(modifier = modifier) {
        Text(
            text = "Tiempo restante del día en EdukRD:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = timeRemaining,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}