package com.innomalist.taxi.driver.models

import com.google.gson.annotations.Expose

class Report {
    @Expose
    var date: String? = null
    @Expose
    var amount: Float? = null

}