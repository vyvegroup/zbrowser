package com.zbrowser.app

import android.app.Application
import com.zbrowser.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ZBrowserApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @Volatile
        private var instance: ZBrowserApp? = null

        fun getInstance(): ZBrowserApp = instance ?: throw IllegalStateException("Application not initialized")
    }
}
