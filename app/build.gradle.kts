plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
    id("com.google.gms.google-services")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10"
    // id("com.google.firebase.crashlytics") // Opcional si lo deseas
}

android {
    namespace = "com.edukrd.app"
    compileSdk = 35
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.6.11" }

    defaultConfig {
        applicationId = "com.edukrd.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "SUPABASE_PROJECT_REF",
            "\"${project.findProperty("SUPABASE_PROJECT_REF") ?: "default_project_ref"}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_PUBLIC_ANON_KEY",
            "\"${project.findProperty("SUPABASE_PUBLIC_ANON_KEY") ?: "default_key"}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Configuraciones específicas para debug, si son necesarias
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true

    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        // La versión del compilador de Compose debe ser compatible con el BOM
        kotlinCompilerExtensionVersion = "1.5.15"
    }
}



dependencies {
    // BOM para unificar las versiones de todas las librerías de Compose
    implementation(platform("androidx.compose:compose-bom:2025.03.00"))
    implementation("androidx.compose.foundation:foundation")

    // Dependencias básicas de Compose
    implementation("androidx.compose.ui:ui")

    // La librería Material (versión antigua) se usa para componentes como ModalBottomSheetLayout
    implementation("androidx.compose.material:material")
    // Material3 para componentes modernos
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(files("libs/compose-charts-android-0.0.13.aar"))

    implementation("androidx.compose.material:material-icons-extended:<version>")

    //lottie
    implementation ("com.airbnb.android:lottie-compose:6.0.0")

    //otros
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
    implementation("com.github.MackHartley:RoundedProgressBar:3.0.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")



    //Vico

    implementation(files("libs/compose-m2-2.1.1.aar"))
    implementation(files("libs/core-2.1.1.aar"))



    //Fonts Google
    implementation("androidx.compose.ui:ui-text-google-fonts:1.3.0")




    // Activity Compose
    implementation("androidx.activity:activity-compose:1.10.1")

    // Navegación en Compose
    implementation("androidx.navigation:navigation-compose:2.8.9")

    // Coil para carga de imágenes
    implementation("io.coil-kt:coil-compose:2.2.2")

    // Firebase: Usa el BOM para sincronizar versiones
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-database-ktx:20.2.1")

    // Hilt para inyección de dependencias
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-android-compiler:2.48.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.8.0")

    // Otras dependencias de AndroidX
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    //sopabase

    //implementation("io.github.jan-tennert.supabase-kt:supabase-kt:0.1.0")

    //google-pager

    implementation("com.google.accompanist:accompanist-pager:0.31.5-beta")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.31.5-beta")


    //knor

    val ktorVersion = "2.2.4"

    // Ktor Client Core
    implementation("io.ktor:ktor-client-core:$ktorVersion")

    // Motor de red: CIO (puedes usar OkHttp si prefieres)
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    // Para manejar la negociación de contenido (JSON)
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // Para serializar/deserializar JSON con Kotlinx
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Librería de Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    //animacion transiciones pantalla

    implementation("androidx.compose.animation:animation:1.5.4")





    // Dependencias para testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}