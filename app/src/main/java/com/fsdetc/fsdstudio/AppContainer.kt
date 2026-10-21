package com.fsdetc.fsdstudio

import android.content.Context
import androidx.room.Room
import com.fsdetc.fsdstudio.data.CharacterStore
import com.fsdetc.fsdstudio.data.Entitlements
import com.fsdetc.fsdstudio.data.HistoryRepository
import com.fsdetc.fsdstudio.data.SkinRepository
import com.fsdetc.fsdstudio.data.db.AppDatabase

/** Tiny manual dependency container — no Hilt in V1, everything is created once here. */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val skinRepository: SkinRepository = SkinRepository(appContext)
    val characterStore: CharacterStore = CharacterStore(appContext)
    val entitlements: Entitlements = Entitlements(appContext)

    val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "fsd_studio.db").build()
    }

    val historyRepository: HistoryRepository by lazy {
        HistoryRepository(appContext, database.historyDao())
    }
}
