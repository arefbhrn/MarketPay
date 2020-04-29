package com.arefbhrn.marketpay

import android.content.Intent
import com.arefbhrn.marketpay.callback.PurchaseCallback
import com.arefbhrn.marketpay.config.SecurityCheck
import com.arefbhrn.marketpay.constant.MarketIntent
import com.arefbhrn.marketpay.exception.PurchaseHijackedException
import com.arefbhrn.marketpay.mapper.RawDataToPurchaseInfo
import com.arefbhrn.marketpay.security.PurchaseVerifier
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException

internal class PurchaseResultParser(
    private val rawDataToPurchaseInfo: RawDataToPurchaseInfo,
    private val purchaseVerifier: PurchaseVerifier
) {

    fun handleReceivedResult(
        securityCheck: SecurityCheck,
        data: Intent?,
        purchaseCallback: PurchaseCallback.() -> Unit
    ) {
        if (data?.extras?.get(MarketIntent.RESPONSE_CODE) == MarketIntent.RESPONSE_RESULT_OK) {
            parseResult(securityCheck, data, purchaseCallback)
        } else {
            PurchaseCallback().apply(purchaseCallback)
                .purchaseFailed
                .invoke(IllegalStateException("Response code is not valid"))
        }
    }

    private fun parseResult(
        securityCheck: SecurityCheck,
        data: Intent?,
        purchaseCallback: PurchaseCallback.() -> Unit
    ) {
        val purchaseData = data?.getStringExtra(MarketIntent.RESPONSE_PURCHASE_DATA)
        val dataSignature = data?.getStringExtra(MarketIntent.RESPONSE_SIGNATURE_DATA)
        if (purchaseData != null && dataSignature != null) {
            validatePurchase(
                securityCheck = securityCheck,
                purchaseData = purchaseData,
                dataSignature = dataSignature,
                purchaseIsValid = {
                    val purchaseInfo = rawDataToPurchaseInfo.mapToPurchaseInfo(
                        purchaseData,
                        dataSignature
                    )
                    PurchaseCallback().apply(purchaseCallback)
                        .purchaseSucceed
                        .invoke(purchaseInfo)
                },
                purchaseIsNotValid = { throwable ->
                    PurchaseCallback().apply(purchaseCallback)
                        .purchaseFailed
                        .invoke(throwable)
                }
            )
        } else {
            PurchaseCallback().apply(purchaseCallback)
                .purchaseFailed
                .invoke(IllegalStateException("Received data is not valid"))
        }
    }

    private inline fun validatePurchase(
        securityCheck: SecurityCheck,
        purchaseData: String,
        dataSignature: String,
        purchaseIsValid: () -> Unit,
        purchaseIsNotValid: (Throwable) -> Unit
    ) {
        if (securityCheck is SecurityCheck.Enable) {
            try {
                val isPurchaseValid = purchaseVerifier.verifyPurchase(
                    securityCheck.rsaPublicKey,
                    purchaseData,
                    dataSignature
                )
                if (isPurchaseValid) {
                    purchaseIsValid.invoke()
                } else {
                    purchaseIsNotValid.invoke(PurchaseHijackedException())
                }
            } catch (e: NoSuchAlgorithmException) {
                purchaseIsNotValid.invoke(e)
            } catch (e: InvalidKeySpecException) {
                purchaseIsNotValid.invoke(e)
            } catch (e: InvalidKeyException) {
                purchaseIsNotValid.invoke(e)
            } catch (e: SignatureException) {
                purchaseIsNotValid.invoke(e)
            } catch (e: IllegalArgumentException) {
                purchaseIsNotValid.invoke(e)
            }
        } else {
            purchaseIsValid.invoke()
        }
    }

}
