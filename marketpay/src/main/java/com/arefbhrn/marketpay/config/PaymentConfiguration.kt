package com.arefbhrn.marketpay.config

data class PaymentConfiguration(
    val localSecurityCheck: SecurityCheck,
    val shouldSupportSubscription: Boolean = true
)
