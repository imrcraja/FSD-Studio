package com.fsdetc.fsdstudio.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * The single DataStore instance for the whole app (current character, entitlement
 * switch, AI daily counters, cached profile JSON). Declared exactly once here —
 * a second delegate with the same name would create a conflicting instance.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fsd_studio_prefs")
