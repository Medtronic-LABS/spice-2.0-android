package com.medtroniclabs.opensource.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class SpiceLocationManager(private val context: Context) {

    private val fusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun getCurrentLocation(callback: (Location) -> Unit) {

        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            fusedLocationProviderClient.lastLocation.addOnSuccessListener { lastLocation ->
                lastLocation?.let {
                    SecuredPreference.putDouble(
                        SecuredPreference.EnvironmentKey.CURRENT_LATITUDE.name,
                        it.latitude
                    )
                    SecuredPreference.putDouble(
                        SecuredPreference.EnvironmentKey.CURRENT_LONGITUDE.name,
                        it.longitude
                    )
                    callback(it)
                }

                val priority = Priority.PRIORITY_HIGH_ACCURACY
                val cancellationTokenSource = CancellationTokenSource()

                fusedLocationProviderClient.getCurrentLocation(
                    priority,
                    cancellationTokenSource.token
                ).addOnSuccessListener { currentLocation ->
                    currentLocation?.let {
                        SecuredPreference.putDouble(
                            SecuredPreference.EnvironmentKey.CURRENT_LATITUDE.name,
                            it.latitude
                        )
                        SecuredPreference.putDouble(
                            SecuredPreference.EnvironmentKey.CURRENT_LONGITUDE.name,
                            it.longitude
                        )
                        callback(it)
                    }
                }
            }
        }
    }
}