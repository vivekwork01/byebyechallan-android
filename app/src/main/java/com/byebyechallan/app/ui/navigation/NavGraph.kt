package com.byebyechallan.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.byebyechallan.app.ByeByeChallanApp
import com.byebyechallan.app.ui.screens.auth.LoginScreen
import com.byebyechallan.app.ui.screens.auth.RegisterScreen
import com.byebyechallan.app.ui.screens.document.DocumentUploadScreen
import com.byebyechallan.app.ui.screens.home.HomeScreen
import com.byebyechallan.app.ui.screens.profile.CreateProfileScreen
import com.byebyechallan.app.ui.screens.profile.ProfileDetailScreen
import com.byebyechallan.app.ui.screens.splash.SplashScreen
import com.byebyechallan.app.ui.screens.vehicle.AddVehicleScreen
import com.byebyechallan.app.ui.screens.vehicle.VehicleDetailScreen

@Composable
fun AppNavGraph(app: ByeByeChallanApp) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(
                app = app,
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                app = app,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                app = app,
                onRegisterSuccess = {
                    // Per spec: after registering, redirect to Login (not auto-login)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                app = app,
                navController = navController,
                onProfileClick = { id, name ->
                    navController.navigate(Screen.ProfileDetail.createRoute(id, name))
                },
                onAddProfileClick = { navController.navigate(Screen.CreateProfile.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CreateProfile.route) {
            CreateProfileScreen(
                app = app,
                onBack = { navController.popBackStack() },
                onProfileCreated = {
                    // Signal Home to refresh its profiles list, then return
                    navController.previousBackStackEntry?.savedStateHandle?.set("refreshProfiles", true)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ProfileDetail.route,
            arguments = listOf(
                navArgument("profileId") { type = NavType.LongType },
                navArgument("profileName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getLong("profileId") ?: 0L
            val profileName = backStackEntry.arguments?.getString("profileName") ?: ""
            ProfileDetailScreen(
                app = app,
                navController = navController,
                profileId = profileId,
                profileName = profileName,
                onBack = { navController.popBackStack() },
                onAddVehicle = { navController.navigate(Screen.AddVehicle.createRoute(profileId)) },
                onVehicleClick = { vehicle ->
                    navController.navigate(
                        Screen.VehicleDetail.createRoute(
                            profileId = profileId,
                            vehicleRegNo = vehicle.registrationNo,
                            country = vehicle.country,
                            state = vehicle.state,
                            registrationType = vehicle.registrationType,
                            vehicleType = vehicle.vehicleType
                        )
                    )
                }
            )
        }

        composable(
            route = Screen.AddVehicle.route,
            arguments = listOf(navArgument("profileId") { type = NavType.LongType })
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getLong("profileId") ?: 0L
            AddVehicleScreen(
                app = app,
                profileId = profileId,
                onBack = { navController.popBackStack() },
                onVehicleAdded = { vehicle ->
                    // Tell ProfileDetail to refresh its vehicle list when AddVehicle completes
                    navController.previousBackStackEntry?.savedStateHandle?.set("refreshVehicles", true)

                    navController.navigate(
                        Screen.VehicleDetail.createRoute(
                            profileId = profileId,
                            vehicleRegNo = vehicle.registrationNo,
                            country = vehicle.country,
                            state = vehicle.state,
                            registrationType = vehicle.registrationType,
                            vehicleType = vehicle.vehicleType
                        )
                    ) {
                        popUpTo(Screen.ProfileDetail.route) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Screen.VehicleDetail.route,
            arguments = listOf(
                navArgument("profileId") { type = NavType.LongType },
                navArgument("vehicleRegNo") { type = NavType.StringType },
                navArgument("country") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("state") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("registrationType") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("vehicleType") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments!!
            val profileId = args.getLong("profileId")
            val vehicleRegNo = args.getString("vehicleRegNo") ?: ""
            val country = args.getString("country") ?: ""
            val state = args.getString("state") ?: ""
            val registrationType = args.getString("registrationType") ?: ""
            val vehicleType = args.getString("vehicleType") ?: ""

            VehicleDetailScreen(
                app = app,
                navController = navController,
                profileId = profileId,
                vehicleRegNo = vehicleRegNo,
                country = country,
                state = state,
                registrationType = registrationType,
                vehicleType = vehicleType,
                onBack = { navController.popBackStack() },
                onDocumentClick = { item ->
                    navController.navigate(
                        Screen.DocumentUpload.createRoute(
                            profileId = profileId,
                            vehicleRegNo = vehicleRegNo,
                            docTemplateId = item.template.docTemplateId.ifBlank { item.template.docId },
                            docName = item.template.docName
                        )
                    )
                }
            )
        }

        composable(
            route = Screen.DocumentUpload.route,
            arguments = listOf(
                navArgument("profileId") { type = NavType.LongType },
                navArgument("vehicleRegNo") { type = NavType.StringType },
                navArgument("docTemplateId") { type = NavType.StringType },
                navArgument("docName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments!!
            DocumentUploadScreen(
                app = app,
                profileId = args.getLong("profileId"),
                vehicleRegNo = args.getString("vehicleRegNo") ?: "",
                docTemplateId = args.getString("docTemplateId") ?: "",
                docName = args.getString("docName") ?: "",
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("refreshDocuments", true)
                    navController.popBackStack()
                }
            )
        }
    }
}
