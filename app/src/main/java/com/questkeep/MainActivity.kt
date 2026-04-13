package com.questkeep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.questkeep.auth.AuthViewModel
import com.questkeep.navigation.RootNavGraph
import com.questkeep.ui.theme.QuestKeepTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuestKeepTheme {
                val navController = rememberNavController()
                RootNavGraph(navController = navController, authViewModel = authViewModel)
            }
        }
    }
}
