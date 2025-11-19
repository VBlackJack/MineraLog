package net.meshcore.mineralog.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.data.repository.MineralRepositoryImpl
import net.meshcore.mineralog.data.repository.PhotoStorageServiceImpl
import net.meshcore.mineralog.data.repository.SettingsRepository
import net.meshcore.mineralog.data.repository.SettingsRepositoryImpl
import net.meshcore.mineralog.domain.service.PhotoStorageService

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMineralRepository(
        impl: MineralRepositoryImpl
    ): MineralRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindPhotoStorageService(
        impl: PhotoStorageServiceImpl
    ): PhotoStorageService
}
