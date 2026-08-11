package com.martinsterentjevs.cacheit

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.martinsterentjevs.cacheit.ui.auth.LoginScreen
import com.martinsterentjevs.cacheit.ui.error.ErrorScreen
import com.martinsterentjevs.cacheit.ui.navigation.CacheItNavHost
import com.martinsterentjevs.cacheit.ui.onboarding.WelcomeScreen
import com.martinsterentjevs.cacheit.ui.theme.CacheItTheme

@Composable
fun CacheItApp (){
    CacheItNavHost()
}

