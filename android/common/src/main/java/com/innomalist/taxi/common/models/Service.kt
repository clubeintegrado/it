package com.innomalist.taxi.common.models

import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
class Service : Serializable {
    enum class DistanceFee {
        None, PickupToDestination
    }

    enum class FeeEstimationMode {
        Static, Dynamic, Ranged, RangedStrict, Disabled
    }

    enum class PaymentMethod {
        CashCredit, OnlyCredit, OnlyCash
    }

    enum class PaymentTime {
        PrePay, PostPay
    }

    enum class QuantityMode {
        Singular, Multiple
    }

    var serviceCategory: ServiceCategory? = null
    var media: Media? = null
    val availableTimeFrom: String? = null
    var perHundredMeters = 0.0
    val availableTimeTo: String? = null
    var perMinuteDrive = 0.0
    var rangeMinusPercent: Int? = null
    var rangePlusPercent: Int? = null
    var baseFare = 0.0
    var id: Long = 0
    var title: String? = null
    var perMinuteWait = 0.0
    var cost = 0.0
    var canUseConfirmationCode: Int = 0
    var distanceFeeMode: DistanceFee? = null
    var feeEstimationMode: FeeEstimationMode? = null
    var paymentMethod: PaymentMethod? = null
    var paymentTime: PaymentTime? = null
    var quantityMode: QuantityMode? = null

}