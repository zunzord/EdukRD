package com.edukrd.app.util

import android.content.Context

object TutorialPreferenceHelper {
    private const val PREFS_NAME = "tutorial_prefs"
    private const val KEY_TUTORIAL_SHOWN = "tutorial_shown"

    fun isTutorialShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_TUTORIAL_SHOWN, false)
    }

    fun setTutorialShown(context: Context, shown: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_TUTORIAL_SHOWN, shown).apply()
    }
}
