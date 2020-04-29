package com.arefbhrn.marketpay.rxbase.exception

import java.lang.Exception

class PurchaseCanceledException : Exception() {

    override val message: String?
        get() = "Purchase canceled by user"

}
