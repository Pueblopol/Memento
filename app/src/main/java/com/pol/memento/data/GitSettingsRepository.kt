package com.pol.memento.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class GitSettingsRepository(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "git_secret_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getRepoUrl(): String? = sharedPreferences.getString("git_repo_url", null)
    fun getUsername(): String? = sharedPreferences.getString("git_username", null)
    fun getPat(): String? = sharedPreferences.getString("git_pat", null)
    fun isAutoSyncEnabled(): Boolean = sharedPreferences.getBoolean("git_auto_sync", false)

    fun saveGitCredentials(repoUrl: String, username: String, pat: String) {
        sharedPreferences.edit()
            .putString("git_repo_url", repoUrl)
            .putString("git_username", username)
            .putString("git_pat", pat)
            .apply()
    }
    
    fun setAutoSync(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean("git_auto_sync", enabled)
            .apply()
    }
    
    fun clearCredentials() {
        sharedPreferences.edit().clear().apply()
    }
}
