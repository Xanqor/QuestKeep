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
    fun provideFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        try {
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build())
                .build()
            firestore.firestoreSettings = settings
        } catch (_: Exception) {}
        return firestore
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideImgbbRetrofit(client: okhttp3.OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(Constants.IMGBB_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideImgbbApiService(retrofit: Retrofit): ImgbbApiService =
        retrofit.create(ImgbbApiService::class.java)

    @Provides
    @Singleton
    fun provideImgurApiService(client: okhttp3.OkHttpClient): com.questkeep.data.remote.ImgurApiService =
        Retrofit.Builder()
            .baseUrl(Constants.IMGUR_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.questkeep.data.remote.ImgurApiService::class.java)
}
