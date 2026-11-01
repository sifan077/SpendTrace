package com.sifan.spendtrace.data

import android.content.Context

/** 极少量本地偏好设置，使用 SharedPreferences 即可，无需引入额外依赖。 */
class AppPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 上一次使用的分类，用于新增支出时的默认选中项。 */
    fun lastCategoryId(): Long? = prefs.getLong(KEY_LAST_CATEGORY_ID, 0L).takeIf { it > 0L }

    fun setLastCategoryId(categoryId: Long) {
        prefs.edit().putLong(KEY_LAST_CATEGORY_ID, categoryId).apply()
    }

    /** 导入备份后分类可能整体换掉，清掉旧记忆避免默认选中不存在的分类。 */
    fun clearLastCategoryId() {
        prefs.edit().remove(KEY_LAST_CATEGORY_ID).apply()
    }

    fun themeMode(): ThemeMode = ThemeMode.fromName(prefs.getString(KEY_THEME_MODE, null))

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    private companion object {
        const val PREFS_NAME = "spendtrace_prefs"
        const val KEY_LAST_CATEGORY_ID = "last_category_id"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
