package com.sifan.spendtrace

import android.app.Application
import android.content.Context
import com.sifan.spendtrace.data.AppPreferences
import com.sifan.spendtrace.data.backup.BackupService
import com.sifan.spendtrace.data.local.SpendTraceDatabase
import com.sifan.spendtrace.data.repository.CategoryRepository
import com.sifan.spendtrace.data.repository.ExpenseRepository

class SpendTraceApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** 简单的依赖容器，避免为了 MVP 引入依赖注入框架。 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database = SpendTraceDatabase.build(appContext)

    val expenseRepository = ExpenseRepository(database.expenseDao())

    val categoryRepository = CategoryRepository(database.categoryDao())

    val preferences = AppPreferences(appContext)

    /** 导出 / 导入本地备份。 */
    val backupService = BackupService(
        database = database,
        appVersion = appContext.getString(R.string.version_name),
        preferences = preferences,
    )
}
