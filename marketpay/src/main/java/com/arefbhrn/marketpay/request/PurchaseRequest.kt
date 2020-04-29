package com.arefbhrn.marketpay.request

data class PurchaseRequest(
    val productId: String,
    val requestCode: Int,
    val payload: String? = null
)
