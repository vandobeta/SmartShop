package com.smartshop.sovereign.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartshop.sovereign.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val step: Int = 0,
    val passcode: String = "",
    val confirmPasscode: String = "",
    val shopType: String = "",
    val shopName: String = "",
    val shopTel: String = "",
    val isComplete: Boolean = false,
    val isLoading: Boolean = false,
    val error: String = ""
)

const val SETUP_STEP_WELCOME = 0
const val SETUP_STEP_PASSCODE = 1
const val SETUP_STEP_SHOP_TYPE = 2
const val SETUP_STEP_SHOP_INFO = 3

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
        _uiState.update { it.copy(passcode = passcode, error = "") }
    }

    fun setConfirmPasscode(passcode: String) {
        _uiState.update { it.copy(confirmPasscode = passcode, error = "") }
    }

    fun setShopType(type: String) {
        _uiState.update { it.copy(shopType = type, error = "") }
    }

    fun setShopName(name: String) {
        _uiState.update { it.copy(shopName = name, error = "") }
    }

    fun setShopTel(tel: String) {
        _uiState.update { it.copy(shopTel = tel, error = "") }
    }

    fun nextStep() {
        _uiState.update { state ->
            when (state.step) {
                SETUP_STEP_WELCOME -> state.copy(step = SETUP_STEP_PASSCODE)
                SETUP_STEP_PASSCODE -> {
                    if (state.passcode.length >= 4 && state.passcode == state.confirmPasscode) {
                        state.copy(step = SETUP_STEP_SHOP_TYPE, error = "")
                    } else {
                        state.copy(error = "PINs don't match or too short (min 4 digits)")
                    }
                }
                SETUP_STEP_SHOP_TYPE -> {
                    if (state.shopType.isNotEmpty()) {
                        state.copy(step = SETUP_STEP_SHOP_INFO, error = "")
                    } else {
                        state.copy(error = "Please select a shop type")
                    }
                }
                SETUP_STEP_SHOP_INFO -> state.copy(step = 4)
                else -> state
            }
        }
    }

    fun completeSetup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val state = _uiState.value
                if (state.shopName.isBlank() || state.shopTel.isBlank()) {
                    _uiState.update { it.copy(isLoading = false, error = "Please fill all fields") }
                    return@launch
                }

                settingsDataStore.setAdminPasscode(state.passcode)
                settingsDataStore.setShopInfo(state.shopName, state.shopTel, state.shopType)
                settingsDataStore.setFirstLaunchComplete()

                _uiState.update { it.copy(isLoading = false, isComplete = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Setup failed") }
            }
        }
    }
}