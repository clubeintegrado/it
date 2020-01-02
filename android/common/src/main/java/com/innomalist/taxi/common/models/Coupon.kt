package com.innomalist.taxi.common.models

import com.squareup.moshi.JsonClass
import java.io.Serializable
import java.sql.Timestamp
import java.util.*

@JsonClass(generateAdapter = true)
class Coupon : Serializable {
    var isEnabled = 0
    var manyUsersCanUse = 0
    var manyTimesUserCanUse = 0
    var flatDiscount: Double = 0.0
    var code: String? = null
    var description: String? = null
    var id = 0
    var title: String? = null
    var startAt: Long = 0
    var expirationAt: Long = 0
    var discountPercent = 0
    var isFirstTravelOnly = 0
    var daysLeft = 0
        get() {
            field = ((expirationAt - Date().time) / (1000 * 60 * 60 * 24)).toInt()
            return field
        }

}