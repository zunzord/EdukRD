// GetDeviceIdUseCase.kt
package com.edukrd.app.usecase

import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetDeviceIdUseCase @Inject constructor() {
    suspend operator fun invoke(): String {
        return FirebaseInstallations.getInstance().id.await()
    }
}
