package com.questkeep.di

import com.questkeep.data.repository.AuthRepositoryImpl
import com.questkeep.data.repository.CampaignRepositoryImpl
import com.questkeep.data.repository.CharacterRepositoryImpl
import com.questkeep.data.repository.GmNoteRepositoryImpl
import com.questkeep.data.repository.ImageRepositoryImpl
import com.questkeep.data.repository.InventoryRepositoryImpl
import com.questkeep.data.repository.ItemCardRepositoryImpl
import com.questkeep.data.repository.MessageRepositoryImpl
import com.questkeep.data.repository.NpcCardRepositoryImpl
import com.questkeep.data.repository.PlayerNoteRepositoryImpl
import com.questkeep.data.repository.PlayerNpcRepositoryImpl
import com.questkeep.data.repository.SharedInventoryRepositoryImpl
import com.questkeep.data.repository.SpellRepositoryImpl
import com.questkeep.domain.repository.AuthRepository
import com.questkeep.domain.repository.CampaignRepository
import com.questkeep.domain.repository.CharacterRepository
import com.questkeep.domain.repository.GmNoteRepository
import com.questkeep.domain.repository.ImageRepository
import com.questkeep.domain.repository.InventoryRepository
import com.questkeep.domain.repository.ItemCardRepository
import com.questkeep.domain.repository.MessageRepository
import com.questkeep.domain.repository.NpcCardRepository
import com.questkeep.domain.repository.PlayerNoteRepository
import com.questkeep.domain.repository.PlayerNpcRepository
import com.questkeep.domain.repository.SharedInventoryRepository
import com.questkeep.domain.repository.SpellRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCharacterRepository(impl: CharacterRepositoryImpl): CharacterRepository

    @Binds
    @Singleton
    abstract fun bindCampaignRepository(impl: CampaignRepositoryImpl): CampaignRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Singleton
    abstract fun bindGmNoteRepository(impl: GmNoteRepositoryImpl): GmNoteRepository

    @Binds
    @Singleton
    abstract fun bindNpcCardRepository(impl: NpcCardRepositoryImpl): NpcCardRepository

    @Binds
    @Singleton
    abstract fun bindSpellRepository(impl: SpellRepositoryImpl): SpellRepository

    @Binds
    @Singleton
    abstract fun bindInventoryRepository(impl: InventoryRepositoryImpl): InventoryRepository

    @Binds
    @Singleton
    abstract fun bindItemCardRepository(impl: ItemCardRepositoryImpl): ItemCardRepository

    @Binds
    @Singleton
    abstract fun bindImageRepository(impl: ImageRepositoryImpl): ImageRepository

    @Binds
    @Singleton
    abstract fun bindSharedInventoryRepository(impl: SharedInventoryRepositoryImpl): SharedInventoryRepository

    @Binds
    @Singleton
    abstract fun bindPlayerNoteRepository(impl: PlayerNoteRepositoryImpl): PlayerNoteRepository

    @Binds
    @Singleton
    abstract fun bindPlayerNpcRepository(impl: PlayerNpcRepositoryImpl): PlayerNpcRepository
}
