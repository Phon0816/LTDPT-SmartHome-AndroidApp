package com.example.safehome.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.safeHomeDataStore by preferencesDataStore(name = "auth_prefs")
