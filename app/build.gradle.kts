plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    //id("com.google.firebase.crashlytics") // Plugin de Crashlytics en Kotlin DSL
}

android {
    namespace = "com.edukrd.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.edukrd.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Se configura un valor para evitar el error de `mapping_file_id`
       // buildConfigField("String", "CRASHLYTICS_MAPPING_ID", "\"${System.currentTimeMillis()}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

           // configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
            //    mappingFileUploadEnabled.set(true) // Habilita reportes de Crashlytics en Release
           // }
        }
        debug {
           // configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
              //  mappingFileUploadEnabled.set(false) // Deshabilita reportes en Debug
          // }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true // ✅ Habilita la generación de BuildConfig
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("io.coil-kt:coil-compose:2.0.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Firebase Crashlytics (corregido para Kotlin DSL)
   // implementation("com.google.firebase:firebase-crashlytics-ktx")

    // Extras
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.material:material:1.7.8")
    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.8")
    implementation("androidx.navigation:navigation-compose:2.8.7")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
}
