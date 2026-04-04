package com.example.myapplication.core.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public final class LanguageManager {

    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String LANG_VI = "vi";

    private LanguageManager() {
    }

    @NonNull
    public static String getSavedLanguage(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, LANG_VI);
    }

    public static void applySavedLanguage(@NonNull Context context) {
        String lang = getSavedLanguage(context);
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang));
    }

    public static void setLanguage(@NonNull Context context, @NonNull String languageTag) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE, languageTag)
                .apply();
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag));
    }

    public static boolean isCurrentLanguage(@NonNull Context context, @NonNull String languageTag) {
        return languageTag.equalsIgnoreCase(getSavedLanguage(context));
    }
}