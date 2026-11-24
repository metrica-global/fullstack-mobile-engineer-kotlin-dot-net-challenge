package com.fieldops.app.data.local

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit
import com.fieldops.app.LoginActivity
import com.fieldops.app.Session

interface TokenProvider {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun updateTokens(accessToken: String, refreshToken: String)
    fun onSessionExpired()
}

object SessionManager : TokenProvider {

    private const val SECURE_PREFS_NAME = "fieldops_secure_session"
    private const val LEGACY_PREFS_NAME = "fieldops"

    private const val KEY_ENC_ACCESS_TOKEN = "enc_access_token"
    private const val KEY_ENC_REFRESH_TOKEN = "enc_refresh_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_FULL_NAME = "full_name"
    private const val KEY_USERNAME = "username"

    private var appContext: Context? = null

    fun init(context: Context) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        purgeLegacyInsecureData(applicationContext)
        syncLegacySessionObject()
    }

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: Int,
        fullName: String,
        username: String,
    ) {
        val encryptedAccess = KeystoreCrypto.encrypt(accessToken)
        val encryptedRefresh = KeystoreCrypto.encrypt(refreshToken)

        prefs()?.edit(commit = true) {
            putString(KEY_ENC_ACCESS_TOKEN, encryptedAccess)
            putString(KEY_ENC_REFRESH_TOKEN, encryptedRefresh)
            putInt(KEY_USER_ID, userId)
            putString(KEY_FULL_NAME, fullName)
            putString(KEY_USERNAME, username)
        }

        Session.token = accessToken
        Session.userId = userId
        Session.fullName = fullName
    }

    override fun updateTokens(accessToken: String, refreshToken: String) {
        val encryptedAccess = KeystoreCrypto.encrypt(accessToken)
        val encryptedRefresh = KeystoreCrypto.encrypt(refreshToken)

        prefs()?.edit(commit = true) {
            putString(KEY_ENC_ACCESS_TOKEN, encryptedAccess)
            putString(KEY_ENC_REFRESH_TOKEN, encryptedRefresh)
        }

        Session.token = accessToken
    }

    override fun getAccessToken(): String? {
        val stored = prefs()?.getString(KEY_ENC_ACCESS_TOKEN, null)
        if (stored != null) {
            val decrypted = KeystoreCrypto.decrypt(stored)
            Session.token = decrypted
            return decrypted
        }
        return Session.token
    }

    override fun getRefreshToken(): String? {
        val stored = prefs()?.getString(KEY_ENC_REFRESH_TOKEN, null) ?: return null
        return KeystoreCrypto.decrypt(stored)
    }

    fun hasActiveSession(): Boolean {
        return !getAccessToken().isNullOrBlank() && !getRefreshToken().isNullOrBlank()
    }

    fun getFullName(): String {
        return prefs()?.getString(KEY_FULL_NAME, null) ?: Session.fullName
    }

    fun getLastUsername(): String {
        return prefs()?.getString(KEY_USERNAME, "") ?: ""
    }

    fun clearSession() {
        prefs()?.edit(commit = true) {
            remove(KEY_ENC_ACCESS_TOKEN)
            remove(KEY_ENC_REFRESH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_FULL_NAME)
        }
        Session.token = null
        Session.userId = 0
        Session.fullName = ""
    }

    override fun onSessionExpired() {
        clearSession()
        val context = appContext ?: return
        Handler(Looper.getMainLooper()).post {
            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }
    }

    private fun syncLegacySessionObject() {
        val preferences = prefs() ?: return
        Session.token = getAccessToken()
        Session.userId = preferences.getInt(KEY_USER_ID, 0)
        Session.fullName = preferences.getString(KEY_FULL_NAME, "") ?: ""
    }

    private fun purgeLegacyInsecureData(context: Context) {
        context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove("token")
            remove("password")
        }
    }

    private fun prefs(): SharedPreferences? {
        return appContext?.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
    }
}
