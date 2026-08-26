package com.canni.runpod

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.canni.runpod.data.auth.ApiKeyStore
import com.canni.runpod.ui.nav.AppNav
import com.canni.runpod.ui.theme.RunPodTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var keyStore: ApiKeyStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RunPodTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav(navController = rememberNavController(), keyStore = keyStore)
                }
            }
        }
    }
}
