package com.wildlife.deterrence.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

open class TokenManager(context: Context?) {
  private val sharedPreferences = context?.let {
    val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    EncryptedSharedPreferences.create(
      "secure_prefs",
      masterKeyAlias,
      it,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
  }

  private var testToken: String? = null

  open fun saveToken(token: String) {
    if (sharedPreferences != null) {
      sharedPreferences.edit().putString("jwt_token", token).apply()
    } else {
      testToken = token
    }
  }

  open fun getToken(): String? {
    return if (sharedPreferences != null) {
      sharedPreferences.getString("jwt_token", null)
    } else {
      testToken
    }
  }

  open fun deleteToken() {
    if (sharedPreferences != null) {
      sharedPreferences.edit().remove("jwt_token").apply()
    } else {
      testToken = null
    }
  }
}
