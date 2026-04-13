package com.questkeep.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.questkeep.data.local.database.CampaignDao
import com.questkeep.data.local.database.CharacterDao
import com.questkeep.data.local.database.GmNoteDao
import com.questkeep.data.local.database.InventoryDao
import com.questkeep.data.local.database.ItemCardDao
import com.questkeep.data.local.database.MessageDao
import com.questkeep.data.local.database.NpcCardDao
import com.questkeep.data.local.database.PlayerNoteDao
import com.questkeep.data.local.database.PlayerNpcDao
import com.questkeep.data.local.database.SharedInventoryDao
import com.questkeep.data.local.database.SpellDao
import com.questkeep.data.local.database.QuestKeepDatabase
import com.questkeep.data.remote.ImgbbApiService
import com.questkeep.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QuestKeepDatabase =
        Room.databaseBuilder(context, QuestKeepDatabase::class.java, "questkeep.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCharacterDao(db: QuestKeepDatabase): CharacterDao = db.characterDao()

    @Provides
    fun provideMessageDao(db: QuestKeepDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideCampaignDao(db: QuestKeepDatabase): CampaignDao = db.campaignDao()

    @Provides
    fun provideGmNoteDao(db: QuestKeepDatabase): GmNoteDao = db.gmNoteDao()

    @Provides
    fun provideNpcCardDao(db: QuestKeepDatabase): NpcCardDao = db.npcCardDao()

    @Provides
    fun provideSpellDao(db: QuestKeepDatabase): SpellDao = db.spellDao()

    @Provides
    fun provideInventoryDao(db: QuestKeepDatabase): InventoryDao = db.inventoryDao()

    @Provides
    fun provideItemCardDao(db: QuestKeepDatabase): ItemCardDao = db.itemCardDao()

    @Provides
    fun provideSharedInventoryDao(db: QuestKeepDatabase): SharedInventoryDao = db.sharedInventoryDao()

    @Provides
    fun providePlayerNoteDao(db: QuestKeepDatabase): PlayerNoteDao = db.playerNoteDao()

    @Provides
    fun providePlayerNpcDao(db: QuestKeepDatabase): PlayerNpcDao = db.playerNpcDao()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideImgbbRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(Constants.IMGBB_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideImgbbApiService(retrofit: Retrofit): ImgbbApiService =
        retrofit.create(ImgbbApiService::class.java)
}
