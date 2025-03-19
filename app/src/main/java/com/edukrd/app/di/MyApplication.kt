package com.edukrd.app.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Aquí puedes inicializar cualquier recurso que necesite tu aplicación al arrancar
        // (Por ejemplo, librerías de análisis, Crashlytics, etc.)
    }
}
