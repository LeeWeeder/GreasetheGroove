package com.leeweeder.greasethegroove

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.leeweeder.greasethegroove.ui.screen.WorkoutScreen
import com.leeweeder.greasethegroove.ui.theme.GreaseTheGrooveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GreaseTheGrooveTheme {
                WorkoutScreen()
            }
        }
    }
}