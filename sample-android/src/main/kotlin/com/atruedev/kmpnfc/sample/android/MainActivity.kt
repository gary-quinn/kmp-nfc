package com.atruedev.kmpnfc.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.atruedev.kmpnfc.sample.App
import com.atruedev.kmpnfc.sample.platform.bindSettingsContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindSettingsContext(this)
        enableEdgeToEdge()
        setContent { App() }
    }
}
