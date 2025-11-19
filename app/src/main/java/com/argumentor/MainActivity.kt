package com.argumentor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.argumentor.ui.ArguMentorApp
import com.argumentor.core.ui.theme.ArguMentorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArguMentorTheme {
                Surface {
                    ArguMentorApp()
                }
            }
        }
    }
}
