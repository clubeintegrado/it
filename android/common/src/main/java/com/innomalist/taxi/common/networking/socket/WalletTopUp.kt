package com.innomalist.taxi.common.networking.socket

import com.innomalist.taxi.common.networking.socket.interfaces.SocketRequest

class WalletTopUp(gatewayId: Int, currency: String, token: String, amount: Double) : SocketRequest() {
    init {
        this.params = arrayOf(gatewayId, currency, token, amount)
    }
}