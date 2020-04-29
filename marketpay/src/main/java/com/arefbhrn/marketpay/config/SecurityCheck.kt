package com.arefbhrn.marketpay.config

/**
 * You can use this class to disable or enable local security checks for purchases and queries.
 * Note that it's highly recommended to disable local security checks and use Market's REST API to
 * validate a purchase. You can check out Market's documentation
 * @see Disable
 * @see Enable
 */
sealed class SecurityCheck {

    /**
     * You have to use this object in order to disable local security checks.
     */
    object Disable : SecurityCheck()

    /**
     * You have to use this class in order to enable local security checks. You can access to your
     * app's public rsa key from Market's developer panel, under "In-App Billing" tab
     */
    data class Enable(val rsaPublicKey: String) : SecurityCheck()

}
