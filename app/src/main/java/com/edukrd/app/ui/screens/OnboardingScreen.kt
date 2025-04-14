package com.edukrd.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.edukrd.app.models.OnboardingPage
import com.edukrd.app.navigation.Screen
import com.edukrd.app.viewmodel.OnboardingViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    onboardingViewModel: OnboardingViewModel = hiltViewModel()
) {
    val pages = listOf(
        OnboardingPage(
            title = "Bienvenido a EdukRD",
            description = "",
            imageResId = com.edukrd.app.R.drawable.onboarding_image11_round
        ),
        OnboardingPage(
            title = "Aprende de forma interactiva",
            description = "",
            imageResId = com.edukrd.app.R.drawable.onboarding_image22_round
        ),
        OnboardingPage(
            title = "Beneficios exclusivos",
            description = "",
            imageResId = com.edukrd.app.R.drawable.onboarding_image33_round
        )
    )

    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observa el evento de finalización
    val completion by onboardingViewModel.complete.collectAsState(initial = null)
    LaunchedEffect(completion) {
        completion?.let { success ->
            if (success) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            } else {
                Toast.makeText(
                    context,
                    "Error completando onboarding. Contacta soporte.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // El Scaffold se encarga del layout global de la pantalla
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // HorizontalPager que ocupa la mayor parte de la pantalla
            HorizontalPager(
                count = pages.size,
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                OnboardingPageContent(page = pages[page])
            }

            // Indicador del pager
            HorizontalPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            )

            // Botón al final (solo se reserva el espacio suficiente para éste)
            val isLastPage = pagerState.currentPage == pages.lastIndex
            Button(
                onClick = {
                    if (isLastPage) onboardingViewModel.completeOnboarding()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = if (isLastPage) "Bienvenido a EdukRD" else "Siguiente")
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    // Usamos un Box para que la imagen ocupe todo el espacio disponible en cada página
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = page.imageResId),
            contentDescription = page.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit  // o ContentScale.Inside
        )
        // Si deseas agregar textos superpuestos a la imagen, podrías colocarlos aquí.
        // Por ejemplo, un título en la parte superior o inferior, con un fondo semi-transparente.
    }
}
