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
 * The backend API (as of the Swagger spec provided) has no endpoint to list all
 * vehicles under a profile - only to fetch documents for a vehicle you already
 * know the registration number of. Since vehicles are created implicitly on
 * first document upload (per your decision), this class caches the list of
 * vehicles locally on-device so the Profile screen has something to display.
 *
 * TRADE-OFF: if the user reinstalls the app or switches devices, this local
 * list is lost (though their actual document data on the backend is safe).
 * RECOMMENDED FIX: ask backend to add GET /api/v1/user/{userId}/profile/{profileId}/vehicles
 * that returns distinct vehicle registration numbers + their stored type info.
 * Once that exists, swap this local cache for a real repository call - the rest
 * of the app's screens don't need to change.
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
