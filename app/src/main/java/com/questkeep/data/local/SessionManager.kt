package com.questkeep.data.local

import android.content.Context
import android.content.SharedPreferences
import com.questkeep.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.DATASTORE_NAME, Context.MODE_PRIVATE)

    private val _selectedCampaignId = MutableStateFlow(prefs.getString(Constants.KEY_SELECTED_CAMPAIGN_ID, null))
    val selectedCampaignId: StateFlow<String?> = _selectedCampaignId.asStateFlow()

    private val _isGm = MutableStateFlow(prefs.getBoolean("is_gm_selected", false))
    val isGm: StateFlow<Boolean> = _isGm.asStateFlow()

    private val _selectedGmId = MutableStateFlow(prefs.getString("selected_gm_id", null))
    val selectedGmId: StateFlow<String?> = _selectedGmId.asStateFlow()

    companion object {
        private val _themeFlow = MutableStateFlow("xanteya")
        val currentThemeFlow: StateFlow<String> = _themeFlow.asStateFlow()

        fun setGlobalTheme(context: Context, themeKey: String) {
            _themeFlow.value = themeKey
            context.getSharedPreferences(Constants.DATASTORE_NAME, Context.MODE_PRIVATE)
                .edit().putString(Constants.KEY_SELECTED_THEME, themeKey).apply()
        }
    }

    val selectedTheme: StateFlow<String> = _themeFlow.asStateFlow()

    init {
        val initialTheme = prefs.getString(Constants.KEY_SELECTED_THEME, "xanteya") ?: "xanteya"
        _themeFlow.value = initialTheme
        prefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == Constants.KEY_SELECTED_THEME) {
                _themeFlow.value = prefs.getString(Constants.KEY_SELECTED_THEME, "xanteya") ?: "xanteya"
            }
        }
    }

    fun saveSelectedCampaign(campaignId: String?, isGm: Boolean, gmId: String?) {
        _selectedCampaignId.value = campaignId
        _isGm.value = isGm
        _selectedGmId.value = gmId

        prefs.edit().apply {
            if (campaignId != null) putString(Constants.KEY_SELECTED_CAMPAIGN_ID, campaignId)
            else remove(Constants.KEY_SELECTED_CAMPAIGN_ID)

            putBoolean("is_gm_selected", isGm)

            if (gmId != null) putString("selected_gm_id", gmId)
            else remove("selected_gm_id")
            apply()
        }
    }

    fun saveTheme(themeKey: String) {
        _themeFlow.value = themeKey
        prefs.edit().putString(Constants.KEY_SELECTED_THEME, themeKey).apply()
    }

    fun clearSession() {
        _selectedCampaignId.value = null
        _isGm.value = false
        _selectedGmId.value = null
        prefs.edit()
            .remove(Constants.KEY_SELECTED_CAMPAIGN_ID)
            .remove("is_gm_selected")
            .remove("selected_gm_id")
            .apply()
    }
}
