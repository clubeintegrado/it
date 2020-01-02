package com.innomalist.taxi.rider.networking.socket

import com.innomalist.taxi.common.models.Review
import com.innomalist.taxi.common.models.Rider
import com.innomalist.taxi.common.networking.socket.interfaces.SocketRequest
import com.innomalist.taxi.common.utils.Adapters
import org.json.JSONObject

class UpdateProfile(user: Rider): SocketRequest() {
    init {
        val rd = Adapters.moshi.adapter<Rider>(Rider::class.java).toJsonValue(user) as JSONObject
        this.params = arrayOf(rd)
    }
}