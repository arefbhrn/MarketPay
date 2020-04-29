package com.arefbhrn.marketpay.billing.consume

import android.content.Context
import android.os.RemoteException
import com.android.vending.billing.IInAppBillingService
import com.arefbhrn.marketpay.billing.BillingFunction
import com.arefbhrn.marketpay.callback.ConsumeCallback
import com.arefbhrn.marketpay.constant.MarketIntent
import com.arefbhrn.marketpay.constant.Billing
import com.arefbhrn.marketpay.exception.ConsumeFailedException
import com.arefbhrn.marketpay.takeIf
import com.arefbhrn.marketpay.thread.MarketPayThread

internal class ConsumeFunction(
    private val mainThread: MarketPayThread<() -> Unit>,
    private val context: Context
) : BillingFunction<ConsumeFunctionRequest> {

    override fun function(
        billingService: IInAppBillingService,
        request: ConsumeFunctionRequest
    ): Unit = with(request) {
        try {
            billingService.consumePurchase(Billing.IN_APP_BILLING_VERSION, context.packageName, purchaseToken)
                .takeIf(
                    thisIsTrue = { it == MarketIntent.RESPONSE_RESULT_OK },
                    andIfNot = {
                        mainThread.execute {
                            ConsumeCallback().apply(callback)
                                .consumeFailed
                                .invoke(ConsumeFailedException())
                        }
                    }
                )
                ?.also {
                    mainThread.execute {
                        ConsumeCallback().apply(callback).consumeSucceed.invoke()
                    }
                }
        } catch (e: RemoteException) {
            mainThread.execute {
                ConsumeCallback().apply(callback).consumeFailed.invoke(e)
            }
        }
    }

}