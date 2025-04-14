package com.edukrd.app.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.edukrd.app.R
import com.edukrd.app.models.User
import com.edukrd.app.navigation.Screen
import com.edukrd.app.viewmodel.AuthResult
import com.edukrd.app.viewmodel.AuthViewModel
import com.edukrd.app.viewmodel.UserViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController) {
    // Inyección de dependencias de los ViewModels
    val authViewModel: AuthViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val context = LocalContext.current

    // Estados para los campos del formulario y sus errores
    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    var lastName by remember { mutableStateOf("") }
    var lastNameError by remember { mutableStateOf(false) }

    var birthDate by remember { mutableStateOf("") }
    var birthDateError by remember { mutableStateOf(false) }

    var sector by remember { mutableStateOf("") }
    var sectorError by remember { mutableStateOf(false) }

    var phone by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }

    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    var confirmPassword by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf(false) }

    var themePreference by remember { mutableStateOf("light") }

    // Flag para evitar múltiples lanzamientos del DatePicker
    var isDatePickerShown by remember { mutableStateOf(false) }

    // Configuración del DatePicker para seleccionar fecha
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            // Se formatea la fecha en "DD/MM/AAAA"
            val formattedDay = if (selectedDay < 10) "0$selectedDay" else "$selectedDay"
            val formattedMonth = if ((selectedMonth + 1) < 10) "0${selectedMonth + 1}" else "${selectedMonth + 1}"
            birthDate = "$formattedDay/$formattedMonth/$selectedYear"
            birthDateError = false // Limpia error al seleccionar fecha válida
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    // Al cerrar el DatePicker, se reinicia el flag.
    datePickerDialog.setOnDismissListener { isDatePickerShown = false }

    // Función para autollenar el campo de fecha con formato "DD/MM/YYYY"
    // Se remueven caracteres no numéricos y se insertan los '/' en los lugares correspondientes.
    // Si después de la segunda barra solo se ingresan dos dígitos, se convierten automáticamente en un año de 4 dígitos.
    fun formatBirthDateInput(input: String): Pair<String, Boolean> {
        try {
            // Filtra sólo dígitos
            val digits = input.filter { it.isDigit() }
            // Limita a 8 dígitos (DDMMYYYY o DDMMYY)
            val limited = if (digits.length > 8) digits.substring(0, 8) else digits
            val sb = StringBuilder()
            // Día (2 dígitos)
            if (limited.length >= 2) {
                sb.append(limited.substring(0, 2))
            } else {
                sb.append(limited)
                return Pair(sb.toString(), false)
            }
            // Agrega barra si hay más dígitos
            if (limited.length >= 3) {
                sb.append("/")
            }
            // Mes (2 dígitos)
            if (limited.length >= 4) {
                sb.append(limited.substring(2, 4))
            } else if (limited.length > 2) {
                sb.append(limited.substring(2))
                return Pair(sb.toString(), false)
            }
            // Agrega segunda barra si hay más dígitos
            if (limited.length >= 5) {
                sb.append("/")
            }
            // Año (resto de dígitos)
            if (limited.length >= 5) {
                val yearDigits = limited.substring(4)
                if (yearDigits.length == 2) {
                    // Heurística: si los dos dígitos son mayores que los dos últimos del año actual, se asume 19xx, sino 20xx.
                    val yearInt = yearDigits.toIntOrNull() ?: return Pair(sb.toString(), true)
                    val currentYearLastTwo = Calendar.getInstance().get(Calendar.YEAR) % 100
                    val fullYear = if (yearInt > currentYearLastTwo) "19$yearDigits" else "20$yearDigits"
                    sb.append(fullYear)
                } else {
                    sb.append(yearDigits)
                }
            }
            // El resultado debe tener 10 caracteres ("DD/MM/YYYY") para ser válido.
            val result = sb.toString()
            val valid = result.length == 10 && result.matches(Regex("""\d{2}/\d{2}/\d{4}"""))
            return Pair(result, !valid)
        } catch (e: Exception) {
            return Pair(input, true)
        }
    }

    // Flujo de resultados de autenticación
    LaunchedEffect(Unit) {
        authViewModel.authResult.collect { result ->
            when (result) {
                is AuthResult.Success -> {
                    // Construye el objeto User (se aplica trim() para limpiar espacios accidentales)
                    val newUser = User(
                        name = name.trim(),
                        lastName = lastName.trim(),
                        birthDate = birthDate,
                        sector = sector.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        createdAt = com.google.firebase.Timestamp.now(),
                        notificationsEnabled = false,
                        notificationFrequency = "Diaria",
                        themePreference = themePreference,
                        primerAcceso = true
                    )
                    userViewModel.updateCurrentUserData(newUser) { updateSuccess ->
                        if (updateSuccess) {
                            navController.navigate(
                                Screen.VerificationPending.createRoute(email.trim())
                            ) {
                                popUpTo("register") { inclusive = true }
                            }
                        } else {
                            Toast.makeText(context, "Error al guardar datos del usuario", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                is AuthResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Interfaz: Se definen los límites máximos para cada campo según su naturaleza.
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.registro), // Reemplaza por tu recurso de imagen
                contentDescription = "Imagen de cabecera",
                modifier = Modifier
                    .fillMaxWidth(),

                contentScale = ContentScale.Fit,
                alignment = Alignment.TopStart
            )

            OutlinedTextField(
                value = name,
                onValueChange = { input ->
                    if (input.length <= 50) {
                        name = input; nameError = false
                    } else {
                        nameError = true
                    }
                },
                label = { Text("Nombre") },
                isError = nameError,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            if (nameError) {
                Text(
                    text = "Máximo 50 caracteres",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Apellido (máx. 50 caracteres)
            OutlinedTextField(
                value = lastName,
                onValueChange = { input ->
                    if (input.length <= 50) {
                        lastName = input; lastNameError = false
                    } else {
                        lastNameError = true
                    }
                },
                label = { Text("Apellido") },
                isError = lastNameError,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            if (lastNameError) {
                Text(
                    text = "Máximo 50 caracteres",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Campo de Fecha de Nacimiento: Permite editar manualmente, pero al recibir foco (si está vacío) se lanza el calendario.
            OutlinedTextField(
                value = birthDate,
                onValueChange = { input ->
                    val (formatted, errorFlag) = formatBirthDateInput(input)
                    birthDate = formatted
                    birthDateError = errorFlag
                },
                label = { Text("Fecha de Nacimiento") },
                trailingIcon = {
                    // Ícono para abrir el DatePicker (además, el campo completo también lo lanza)
                    Text(
                        "📅",
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isDatePickerShown = true
                            datePickerDialog.show()
                            birthDateError = false
                        }
                    )
                },
                isError = birthDateError,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        // Si el campo recibe foco y aún está vacío y no se ha mostrado ya el DatePicker, se lanza.
                        if (focusState.isFocused && birthDate.isEmpty() && !isDatePickerShown) {
                            isDatePickerShown = true
                            datePickerDialog.show()
                        }
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            if (birthDateError) {
                Text(
                    text = "Formato de fecha inválido",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Sector (máx. 50 caracteres)
            OutlinedTextField(
                value = sector,
                onValueChange = { input ->
                    if (input.length <= 50) {
                        sector = input; sectorError = false
                    } else {
                        sectorError = true
                    }
                },
                label = { Text("Sector") },
                isError = sectorError,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            if (sectorError) {
                Text(
                    text = "Máximo 50 caracteres",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Teléfono (máx. 15 caracteres)
            OutlinedTextField(
                value = phone,
                onValueChange = { input ->
                    if (input.length <= 15) {
                        phone = input; phoneError = false
                    } else {
                        phoneError = true
                    }
                },
                label = { Text("Teléfono") },
                isError = phoneError,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            if (phoneError) {
                Text(
                    text = "Máximo 15 caracteres",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Correo electrónico (máx. 100 caracteres)
            OutlinedTextField(
                value = email,
                onValueChange = { input ->
                    if (input.length <= 100) {
                        email = input; emailError = false
                    } else {
                        emailError = true
                    }
                },
                label = { Text("Correo electrónico") },
                isError = emailError,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            if (emailError) {
                Text(
                    text = "Máximo 100 caracteres",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Contraseña (máx. 50 caracteres)
            OutlinedTextField(
                value = password,
                onValueChange = { input ->
                    if (input.length <= 50) {
                        password = input; passwordError = false
                    } else {
                        passwordError = true
                    }
                },
                label = { Text("Contraseña") },
                isError = passwordError,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            if (passwordError) {
                Text(
                    text = "Máximo 50 caracteres",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Confirmar Contraseña (máx. 50 caracteres)
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { input ->
                    if (input.length <= 50) {
                        confirmPassword = input; confirmPasswordError = false
                    } else {
                        confirmPasswordError = true
                    }
                },
                label = { Text("Confirmar Contraseña") },
                isError = confirmPasswordError,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            if (confirmPasswordError) {
                Text(
                    text = "Máximo 50 caracteres",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    when {
                        name.isEmpty() || lastName.isEmpty() || birthDate.isEmpty() ||
                                sector.isEmpty() || phone.isEmpty() || email.isEmpty() ||
                                password.isEmpty() || confirmPassword.isEmpty() -> {
                            Toast.makeText(context, "Todos los campos son obligatorios", Toast.LENGTH_LONG).show()
                        }
                        nameError || lastNameError || birthDateError ||
                                sectorError || phoneError || emailError ||
                                passwordError || confirmPasswordError -> {
                            Toast.makeText(context, "Verifica los campos marcados en rojo", Toast.LENGTH_LONG).show()
                        }
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                            Toast.makeText(context, "El correo electrónico no es válido", Toast.LENGTH_LONG).show()
                        }
                        password != confirmPassword -> {
                            Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            authViewModel.register(email.trim(), password)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Registrarse")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = {
                navController.navigate("login") {
                    popUpTo("login") { inclusive = true }
                }
            }) {
                Text("¿Ya tienes cuenta? Inicia sesión")
            }
        }
    }
}






/*package com.edukrd.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.edukrd.app.models.User
import com.edukrd.app.navigation.Screen
import com.edukrd.app.viewmodel.AuthResult
import com.edukrd.app.viewmodel.AuthViewModel
import com.edukrd.app.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController) {
    // Inyección de dependencias de los ViewModels
    val authViewModel: AuthViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val context = LocalContext.current

    // Estados para los campos del formulario
    var name by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var themePreference by remember { mutableStateOf("light") }

    // flujo de resultados de autenticación
    LaunchedEffect(Unit) {
        authViewModel.authResult.collect { result ->
            when (result) {
                is AuthResult.Success -> {
                    // Si el registro en FirebaseAuth fue exitoso,
                    // construye el objeto User con los datos ingresados
                    val newUser = User(
                        name = name,
                        lastName = lastName,
                        birthDate = birthDate,
                        sector = sector,
                        phone = phone,
                        email = email,
                        createdAt = com.google.firebase.Timestamp.now(),
                        notificationsEnabled = false,
                        notificationFrequency = "Diaria",
                        themePreference = themePreference,
                        // Inicializamos el campo primerAcceso en true para mostrar Onboarding
                        primerAcceso = true
                    )
                    // Actualiza la información del usuario en Firestore a través del UserViewModel
                    userViewModel.updateCurrentUserData(newUser) { updateSuccess ->
                        if (updateSuccess) {
                            // Navegar a la pantalla de verificación pendiente, donde se le
                            // indicará al usuario que revise su correo para confirmar el registro.
                            navController.navigate(
                                Screen.VerificationPending.createRoute(email)
                            ) {
                                popUpTo("register") { inclusive = true }
                            }
                        } else {
                            Toast.makeText(context, "Error al guardar datos del usuario", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                is AuthResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        /*topBar = {
            TopAppBar(
                title = { Text("Registro") }
            )
        }*/
    ) { innerPadding ->
        // Agregamos verticalScroll y ajustamos la alineación
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top, // O simplemente omitir verticalArrangement
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Registro", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.trim() },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it.trim() },
                label = { Text("Apellido") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = birthDate,
                onValueChange = { birthDate = it.trim() },
                label = { Text("Fecha de Nacimiento (DD/MM/AAAA)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sector,
                onValueChange = { sector = it.trim() },
                label = { Text("Sector") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.trim() },
                label = { Text("Teléfono") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Correo electrónico") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it.trim() },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it.trim() },
                label = { Text("Confirmar Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(8.dp))
            /*Text("Preferencia de Tema", style = MaterialTheme.typography.bodyMedium, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = (themePreference == "light"),
                    onClick = { themePreference = "light" }
                )
                Text("Claro")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = (themePreference == "dark"),
                    onClick = { themePreference = "dark" }
                )
                Text("Oscuro")
            }*/
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    when {
                        name.isEmpty() || lastName.isEmpty() || birthDate.isEmpty() ||
                                sector.isEmpty() || phone.isEmpty() || email.isEmpty() ||
                                password.isEmpty() || confirmPassword.isEmpty() -> {
                            Toast.makeText(context, "Todos los campos son obligatorios", Toast.LENGTH_LONG).show()
                        }
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                            Toast.makeText(context, "El correo electrónico no es válido", Toast.LENGTH_LONG).show()
                        }
                        password != confirmPassword -> {
                            Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            // Inicia el proceso de registro en AuthViewModel
                            authViewModel.register(email, password)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Registrarse")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { navController.navigate("login") }) {
                Text("¿Ya tienes cuenta? Inicia sesión")
            }
        }
    }
}*/
