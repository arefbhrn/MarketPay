package com.arefbhrn.marketpay.exception

import com.arefbhrn.marketpay.BillingConnection

class DisconnectException : IllegalStateException() {

    override val message: String?
        get() = "We can't communicate with " + BillingConnection.MARKET_NAME + ": Service is disconnected"

}
