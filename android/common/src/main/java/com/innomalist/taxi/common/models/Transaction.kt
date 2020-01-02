package com.innomalist.taxi.common.models

import com.squareup.moshi.JsonClass
import java.sql.Timestamp

@JsonClass(generateAdapter = true)
class Transaction {
    var amount: Double? = null
    val currency: String? = null
    var documentNumber: String? = null
    var operatorId = 0
    var transactionTime: Timestamp? = null
    var details: Any? = null
    var id = 0
    var transactionType: String? = null
    var riderId = 0

    val day: String
        get() = transactionTime!!.date.toString()

    val month: String
        get() {
            val months = arrayOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
            return months[transactionTime!!.month]
        }
}