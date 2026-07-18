package com.byebyechallan.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

private val Context.vehicleDataStore by preferencesDataStore(name = "byebyechallan_vehicles")

data class LocalVehicle(
    val profileId: Long,
    val registrationNo: String,
    val country: String,
    val state: String,
    val registrationType: String,
    val vehicleType: String,
    val vehicleName: String? = null
)

/**
 * KNOWN LIMITATION - PLEASE READ:
 * The backend list endpoint returns the registration number and vehicle name,
 * but not the country/state/registration/vehicle-type fields needed to reload
 * the document checklist. This cache preserves those local selections after a
 * vehicle is added, and the profile screen merges them with the server list.
 *
 * TRADE-OFF: if the user reinstalls the app or switches devices, this local
 * list is lost (though their actual document data on the backend is safe).
 * RECOMMENDED FIX: extend GET /api/v1/user/{userId}/profile/{profileId}/all-vehicle
 * to include country, state, registration type, and vehicle type.
 */
class VehicleLocalStore(private val context: Context) {

    private val gson = Gson()
    private val key = stringPreferencesKey("known_vehicles")

    private suspend fun readAll(): List<LocalVehicle> {
        val json = context.vehicleDataStore.data.first()[key] ?: return emptyList()
        val type = object : TypeToken<List<LocalVehicle>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getVehiclesForProfile(profileId: Long): List<LocalVehicle> {
        return readAll().filter { it.profileId == profileId }
    }

    suspend fun addVehicle(vehicle: LocalVehicle) {
        val current = readAll().toMutableList()
        // Avoid duplicate entries for the same profile + reg no
        current.removeAll { it.profileId == vehicle.profileId && it.registrationNo == vehicle.registrationNo }
        current.add(vehicle)
        context.vehicleDataStore.edit { prefs ->
            prefs[key] = gson.toJson(current)
        }
    }
}
