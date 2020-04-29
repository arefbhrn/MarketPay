package com.arefbhrn.marketpay.billing.consume

import com.arefbhrn.marketpay.billing.FunctionRequest
import com.arefbhrn.marketpay.callback.ConsumeCallback

internal class ConsumeFunctionRequest(
    val purchaseToken: String,
    val callback: ConsumeCallback.() -> Unit
): FunctionRequest