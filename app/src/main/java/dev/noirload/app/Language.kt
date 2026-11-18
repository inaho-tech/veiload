package dev.noirload.app

import android.content.Context
import java.util.Locale

enum class AppLanguage(val storedValue: String) {
    System("system"), Japanese("ja"), English("en");

    companion object {
        fun from(value: String?): AppLanguage = entries.firstOrNull { it.storedValue == value } ?: System
    }
}

object AppLocale {
    private const val PREFERENCES = "app_settings"
    private const val KEY = "language"

    @Volatile
    var selected: AppLanguage = AppLanguage.System
        private set

    fun initialize(context: Context) {
        selected = AppLanguage.from(context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(KEY, null))
    }

    fun set(context: Context, language: AppLanguage) {
        selected = language
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putString(KEY, language.storedValue).apply()
    }

    fun isJapanese(): Boolean = when (selected) {
        AppLanguage.Japanese -> true
        AppLanguage.English -> false
        AppLanguage.System -> Locale.getDefault().language.equals("ja", ignoreCase = true)
    }
}

fun tr(japanese: String, english: String): String = if (AppLocale.isJapanese()) japanese else english

fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.System -> tr("端末に合わせる", "Follow device")
    AppLanguage.Japanese -> tr("日本語", "Japanese")
    AppLanguage.English -> tr("英語", "English")
}
