package com.arefbhrn.marketpay.exception

import com.arefbhrn.marketpay.BillingConnection

class SubsNotSupportedException : IllegalAccessException() {

    override val message: String?
        get() = "Subscription is not supported in this version of installed " + BillingConnection.MARKET_NAME

}
