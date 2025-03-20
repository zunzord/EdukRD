package com.edukrd.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import com.edukrd.app.utils.OnboardingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {
    private val onboardingManager = OnboardingManager(context)
    private val _isOnboardingCompleted = MutableStateFlow(onboardingManager.isOnboardingCompleted())
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted

    fun completeOnboarding() {
        onboardingManager.completeOnboarding()
        _isOnboardingCompleted.value = true
    }
}
