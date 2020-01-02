package com.innomalist.taxi.driver.models

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.innomalist.taxi.driver.models.FinishService

class FinishService {
    var log: String?
    var cost: Double
    @SerializedName("confirmation_code")
    var confirmationCode = 0
    var distance: Int

    constructor(log: String?, cost: Double, distance: Int) {
        this.log = log
        this.cost = cost
        this.distance = distance
    }

    constructor(log: String?, cost: Double, distance: Int, confirmationCode: Int) {
        this.log = log
        this.cost = cost
        this.confirmationCode = confirmationCode
        this.distance = distance
    }

    fun toJson(): String {
        val gsonBuilder = GsonBuilder()
        val customGson = gsonBuilder.create()
        return customGson.toJson(this)
    }

    companion object {
        fun fromJson(json: String?): FinishService {
            val gsonBuilder = GsonBuilder()
            val customGson = gsonBuilder.create()
            return customGson.fromJson(json, FinishService::class.java)
        }
    }
}