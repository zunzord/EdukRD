package com.edukrd.app.di

import com.edukrd.app.repository.TimeRepository
import com.edukrd.app.repository.TimeRepositoryImpl
import com.edukrd.app.usecase.GetServerDateUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {



    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase =
        FirebaseDatabase.getInstance("https://edukrd-58236-default-rtdb.firebaseio.com")

    @Provides @Singleton
    fun provideTimeRepository(
        firebaseDatabase: FirebaseDatabase
    ): TimeRepository = TimeRepositoryImpl(firebaseDatabase)

    @Provides @Singleton
    fun provideGetServerDateUseCase(
        timeRepository: TimeRepository
    ): GetServerDateUseCase = GetServerDateUseCase(timeRepository)


}
