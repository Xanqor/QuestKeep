package com.questkeep.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.questkeep.auth.AuthState
import com.questkeep.auth.AuthViewModel
import com.questkeep.auth.LoginScreen
import com.questkeep.auth.RegisterScreen

@Composable
fun RootNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    val startDestination = when (authState) {
        is AuthState.Authenticated -> NavRoutes.MAIN
        else -> NavRoutes.LOGIN
    }

    // Re-route when auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.navigate(NavRoutes.MAIN) {
                    popUpTo(NavRoutes.LOGIN) { inclusive = true }
                }
            }
            is AuthState.Unauthenticated -> {
                navController.navigate(NavRoutes.LOGIN) {
                    popUpTo(NavRoutes.MAIN) { inclusive = true }
                }
            }
            else -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (authState is AuthState.Loading) NavRoutes.LOGIN else startDestination
    ) {
        composable(NavRoutes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(NavRoutes.REGISTER) },
                onLoginSuccess = {}  // handled by LaunchedEffect above
            )
        }

        composable(NavRoutes.REGISTER) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {}
            )
        }

        composable(NavRoutes.MAIN) {
            MainScreen(onLogout = { authViewModel.logout() })
        }
    }
}
