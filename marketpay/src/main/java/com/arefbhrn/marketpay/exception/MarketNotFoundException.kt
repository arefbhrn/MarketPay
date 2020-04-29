package com.arefbhrn.marketpay.exception

import com.arefbhrn.marketpay.BillingConnection

class MarketNotFoundException : IllegalStateException() {

    override val message: String?
        get() = BillingConnection.MARKET_NAME + " is not installed"

}
