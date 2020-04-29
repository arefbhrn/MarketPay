package com.arefbhrn.marketpay.callback

import com.arefbhrn.marketpay.entity.PurchaseInfo

class PurchaseCallback {

    internal var purchaseSucceed: (PurchaseInfo) -> Unit = {}

    internal var purchaseCanceled: () -> Unit = {}

    internal var purchaseFailed: (throwable: Throwable) -> Unit = {}

    fun purchaseSucceed(block: (PurchaseInfo) -> Unit) {
        purchaseSucceed = block
    }

    fun purchaseCanceled(block: () -> Unit) {
        purchaseCanceled = block
    }

    fun purchaseFailed(block: (throwable: Throwable) -> Unit) {
        purchaseFailed = block
    }

}
