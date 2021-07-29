/*
 * Copyright 2019 ~ https://github.com/braver-tool
 */

package com.speech.call

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager private constructor(context: Context) {
    private val prefManager: SharedPreferences
    fun setStringValue(keyName: String?, value: String?) {
        prefManager.edit().putString(keyName, value).apply()
    }

    fun getStringValue(keyName: String?): String? {
        return prefManager.getString(keyName, "")
    }

    fun setBooleanValue(keyName: String?, value: Boolean) {
        prefManager.edit().putBoolean(keyName, value).apply()
    }

    fun getBooleanValue(keyName: String?): Boolean {
        return prefManager.getBoolean(keyName, false)
    }

    fun setIntValue(keyName: String?, value: Int) {
        prefManager.edit().putInt(keyName, value).apply()
    }

    fun getIntValue(keyName: String?): Int {
        return prefManager.getInt(keyName, 0)
    }

    fun removePref(context: Context) {
        val preferences = context.getSharedPreferences(PREFERENCE_MAIN, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.clear()
        editor.apply()
    }

    fun removePref(`val`: String?) {
        val editor = prefManager.edit()
        editor.remove(`val`)
        editor.apply()
    }

    companion object {
        private val PREFERENCE_MAIN = PreferencesManager::class.java.getPackage().name
        private var prefInstance: PreferencesManager? = null

        @Synchronized
        fun getInstance(context: Context): PreferencesManager? {
            if (prefInstance == null) {
                prefInstance = PreferencesManager(context.applicationContext)
            }
            return prefInstance
        }
    }

    init {
        prefManager = context.getSharedPreferences(PREFERENCE_MAIN, Context.MODE_PRIVATE)
    }
}