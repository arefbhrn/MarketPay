package com.arefbhrn.marketpay.exception

import com.arefbhrn.marketpay.BillingConnection

class ResultNotOkayException : IllegalStateException() {

    override val message: String?
        get() = "Failed to receive response from " + BillingConnection.MARKET_NAME

}
