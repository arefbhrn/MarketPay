package com.arefbhrn.marketpay.billing.query

import android.content.Context
import android.os.Bundle
import android.os.RemoteException
import com.android.vending.billing.IInAppBillingService
import com.arefbhrn.marketpay.billing.BillingFunction
import com.arefbhrn.marketpay.callback.PurchaseQueryCallback
import com.arefbhrn.marketpay.config.PaymentConfiguration
import com.arefbhrn.marketpay.config.SecurityCheck
import com.arefbhrn.marketpay.constant.MarketIntent
import com.arefbhrn.marketpay.constant.Billing
import com.arefbhrn.marketpay.entity.PurchaseInfo
import com.arefbhrn.marketpay.exception.ResultNotOkayException
import com.arefbhrn.marketpay.mapper.RawDataToPurchaseInfo
import com.arefbhrn.marketpay.security.PurchaseVerifier
import com.arefbhrn.marketpay.takeIf
import com.arefbhrn.marketpay.thread.MarketPayThread

internal class QueryFunction(
    private val rawDataToPurchaseInfo: RawDataToPurchaseInfo,
    private val purchaseVerifier: PurchaseVerifier,
    private val paymentConfiguration: PaymentConfiguration,
    private val mainThread: MarketPayThread<() -> Unit>,
    private val context: Context
) : BillingFunction<QueryFunctionRequest> {

    override fun function(
        billingService: IInAppBillingService,
        request: QueryFunctionRequest
    ): Unit = with(request) {
        try {
            var continuationToken: String? = null
            do {
                billingService.getPurchases(
                    Billing.IN_APP_BILLING_VERSION,
                    context.packageName,
                    purchaseType.type,
                    continuationToken
                )?.takeIf(
                    thisIsTrue = { bundle ->
                        bundle.get(MarketIntent.RESPONSE_CODE) == MarketIntent.RESPONSE_RESULT_OK
                    },
                    andIfNot = {
                        mainThread.execute {
                            PurchaseQueryCallback().apply(callback)
                                .queryFailed
                                .invoke(ResultNotOkayException())
                        }
                    }
                )?.takeIf(
                    thisIsTrue = { bundle ->
                        bundle.containsKey(MarketIntent.RESPONSE_PURCHASE_ITEM_LIST)
                            .and(bundle.containsKey(MarketIntent.RESPONSE_PURCHASE_DATA_LIST))
                            .and(bundle.containsKey(MarketIntent.RESPONSE_DATA_SIGNATURE_LIST))
                            .and(bundle.getStringArrayList(MarketIntent.RESPONSE_PURCHASE_DATA_LIST) != null)
                    },
                    andIfNot = {
                        mainThread.execute {
                            PurchaseQueryCallback().apply(callback)
                                .queryFailed
                                .invoke(IllegalStateException("Missing data from the received result"))
                        }
                    }
                )?.also { bundle ->
                    continuationToken = bundle.getString(MarketIntent.RESPONSE_CONTINUATION_TOKEN)
                }?.let(::extractPurchasedDataFromBundle)?.also { purchasedItems ->
                    mainThread.execute {
                        PurchaseQueryCallback().apply(callback).querySucceed.invoke(purchasedItems)
                    }
                }
            } while (!continuationToken.isNullOrBlank())
        } catch (e: RemoteException) {
            mainThread.execute {
                PurchaseQueryCallback().apply(callback).queryFailed.invoke(e)
            }
        }
    }

    private fun extractPurchasedDataFromBundle(bundle: Bundle): List<PurchaseInfo> {
        val purchaseDataList: List<String> = bundle.getStringArrayList(
            MarketIntent.RESPONSE_PURCHASE_DATA_LIST
        ) ?: emptyList()
        val signatureDataList: List<String> = bundle.getStringArrayList(
            MarketIntent.RESPONSE_DATA_SIGNATURE_LIST
        ) ?: emptyList()
        val validPurchases = ArrayList<PurchaseInfo>(purchaseDataList.size)
        for (i in purchaseDataList.indices) {
            if (paymentConfiguration.localSecurityCheck is SecurityCheck.Enable) {
                val isPurchaseValid = purchaseVerifier.verifyPurchase(
                    paymentConfiguration.localSecurityCheck.rsaPublicKey,
                    purchaseDataList[i],
                    signatureDataList[i]
                )
                if (!isPurchaseValid) continue
            }
            validPurchases += rawDataToPurchaseInfo.mapToPurchaseInfo(
                purchaseDataList[i],
                signatureDataList[i]
            )
        }
        return validPurchases
    }

}