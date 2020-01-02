package com.innomalist.taxi.common.networking.http

import com.innomalist.taxi.common.networking.http.interfaces.HTTPRequest

class GetGatewayPublishableToken(gatewayId: Int, token: String): HTTPRequest() {
    override val path: String = "getway_public_token"
    init {
        this.params = mapOf("gatewayId" to gatewayId.toString(), "token" to token)
    }

}