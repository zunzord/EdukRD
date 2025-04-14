package com.edukrd.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.edukrd.app.R
import com.edukrd.app.viewmodel.VerificationPendingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationPendingScreen(
    navController: NavController,
    email: String,
    viewModel: VerificationPendingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val resendState by viewModel.resendState.collectAsState()

    // Efecto para mostrar Toast según el estado de reenvío
    LaunchedEffect(resendState) {
        when (resendState) {
            is VerificationPendingViewModel.ResendState.Success -> {
                Toast.makeText(
                    context,
                    (resendState as VerificationPendingViewModel.ResendState.Success).message,
                    Toast.LENGTH_LONG
                ).show()
            }
            is VerificationPendingViewModel.ResendState.Error -> {
                Toast.makeText(
                    context,
                    (resendState as VerificationPendingViewModel.ResendState.Error).message,
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> { }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {})
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.verificacorreo),
                contentDescription = "Imagen de cabecera",
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentScale = ContentScale.Fit,
                alignment = Alignment.TopCenter
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "¡Cuenta creada!",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Te hemos enviado un correo de verificación a:")
            Text(
                text = email,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Revisa tu bandeja de entrada (o spam) y haz clic en el enlace para activar tu cuenta.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.resendVerificationEmail(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (resendState is VerificationPendingViewModel.ResendState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Reenviar correo de verificación")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = {
                navController.navigate("login") {
                    popUpTo("verification_pending") { inclusive = true }
                }
            }) {
                Text("Ir a Login")
            }
        }
    }
}
