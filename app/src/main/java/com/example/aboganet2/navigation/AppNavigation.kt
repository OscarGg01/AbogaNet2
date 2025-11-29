package com.example.aboganet2.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.aboganet2.ui.screens.*

object AppRoutes {
    const val LOGIN_SCREEN = "login"
    const val REGISTER_SCREEN = "register"
    const val CLIENT_HOME_SCREEN = "client_home"
    const val LAWYER_HOME_SCREEN = "lawyer_home"
    const val CLIENT_PROFILE_SCREEN = "client_profile"
    const val SPLASH_SCREEN = "splash"
    const val LAWYER_PROFILE_SCREEN = "lawyer_profile"
    const val LAWYER_DETAIL_SCREEN = "lawyer_detail"
}

@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val sessionState by authViewModel.sessionState.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.checkActiveSession()
    }

    LaunchedEffect(sessionState) {
        when (val state = sessionState) {
            is SessionState.LoggedIn -> {
                val route = when (state.role) {
                    "cliente" -> AppRoutes.CLIENT_HOME_SCREEN
                    "abogado" -> AppRoutes.LAWYER_HOME_SCREEN
                    else -> AppRoutes.LOGIN_SCREEN
                }
                navController.navigate(route) {
                    popUpTo(AppRoutes.SPLASH_SCREEN) { inclusive = true }
                }
            }
            is SessionState.LoggedOut -> {
                navController.navigate(AppRoutes.LOGIN_SCREEN) {
                    popUpTo(AppRoutes.SPLASH_SCREEN) { inclusive = true }
                }
            }
            else -> Unit
        }
    }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                val route = when (state.role) {
                    "cliente" -> AppRoutes.CLIENT_HOME_SCREEN
                    "abogado" -> AppRoutes.LAWYER_HOME_SCREEN
                    else -> null
                }
                if (route != null) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                } else {
                    Toast.makeText(context, "Rol de usuario no reconocido.", Toast.LENGTH_LONG).show()
                }
                authViewModel.resetAuthState()
            }
            is AuthState.RegistrationSuccess -> {
                Toast.makeText(context, "Registro exitoso. Por favor, inicia sesión.", Toast.LENGTH_LONG).show()
                navController.navigate(AppRoutes.LOGIN_SCREEN) {
                    popUpTo(AppRoutes.LOGIN_SCREEN) { inclusive = true }
                }
                authViewModel.resetAuthState()
            }
            is AuthState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                authViewModel.resetAuthState()
            }
            else -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.SPLASH_SCREEN
    ) {
        composable(route = AppRoutes.SPLASH_SCREEN) {
            SplashScreen()
        }

        composable(route = AppRoutes.LOGIN_SCREEN) {
            LoginScreen(
                onLoginClick = { email, password -> authViewModel.login(email, password) },
                onNavigateToRegister = { navController.navigate(AppRoutes.REGISTER_SCREEN) }
            )
        }

        composable(route = AppRoutes.REGISTER_SCREEN) {
            RegisterScreen(
                onRegisterClick = { user, password -> authViewModel.register(user, password) },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(route = AppRoutes.CLIENT_HOME_SCREEN) {
            ClientHomeScreen(
                authViewModel = authViewModel,
                onProfileClick = { navController.navigate(AppRoutes.CLIENT_PROFILE_SCREEN) },
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate(AppRoutes.LOGIN_SCREEN) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                },
                onLawyerClick = { lawyerId ->
                    navController.navigate(AppRoutes.LAWYER_DETAIL_SCREEN + "/$lawyerId")
                }
            )
        }

        composable(route = AppRoutes.CLIENT_PROFILE_SCREEN) {
            ClientProfileScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = AppRoutes.LAWYER_HOME_SCREEN) {
            LawyerHomeScreen(
                onProfileClick = { navController.navigate(AppRoutes.LAWYER_PROFILE_SCREEN) },
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate(AppRoutes.LOGIN_SCREEN) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            )
        }

        composable(route = AppRoutes.LAWYER_PROFILE_SCREEN) {
            LawyerProfileScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppRoutes.LAWYER_DETAIL_SCREEN + "/{lawyerId}",
            arguments = listOf(navArgument("lawyerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val lawyerId = backStackEntry.arguments?.getString("lawyerId")
            if (lawyerId != null) {
                LawyerDetailScreen(
                    lawyerId = lawyerId,
                    authViewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}