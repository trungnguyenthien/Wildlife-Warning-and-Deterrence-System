package com.wildlife.deterrence.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson

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
  private var testProfile: UserProfileResponse? = null

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
      sharedPreferences.edit().remove("jwt_token").remove("cached_profile").apply()
    } else {
      testToken = null
      testProfile = null
    }
  }

  open fun saveUserProfile(profile: UserProfileResponse) {
    if (sharedPreferences != null) {
      val json = Gson().toJson(profile)
      sharedPreferences.edit().putString("cached_profile", json).apply()
    } else {
      testProfile = profile
    }
  }

  open fun getUserProfile(): UserProfileResponse? {
    return if (sharedPreferences != null) {
      val json = sharedPreferences.getString("cached_profile", null) ?: return null
      try {
        Gson().fromJson(json, UserProfileResponse::class.java)
      } catch (e: Exception) {
        null
      }
    } else {
      testProfile
    }
  }

  open fun deleteUserProfile() {
    if (sharedPreferences != null) {
      sharedPreferences.edit().remove("cached_profile").apply()
    } else {
      testProfile = null
    }
  }
}
