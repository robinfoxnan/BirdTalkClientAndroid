package com.bird2fish.birdtalksdk.uihelper

import android.content.Context
import android.location.Geocoder
import java.util.Locale

object GeoHelper {

    fun getAddressDetail(context: Context, latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]

                // 拼接更精确的地址
                val city = address.locality ?: ""
                val district = address.subAdminArea ?: ""
                val street = address.thoroughfare ?: ""
                val streetNumber = address.subThoroughfare ?: ""

                "$city $district $street $streetNumber"
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}