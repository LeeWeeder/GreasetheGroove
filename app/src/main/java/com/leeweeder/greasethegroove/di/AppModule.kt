package com.leeweeder.greasethegroove.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.work.WorkManager
import com.leeweeder.greasethegroove.data.repository.WorkoutRepository
import com.leeweeder.greasethegroove.domain.usecase.AdjustRepsUseCase
import com.leeweeder.greasethegroove.domain.usecase.CalculateRepsUseCase
import com.leeweeder.greasethegroove.ui.viewmodel.WorkoutViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // DataStore
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create(
            produceFile = { androidContext().preferencesDataStoreFile("workout_prefs") }
        )
    }
    // WorkManager
    single { WorkManager.getInstance(androidContext()) }

    // Repositories & UseCases
    single { WorkoutRepository(get()) }
    factory { CalculateRepsUseCase() }
    factory { AdjustRepsUseCase() }

    // ViewModel
    viewModelOf(::WorkoutViewModel)
}