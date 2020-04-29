package com.arefbhrn.marketpay.billing.query

import com.arefbhrn.marketpay.PurchaseType
import com.arefbhrn.marketpay.billing.FunctionRequest
import com.arefbhrn.marketpay.callback.PurchaseQueryCallback

internal class QueryFunctionRequest(
    val purchaseType: PurchaseType,
    val callback: PurchaseQueryCallback.() -> Unit
) : FunctionRequest