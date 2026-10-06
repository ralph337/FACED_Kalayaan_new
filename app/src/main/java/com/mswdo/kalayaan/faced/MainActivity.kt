package com.mswdo.kalayaan.faced

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mswdo.kalayaan.faced.ui.navigation.AppNavGraph
import com.mswdo.kalayaan.faced.ui.theme.FacedKalayaanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as FacedApp

        setContent {
            FacedKalayaanTheme {
                AppNavGraph(app = app)
            }
        }
    }
}
