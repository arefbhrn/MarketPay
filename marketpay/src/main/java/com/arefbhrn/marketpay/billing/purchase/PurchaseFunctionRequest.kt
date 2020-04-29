package com.arefbhrn.marketpay.billing.purchase

import android.content.Intent
import android.content.IntentSender
import com.arefbhrn.marketpay.PurchaseType
import com.arefbhrn.marketpay.billing.FunctionRequest
import com.arefbhrn.marketpay.callback.PurchaseIntentCallback
import com.arefbhrn.marketpay.request.PurchaseRequest

internal class PurchaseFunctionRequest(
    val purchaseRequest: PurchaseRequest,
    val purchaseType: PurchaseType,
    val callback: PurchaseIntentCallback.() -> Unit,
    val fireIntentWithIntentSender: (IntentSender) -> Unit,
    val fireIntentWithIntent: (Intent) -> Unit
) : FunctionRequest