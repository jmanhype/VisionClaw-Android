package com.visionclaw.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.visionclaw.android.ui.screens.MainScreen
import com.visionclaw.android.ui.screens.SessionScreen
import com.visionclaw.android.ui.viewmodels.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    
                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            MainScreen(
                                onStartPhone = {
                                    navController.navigate("session/PHONE")
                                },
                                onStartGlasses = {
                                    navController.navigate("session/GLASSES")
                                }
                            )
                        }
                        
                        composable(
                            route = "session/{mode}",
                            arguments = listOf(navArgument("mode") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val modeStr = backStackEntry.arguments?.getString("mode") ?: "PHONE"
                            val mode = try {
                                SessionViewModel.SessionMode.valueOf(modeStr)
                            } catch (e: Exception) {
                                SessionViewModel.SessionMode.PHONE
                            }
                            
                            SessionScreen(mode = mode)
                        }
                    }
                }
            }
        }
    }
}
