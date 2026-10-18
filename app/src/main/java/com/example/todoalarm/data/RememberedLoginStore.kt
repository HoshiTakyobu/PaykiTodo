package com.example.todoalarm.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// Separate from AppSettings: credentials cannot enter migration archives or snapshots.
class RememberedLoginStore(context: Context) {
    private val flags = context.getSharedPreferences("remembered_login_flags", Context.MODE_PRIVATE)
    private val encrypted = runCatching {
        EncryptedSharedPreferences.create(context, "remembered_login_encrypted",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    }.getOrNull()
    val canRememberPassword: Boolean get() = encrypted != null
    val rememberAccount: Boolean get() = flags.getBoolean("account", false)
    val rememberPassword: Boolean get() = canRememberPassword && flags.getBoolean("password", false)
    val username: String get() = if (rememberAccount) flags.getString("username", "").orEmpty() else ""
    val password: String get() = if (rememberPassword) encrypted?.getString("password", "").orEmpty() else ""
    fun save(username: String, password: String, account: Boolean, secret: Boolean) {
        val rememberSecret = account && secret && canRememberPassword
        check(encrypted?.edit()?.apply {
            if (rememberSecret) putString("password", password) else remove("password")
        }?.commit() != false) { "无法安全保存密码，请关闭记住密码后重试" }
        flags.edit().putBoolean("account", account).putBoolean("password", rememberSecret)
            .putString("username", if (account) username.trim() else "").apply()
    }
    fun forgetPassword() { encrypted?.edit()?.remove("password")?.apply(); flags.edit().putBoolean("password", false).apply() }
}
