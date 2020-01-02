package com.innomalist.taxi.common.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.model.LatLng

class LocationHelper(private val con: Context) {
    fun loadGps(listener: LocationListener?) {
        locationManager = con.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(con, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(con, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, listener)
    }

    companion object {
        var locationManager: LocationManager? = null
        @JvmStatic
        fun distFrom(latLng1: LatLng, latLng2: LatLng): Int {
            val locationA = Location("LocationA")
            locationA.latitude = latLng1.latitude
            locationA.longitude = latLng1.longitude
            val locationB = Location("LocationB")
            locationB.latitude = latLng2.latitude
            locationB.longitude = latLng2.longitude
            locationA.distanceTo(locationB)
            return locationA.distanceTo(locationB).toInt()
        }

        @JvmStatic
        fun LatLngToDoubleArray(position: LatLng): DoubleArray {
            return doubleArrayOf(position.latitude, position.longitude)
        }

        @JvmStatic
        fun DoubleArrayToLatLng(position: DoubleArray): LatLng {
            return LatLng(position[0], position[1])
        }
    }

}