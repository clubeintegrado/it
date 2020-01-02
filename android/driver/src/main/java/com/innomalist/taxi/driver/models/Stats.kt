package com.innomalist.taxi.driver.models

import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose

class Stats {
    @Expose
    var amount: Float? = null
    @Expose
    var services: Int? = null
    @Expose
    var rating: Float? = null

    fun fromJson(json: String?): Stats {
        return GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, Stats::class.java)
    }

}