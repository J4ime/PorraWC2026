package com.porrawc2026.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.porrawc2026.app.ui.navigation.PorraNavGraph
import com.porrawc2026.app.ui.theme.WC2026Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WC2026Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PorraNavGraph()
                }
            }
        }
    }
}
