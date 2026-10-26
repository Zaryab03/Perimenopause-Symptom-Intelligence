package com.perimenopauseintelligence.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.perimenopauseintelligence.app.ui.AppRoot
import com.perimenopauseintelligence.app.ui.AppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val container get() = (application as App).container

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // With a PIN set, hide the app's contents from screenshots and the recent-apps preview.
        lifecycleScope.launch {
            container.settings.pinEnabled.collect { on ->
                if (on) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        setContent { AppTheme { AppRoot(container) } }
    }

    override fun onStop() {
        super.onStop()
        container.onBackgrounded()
    }

    override fun onStart() {
        super.onStart()
        container.onForegrounded()
    }
}
