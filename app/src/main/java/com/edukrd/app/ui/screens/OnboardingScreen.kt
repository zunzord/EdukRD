package com.edukrd.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            description = "Descubre un mundo de aprendizaje y beneficios.",
            imageResId = com.edukrd.app.R.drawable.onboarding_image1_round
        ),
        OnboardingPage(
            title = "Aprende de forma interactiva",
            description = "Accede a cursos diseñados para acompañarte.",
            imageResId = com.edukrd.app.R.drawable.onboarding_image2_round
        ),
        OnboardingPage(
            title = "Beneficios exclusivos",
            description = "Gana monedas, medallas y compite en el ranking.",
            imageResId = com.edukrd.app.R.drawable.onboarding_image3_round
        )
    )

    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe completion event
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

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                count = pages.size,
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                OnboardingPageContent(page = pages[page])
            }

            HorizontalPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            )

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
                Text(if (isLastPage) "Bienvenido a EdukRD" else "Siguiente")
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = page.imageResId),
            contentDescription = page.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = page.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = page.description, style = MaterialTheme.typography.bodyMedium)
    }
}
