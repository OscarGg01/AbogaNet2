package com.example.aboganet2.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.role
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aboganet2.ui.screens.* // Importa todo desde screens

object AppRoutes {
    const val LOGIN_SCREEN = "login"
    const val REGISTER_SCREEN = "register"
    const val CLIENT_HOME_SCREEN = "client_home"
    const val LAWYER_HOME_SCREEN = "lawyer_home"
    const val CLIENT_PROFILE_SCREEN = "client_profile"
    const val SPLASH_SCREEN = "splash"
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

    // Navegación basada en el estado de la sesión
    LaunchedEffect(sessionState) {
        when (val state = sessionState) {
            // CORRECCIÓN: Usamos la clase SessionState que definimos en el ViewModel
            is SessionState.LoggedIn -> {
                val route = when (state.role) { // Ahora 'role' existe y es correcto
                    "cliente" -> AppRoutes.CLIENT_HOME_SCREEN
                    "abogado" -> AppRoutes.LAWYER_HOME_SCREEN
                    else -> AppRoutes.LOGIN_SCREEN
                }
                navController.navigate(route) {
                    popUpTo(AppRoutes.SPLASH_SCREEN) { inclusive = true }
                }
            }
            // CORRECCIÓN: Usamos la clase SessionState que definimos en el ViewModel
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
            // --- ERROR CORREGIDO AQUÍ ---
            ClientHomeScreen(
                onProfileClick = {
                    // Navegamos a la nueva pantalla de perfil
                    navController.navigate(AppRoutes.CLIENT_PROFILE_SCREEN)
                },
                onLogoutClick = {
                    // Llamamos a la función de logout del ViewModel
                    authViewModel.logout()
                    // Navegamos de vuelta al login, limpiando la pila
                    navController.navigate(AppRoutes.LOGIN_SCREEN) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            )
        }
        composable(route = AppRoutes.LAWYER_HOME_SCREEN) {
            LawyerHomeScreen()
        }
        // --- NUEVO DESTINO AÑADIDO ---
        composable(route = AppRoutes.CLIENT_PROFILE_SCREEN) {
            ClientProfileScreen(
                authViewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}