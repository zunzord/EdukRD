package com.edukrd.app.viewmodel

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edukrd.app.usecase.GetServerDateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class CountdownViewModel @Inject constructor(
    private val getServerDateUseCase: GetServerDateUseCase
) : ViewModel() {

    private val _timeRemaining = MutableStateFlow("00:00:00")
    val timeRemaining: StateFlow<String> = _timeRemaining

    init {
        viewModelScope.launch {
            // Se obtiene el offset del servidor (en ms) mediante el caso de uso.
            val offset = getServerDateUseCase.invokeOffset()
            while (true) {
                val serverTimeMillis = System.currentTimeMillis() + offset

                // Convertir serverTimeMillis a LocalDateTime
                val zoneId = ZoneId.systemDefault()
                val serverInstant = Instant.ofEpochMilli(serverTimeMillis)
                val serverLocalDateTime = serverInstant.atZone(zoneId).toLocalDateTime()

                // Calcular la medianoche del siguiente día
                val nextMidnight = serverLocalDateTime.toLocalDate().plusDays(1).atStartOfDay()
                val nextMidnightMillis = nextMidnight.atZone(zoneId).toInstant().toEpochMilli()

                // Calcular la diferencia
                val remainingMillis = nextMidnightMillis - serverTimeMillis
                val hours = remainingMillis / (1000 * 60 * 60)
                val minutes = (remainingMillis / (1000 * 60)) % 60
                val seconds = (remainingMillis / 1000) % 60

                // Actualizamos el estado formateado en HH:MM:SS.
                _timeRemaining.value = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                delay(1000L)
            }
        }
    }
}

@Composable
fun CountdownTimer(modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    // Inyectamos el viewModel usando Hilt
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
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
