package com.arefbhrn.marketpay.exception

import android.os.RemoteException
import com.arefbhrn.marketpay.BillingConnection

class ConsumeFailedException : RemoteException() {

    override val message: String?
        get() = "Consume request failed: It's from " + BillingConnection.MARKET_NAME

}
