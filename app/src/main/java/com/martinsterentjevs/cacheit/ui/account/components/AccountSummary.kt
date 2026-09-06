package com.martinsterentjevs.cacheit.ui.account.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.martinsterentjevs.cacheit.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AccountSummaryUiState{
    data object Loading: AccountSummaryUiState
    data class Error(
        val messageId:Int
    ) : AccountSummaryUiState
    data class Content(
        val accountHolder:String,
        val username:String,
        val email:String
    ): AccountSummaryUiState
}

class AccountSummaryViewModel: ViewModel(){
    private val _uiState = MutableStateFlow<AccountSummaryUiState>(AccountSummaryUiState.Loading)
    val uiState: StateFlow<AccountSummaryUiState> = _uiState.asStateFlow()
    private var isLoadInFlight = false
    fun load(){
        if (isLoadInFlight) return
        if (_uiState.value == AccountSummaryUiState.Loading) isLoadInFlight = true
        try {
            TODO("Set up account summary data fetching and populating to content state")
            _uiState.value = AccountSummaryUiState.Content("PLACEHOLDER","PLACEHOLDER","PLACEHOLDER")
        } catch (_:Exception){
            _uiState.value = AccountSummaryUiState.Error(R.string.data_fetch_failed)
        } finally {
            isLoadInFlight = false
        }
    }
}

@Composable
fun AccountSummary (
 viewModel: AccountSummaryViewModel = hiltViewModel()
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier.fillMaxWidth()
    ){
        when (val state = uiState){
            is AccountSummaryUiState.Loading -> CircularProgressIndicator()
            is AccountSummaryUiState.Error -> Text(stringResource(R.string.account_loading_error))
            is AccountSummaryUiState.Content -> {
                Text(stringResource(R.string.registration_accountholder))
                Text(state.accountHolder)
                Text(stringResource(R.string.registration_email))
                Text(state.email)
                Text(stringResource(R.string.registration_username))
                Text(state.username)
            }
        }
    }
}
