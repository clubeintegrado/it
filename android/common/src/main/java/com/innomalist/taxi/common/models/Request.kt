package com.innomalist.taxi.common.models

import com.google.android.gms.maps.model.LatLng
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Request(
        var driver: Driver? = null,
        var rider: Rider? = null,
        var cost: Double? = null,
        var startTimestamp: Long? = 0,
        var log: String? = null,
        var distanceBest: Int? = null,
        var rating: Int? = null,
        var isHidden: Int? = null,
        var addresses: List<String> = ArrayList(),
        var points: List<LatLng> = ArrayList(),
        var finishTimestamp: Long? = null,
        var requestTimestamp: Long? = null,
        var etaPickup: Long? = 0,
        var durationBest: Int? = null,
        var costBest: Double? = null,
        var costAfterCoupon: Double? = null,
        var currency: String? = null,
        var durationReal: Long? = null,
        var distanceReal: Long? = null,
        var id: Long? = null,
        var status: Status? = null,
        var imageUrl: String? = null,
        var service: Service? = null,
        var confirmationCode: Int? = null
) {
    enum class Status(val value: String) {
        @Json(name="Requested")
        Requested("Requested"),
        @Json(name="NotFound")
        NotFound("NotFound"),
        @Json(name="NoCloseFound")
        NoCloseFound("NoCloseFound"),
        @Json(name="Found")
        Found("Found"),
        @Json(name="DriverAccepted")
        DriverAccepted("DriverAccepted"),
        @Json(name="Arrived")
        Arrived("Arrived"),
        @Json(name="WaitingForPrePay")
        WaitingForPrePay("WaitingForPrePay"),
        @Json(name="RiderCanceled")
        RiderCanceled("RiderCanceled"),
        @Json(name="DriverCanceled")
        DriverCanceled("DriverCanceled"),
        @Json(name="WaitingForPostPay")
        WaitingForPostPay("WaitingForPostPay"),
        @Json(name="WaitingForReview")
        WaitingForReview("WaitingForReview"),
        @Json(name="Started")
        Started("Started"),
        @Json(name="Booked")
        Booked("Booked"),
        @Json(name="Expired")
        Expired("Expired"),
        @Json(name="Finished")
        Finished("Finished");

        companion object {
            operator fun get(code: String): Status {
                for (s in values()) {
                    if (s.value == code) return s
                }
                return Requested
            }
        }

    }

    
}