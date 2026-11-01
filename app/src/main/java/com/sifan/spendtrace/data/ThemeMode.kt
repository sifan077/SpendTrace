package com.sifan.spendtrace.data

/** 主题偏好：跟随系统 / 浅色 / 深色。 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
