package com.byebyechallan.app.ui.navigation

/**
 * Every screen's route lives here. Centralizing this avoids typos in route
 * strings scattered across the codebase - always reference Screen.X.route.
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object CreateProfile : Screen("create_profile")

    object ProfileDetail : Screen("profile_detail/{profileId}/{profileName}") {
        fun createRoute(profileId: Long, profileName: String) =
            "profile_detail/$profileId/$profileName"
    }

    object AddVehicle : Screen("add_vehicle/{profileId}") {
        fun createRoute(profileId: Long) = "add_vehicle/$profileId"
    }

    object VehicleDetail : Screen(
        "vehicle_detail/{profileId}/{vehicleRegNo}/{country}/{state}/{registrationType}/{vehicleType}"
    ) {
        fun createRoute(
            profileId: Long,
            vehicleRegNo: String,
            country: String,
            state: String,
            registrationType: String,
            vehicleType: String
        ) = "vehicle_detail/$profileId/$vehicleRegNo/$country/$state/$registrationType/$vehicleType"
    }

    object DocumentUpload : Screen(
        "document_upload/{profileId}/{vehicleRegNo}/{docTemplateId}/{docName}"
    ) {
        fun createRoute(
            profileId: Long,
            vehicleRegNo: String,
            docTemplateId: String,
            docName: String
        ) = "document_upload/$profileId/$vehicleRegNo/$docTemplateId/$docName"
    }
}
