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
    const val LAWYER_CONSULTATION_DETAIL_SCREEN = "lawyer_consultation_detail"
    const val CLIENT_PROFILE_SCREEN = "client_profile"
    const val SPLASH_SCREEN = "splash"
    const val LAWYER_PROFILE_SCREEN = "lawyer_profile"
    const val LAWYER_DETAIL_SCREEN = "lawyer_detail"
    const val PAYMENT_SCREEN = "payment"
    const val CONSULTATION_SCREEN = "consultation"
    const val CONSULTATION_FORM_SCREEN = "consultation_form"
    const val CHAT_SCREEN = "chat/{consultationId}"
    const val MY_CONSULTATIONS_SCREEN = "my_consultations"
    const val CONSULTATION_DETAIL_SCREEN = "consultation_detail"
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
                },
                onMyConsultationsClick = {
                    navController.navigate(AppRoutes.MY_CONSULTATIONS_SCREEN)
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
                authViewModel = authViewModel,
                onProfileClick = { navController.navigate(AppRoutes.LAWYER_PROFILE_SCREEN) },
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate(AppRoutes.LOGIN_SCREEN) {
                        popUpTo(AppRoutes.LAWYER_HOME_SCREEN) { inclusive = true }
                    }
                },
                // --- MODIFICA ESTA LAMBDA ---
                onConsultationClick = { consultationId ->
                    // Navega a la pantalla de detalles pasando el ID
                    navController.navigate("${AppRoutes.LAWYER_CONSULTATION_DETAIL_SCREEN}/$consultationId")
                }
            )
        }

        composable(
            route = AppRoutes.LAWYER_CONSULTATION_DETAIL_SCREEN + "/{consultationId}",
            arguments = listOf(navArgument("consultationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val consultationId = backStackEntry.arguments?.getString("consultationId") ?: ""
            LawyerConsultationDetailScreen(
                consultationId = consultationId,
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenChatClick = { consId, clientId ->
                    // Navega a la pantalla de chat. La ruta ya está preparada para esto.
                    // Pasamos el ID de la consulta para que el chat se cargue correctamente.
                    navController.navigate("chat/$consId")
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
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPayment = { lawyerName, lawyerIdArg, cost ->
                        navController.navigate("${AppRoutes.PAYMENT_SCREEN}/$lawyerName/$lawyerIdArg/$cost")
                    }
                )
            }
        }

        composable(
            route = AppRoutes.PAYMENT_SCREEN + "/{lawyerName}/{lawyerId}/{cost}",
            arguments = listOf(
                navArgument("lawyerName") { type = NavType.StringType },
                navArgument("lawyerId") { type = NavType.StringType },
                navArgument("cost") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val lawyerName = backStackEntry.arguments?.getString("lawyerName") ?: ""
            val lawyerId = backStackEntry.arguments?.getString("lawyerId") ?: ""
            val cost = backStackEntry.arguments?.getFloat("cost") ?: 0f
            PaymentScreen(
                lawyerName = lawyerName,
                cost = cost.toDouble(),
                onNavigateBack = { navController.popBackStack() },
                onPaymentSuccess = {
                    navController.navigate("${AppRoutes.CONSULTATION_SCREEN}/$lawyerId/${cost.toDouble()}") {
                        popUpTo(AppRoutes.CLIENT_HOME_SCREEN)
                    }
                }
            )
        }

        composable(
            route = AppRoutes.CONSULTATION_SCREEN + "/{lawyerId}/{cost}",
            arguments = listOf(
                navArgument("lawyerId") { type = NavType.StringType },
                navArgument("cost") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val lawyerId = backStackEntry.arguments?.getString("lawyerId") ?: ""
            val cost = backStackEntry.arguments?.getFloat("cost") ?: 0f
            ConsultationScreen(
                onGoToConsultation = {
                    navController.navigate("${AppRoutes.CONSULTATION_FORM_SCREEN}/$lawyerId/${cost.toDouble()}")
                }
            )
        }

        composable(
            route = AppRoutes.CONSULTATION_FORM_SCREEN + "/{lawyerId}/{cost}",
            arguments = listOf(
                navArgument("lawyerId") { type = NavType.StringType },
                navArgument("cost") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val lawyerId = backStackEntry.arguments?.getString("lawyerId") ?: ""
            val cost = backStackEntry.arguments?.getFloat("cost") ?: 0f
            ConsultationFormScreen(
                lawyerId = lawyerId,
                cost = cost.toDouble(),
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onSubmissionSuccess = { consultationId ->
                    navController.navigate(AppRoutes.MY_CONSULTATIONS_SCREEN) {
                        popUpTo(AppRoutes.CLIENT_HOME_SCREEN)
                    }
                    navController.navigate("chat/$consultationId")
                }
            )
        }

        composable(
            route = AppRoutes.CHAT_SCREEN,
            arguments = listOf(navArgument("consultationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val consultationId = backStackEntry.arguments?.getString("consultationId") ?: return@composable
            ChatScreen(
                consultationId = consultationId,
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = AppRoutes.MY_CONSULTATIONS_SCREEN) {
            MyConsultationsScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onConsultationClick = { consultationId ->
                    navController.navigate("${AppRoutes.CONSULTATION_DETAIL_SCREEN}/$consultationId")
                }
            )
        }

        composable(
            route = AppRoutes.CONSULTATION_DETAIL_SCREEN + "/{consultationId}",
            arguments = listOf(navArgument("consultationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val consultationId = backStackEntry.arguments?.getString("consultationId") ?: ""
            ConsultationDetailScreen(
                consultationId = consultationId,
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onLawyerProfileClick = { lawyerId ->
                    navController.navigate("${AppRoutes.LAWYER_DETAIL_SCREEN}/$lawyerId")
                },
                onOpenChatClick = { consId, lawId ->
                    navController.navigate("chat/$consId")
                }
            )
        }
    }
}