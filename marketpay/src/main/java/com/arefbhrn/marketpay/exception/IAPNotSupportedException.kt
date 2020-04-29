package com.arefbhrn.marketpay.exception

import com.arefbhrn.marketpay.BillingConnection

class IAPNotSupportedException : IllegalAccessException() {

    override val message: String?
        get() = "In app billing is not supported in this version of installed " + BillingConnection.MARKET_NAME

}
