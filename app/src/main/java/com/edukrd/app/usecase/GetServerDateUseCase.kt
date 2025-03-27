package com.edukrd.app.usecase

import com.edukrd.app.repository.TimeRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetServerDateUseCase @Inject constructor(
    private val timeRepository: TimeRepository
) {
    suspend operator fun invoke(): LocalDate {
        val offsetMillis = timeRepository.fetchServerOffsetMillis()
        val serverMillis = System.currentTimeMillis() + offsetMillis
        return Instant.ofEpochMilli(serverMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    /** Devuelve el offset bruto (ms) obtenido de Firebase Realtime Database */
    suspend fun invokeOffset(): Long {
        return timeRepository.fetchServerOffsetMillis()
    }
}
