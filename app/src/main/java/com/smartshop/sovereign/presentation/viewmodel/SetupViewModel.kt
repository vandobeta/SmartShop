package com.smartshop.sovereign.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartshop.sovereign.data.local.datastore.SettingsDataStore
import com.smartshop.sovereign.domain.usecase.AddProductUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * First Launch / Admin Setup UI State
 */
data class SetupUiState(
    val step: Int = 0, // 0=passcode, 1=shop type, 2=shop info
    val passcode: String = "",
    val confirmPasscode: String = "",
    val shopType: String = "",
    val shopName: String = "",
    val shopTel: String = "",
    val categories: List<String> = emptyList(),
    val isComplete: Boolean = false,
    val isLoading: Boolean = false,
    val error: String = ""
)

/**
 * Predefined shop types/niches
 */
val SHOP_TYPES = listOf(
    "Supermarket", "General Store", "Pharmacy", "Hardware",
    "Electronics", "Clothing", "Restaurant", "Service Station"
)

/**
 * Predefined inventory categories
 */
val DEFAULT_CATEGORIES = listOf(
    "Groceries", "Beverages", "Snacks", "Household", "Personal Care",
    "Medicines", "Electronics", "Clothing", "Other"
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        checkFirstLaunch()
    }

    private fun checkFirstLaunch() {
        viewModelScope.launch {
            settingsDataStore.isFirstLaunch.collect { isFirst ->
                if (!isFirst) {
                    _uiState.update { it.copy(isComplete = true, step = 99) }
                }
            }
        }
    }

    fun setPasscode(passcode: String) {
        _uiState.update { it.copy(passcode = passcode) }
    }

    fun setConfirmPasscode(passcode: String) {
        _uiState.update { it.copy(confirmPasscode = passcode) }
    }

    fun setShopType(type: String) {
        _uiState.update { it.copy(shopType = type) }
    }

    fun setShopName(name: String) {
        _uiState.update { it.copy(shopName = name) }
    }

    fun setShopTel(tel: String) {
        _uiState.update { it.copy(shopTel = tel) }
    }

    fun nextStep() {
        _uiState.update { state ->
            when (state.step) {
                0 -> {
                    // Validate passcode
                    if (state.passcode.length >= 4 && state.passcode == state.confirmPasscode) {
                        state.copy(step = 1, error = "")
                    } else {
                        state.copy(error = "Passcodes don't match or too short")
                    }
                }
                1 -> {
                    // Shop type selected
                    if (state.shopType.isNotEmpty()) {
                        state.copy(step = 2, error = "")
                    } else {
                        state.copy(error = "Select a shop type")
                    }
                }
                2 -> {
                    // Complete setup
                    state.copy(step = 3)
                }
                else -> state
            }
        }
    }

    fun completeSetup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val state = _uiState.value
                settingsDataStore.setAdminPasscode(state.passcode)
                settingsDataStore.setShopInfo(state.shopName, state.shopTel, state.shopType)
                settingsDataStore.setFirstLaunchComplete()

                _uiState.update { it.copy(isLoading = false, isComplete = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Setup failed") }
            }
        }
    }

    fun verifyPasscode(passcode: String): Boolean {
        // Synchronous check would require blocking - this is simplified
        // In production, use a suspend function with proper verification
        return _uiState.value.passcode == passcode
    }
}