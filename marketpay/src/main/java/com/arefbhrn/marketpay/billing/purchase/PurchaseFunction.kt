package com.arefbhrn.marketpay.billing.purchase

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.RemoteException
import com.android.vending.billing.IInAppBillingService
import com.arefbhrn.marketpay.BillingConnection
import com.arefbhrn.marketpay.PurchaseType
import com.arefbhrn.marketpay.billing.BillingFunction
import com.arefbhrn.marketpay.callback.PurchaseIntentCallback
import com.arefbhrn.marketpay.constant.Billing
import com.arefbhrn.marketpay.constant.MarketIntent
import com.arefbhrn.marketpay.exception.ResultNotOkayException
import com.arefbhrn.marketpay.request.PurchaseRequest
import com.arefbhrn.marketpay.takeIf

internal class PurchaseFunction(
    private val context: Context
) : BillingFunction<PurchaseFunctionRequest> {

    override fun function(
        billingService: IInAppBillingService,
        request: PurchaseFunctionRequest
    ): Unit = with(request) {
        try {
            val purchaseConfigBundle = billingService.getPurchaseConfig(
                Billing.IN_APP_BILLING_VERSION
            )

            val intentResponseIsNullError = {
                PurchaseIntentCallback().apply(callback)
                    .failedToBeginFlow
                    .invoke(IllegalStateException("Couldn't receive buy intent from " + BillingConnection.MARKET_NAME))
            }

            if (doesClientSupportIntentV2(purchaseConfigBundle)) {
                getBuyIntentV2FromBillingService(
                    billingService,
                    purchaseRequest,
                    purchaseType,
                    callback
                )?.takeIf(
                    thisIsTrue = { bundle ->
                        bundle.getParcelable<PendingIntent>(INTENT_RESPONSE_BUY) != null
                    }, andIfNot = intentResponseIsNullError
                )?.getParcelable<Intent>(INTENT_RESPONSE_BUY)?.also { purchaseIntent ->
                    fireIntentWithIntent.invoke(purchaseIntent)
                }
            } else {
                getBuyIntentFromBillingService(
                    billingService,
                    purchaseRequest,
                    purchaseType,
                    callback
                )?.takeIf(
                    thisIsTrue = { bundle ->
                        bundle.getParcelable<PendingIntent>(INTENT_RESPONSE_BUY) != null
                    }, andIfNot = intentResponseIsNullError
                )?.getParcelable<PendingIntent>(INTENT_RESPONSE_BUY)?.also { purchaseIntent ->
                    fireIntentWithIntentSender.invoke(purchaseIntent.intentSender)
                }
            }
        } catch (e: RemoteException) {
            PurchaseIntentCallback().apply(callback).failedToBeginFlow.invoke(e)
        }
    }

    private inline fun getBuyIntentFromBillingService(
        billingService: IInAppBillingService,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
    ) = billingService.getBuyIntent(
        Billing.IN_APP_BILLING_VERSION,
        context.packageName,
        purchaseRequest.productId,
        purchaseType.type,
        purchaseRequest.payload
    )?.takeBundleIfResponseIsOk(callback)

    private inline fun getBuyIntentV2FromBillingService(
        billingService: IInAppBillingService,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
    ) = billingService.getBuyIntentV2(
        Billing.IN_APP_BILLING_VERSION,
        context.packageName,
        purchaseRequest.productId,
        purchaseType.type,
        purchaseRequest.payload
    )?.takeBundleIfResponseIsOk(callback)

    private inline fun Bundle.takeBundleIfResponseIsOk(
        callback: PurchaseIntentCallback.() -> Unit
    ): Bundle? = takeIf(
        thisIsTrue = { bundle ->
            bundle.get(MarketIntent.RESPONSE_CODE) == MarketIntent.RESPONSE_RESULT_OK
        }, andIfNot = {
            PurchaseIntentCallback().apply(callback)
                .failedToBeginFlow
                .invoke(ResultNotOkayException())
        }
    )

    private fun doesClientSupportIntentV2(purchaseConfigBundle: Bundle?): Boolean {
        return purchaseConfigBundle?.getBoolean(INTENT_V2_SUPPORT) ?: false
    }

    companion object {
        private const val INTENT_RESPONSE_BUY = "BUY_INTENT"
        private const val INTENT_V2_SUPPORT = "INTENT_V2_SUPPORT"
    }

}