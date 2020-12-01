package com.arefbhrn.marketpay

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.ServiceConnection
import android.os.IBinder
import androidx.fragment.app.Fragment
import com.android.vending.billing.IInAppBillingService
import com.arefbhrn.marketpay.billing.BillingFunction
import com.arefbhrn.marketpay.billing.consume.ConsumeFunctionRequest
import com.arefbhrn.marketpay.billing.purchase.PurchaseFunctionRequest
import com.arefbhrn.marketpay.billing.query.QueryFunctionRequest
import com.arefbhrn.marketpay.callback.ConnectionCallback
import com.arefbhrn.marketpay.callback.ConsumeCallback
import com.arefbhrn.marketpay.callback.PurchaseIntentCallback
import com.arefbhrn.marketpay.callback.PurchaseQueryCallback
import com.arefbhrn.marketpay.config.PaymentConfiguration
import com.arefbhrn.marketpay.constant.Billing
import com.arefbhrn.marketpay.constant.MarketIntent
import com.arefbhrn.marketpay.exception.DisconnectException
import com.arefbhrn.marketpay.exception.IAPNotSupportedException
import com.arefbhrn.marketpay.exception.MarketNotFoundException
import com.arefbhrn.marketpay.exception.SubsNotSupportedException
import com.arefbhrn.marketpay.request.PurchaseRequest
import com.arefbhrn.marketpay.thread.MarketPayThread

internal class BillingConnection(
    private val context: Context,
    private val paymentConfiguration: PaymentConfiguration,
    private val backgroundThread: MarketPayThread<Runnable>,
    private val purchaseFunction: BillingFunction<PurchaseFunctionRequest>,
    private val consumeFunction: BillingFunction<ConsumeFunctionRequest>,
    private val queryFunction: BillingFunction<QueryFunctionRequest>
) : ServiceConnection {

    private var callback: ConnectionCallback? = null

    private var billingService: IInAppBillingService? = null

    internal fun startConnection(connectionCallback: ConnectionCallback.() -> Unit): Connection {
        val packageName: String? = if (MARKET_PACKAGE_NAME.isEmpty()) null else MARKET_PACKAGE_NAME
        callback = ConnectionCallback(disconnect = ::stopConnection).apply(connectionCallback)
        Intent(BILLING_SERVICE_ACTION).apply { `package` = packageName }
            .takeIf(
                thisIsTrue = {
                    context.packageManager.queryIntentServices(it, 0).isNullOrEmpty().not()
                },
                andIfNot = {
                    callback?.connectionFailed?.invoke(MarketNotFoundException())
                }
            )
            ?.also {
                try {
                    if (packageName.isNullOrEmpty()) {
                        it.setPackage(
                            context.packageManager.queryIntentServices(
                                it,
                                0
                            )[0].serviceInfo.packageName
                        );
                    }
                    context.bindService(it, this, Context.BIND_AUTO_CREATE)
                } catch (e: SecurityException) {
                    callback?.connectionFailed?.invoke(e)
                }
            }
        return requireNotNull(callback)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        IInAppBillingService.Stub.asInterface(service)
            ?.takeIf(
                thisIsTrue = {
                    isPurchaseTypeSupported(
                        purchaseType = PurchaseType.IN_APP,
                        inAppBillingService = it
                    )
                },
                andIfNot = {
                    callback?.connectionFailed?.invoke(IAPNotSupportedException())
                }
            )
            ?.takeIf(
                thisIsTrue = {
                    !paymentConfiguration.shouldSupportSubscription || isPurchaseTypeSupported(
                        purchaseType = PurchaseType.SUBSCRIPTION,
                        inAppBillingService = it
                    )
                },
                andIfNot = {
                    callback?.connectionFailed?.invoke(SubsNotSupportedException())
                }
            )
            ?.also { billingService = it }
            ?.also { callback?.connectionSucceed?.invoke() }
    }

    private fun isPurchaseTypeSupported(
        purchaseType: PurchaseType,
        inAppBillingService: IInAppBillingService
    ): Boolean {
        val supportState = inAppBillingService.isBillingSupported(
            Billing.IN_APP_BILLING_VERSION,
            context.packageName,
            purchaseType.type
        )
        return supportState == MarketIntent.RESPONSE_RESULT_OK
    }

    fun purchase(
        activity: Activity,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
    ) {

        val intentSenderFire: (IntentSender) -> Unit = { intentSender ->
            activity.startIntentSenderForResult(
                intentSender,
                purchaseRequest.requestCode,
                Intent(),
                0,
                0,
                0
            )
            PurchaseIntentCallback().apply(callback).purchaseFlowBegan.invoke()
        }

        val intentFire: (Intent) -> Unit = { intent ->
            activity.startActivityForResult(
                intent,
                purchaseRequest.requestCode
            )
            PurchaseIntentCallback().apply(callback).purchaseFlowBegan.invoke()
        }

        purchase(purchaseRequest, purchaseType, callback, intentSenderFire, intentFire)
    }

    fun purchase(
        fragment: Fragment,
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit
    ) {
        val intentSenderFire: (IntentSender) -> Unit = { intentSender ->
            fragment.startIntentSenderForResult(
                intentSender,
                purchaseRequest.requestCode,
                Intent(),
                0,
                0,
                0,
                null
            )
            PurchaseIntentCallback().apply(callback).purchaseFlowBegan.invoke()
        }

        val intentFire: (Intent) -> Unit = { intent ->
            fragment.startActivityForResult(
                intent,
                purchaseRequest.requestCode
            )
            PurchaseIntentCallback().apply(callback).purchaseFlowBegan.invoke()
        }

        purchase(purchaseRequest, purchaseType, callback, intentSenderFire, intentFire)
    }

    private fun purchase(
        purchaseRequest: PurchaseRequest,
        purchaseType: PurchaseType,
        callback: PurchaseIntentCallback.() -> Unit,
        fireIntentSender: (IntentSender) -> Unit,
        fireIntent: (Intent) -> Unit
    ) = withService {
        purchaseFunction.function(
            billingService = this,
            request = PurchaseFunctionRequest(
                purchaseRequest,
                purchaseType,
                callback,
                fireIntentSender,
                fireIntent
            )
        )
    } ifServiceIsDisconnected {
        PurchaseIntentCallback().apply(callback).failedToBeginFlow.invoke(DisconnectException())
    }

    fun consume(
        purchaseToken: String,
        callback: ConsumeCallback.() -> Unit
    ) = withService(runOnBackground = true) {
        consumeFunction.function(
            billingService = this,
            request = ConsumeFunctionRequest(purchaseToken, callback)
        )
    } ifServiceIsDisconnected {
        ConsumeCallback().apply(callback).consumeFailed.invoke(DisconnectException())
    }

    fun queryPurchasedProducts(
        purchaseType: PurchaseType,
        callback: PurchaseQueryCallback.() -> Unit
    ) = withService(runOnBackground = true) {
        queryFunction.function(
            billingService = this,
            request = QueryFunctionRequest(purchaseType, callback)
        )
    } ifServiceIsDisconnected {
        PurchaseQueryCallback().apply(callback).queryFailed.invoke(DisconnectException())
    }

    private fun stopConnection() {
        if (billingService != null) {
            context.unbindService(this)
            disconnect()
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        disconnect()
    }

    private fun disconnect() {
        billingService = null
        callback?.disconnected?.invoke()
        callback = null
        backgroundThread.dispose()
    }

    private inline fun withService(
        runOnBackground: Boolean = false,
        crossinline service: IInAppBillingService.() -> Unit
    ): ConnectionState {
        return billingService?.also {
            if (runOnBackground) {
                backgroundThread.execute(Runnable { service.invoke(it) })
            } else {
                service.invoke(it)
            }
        }?.let { ConnectionState.Connected }
            ?: run { ConnectionState.Disconnected }
    }

    private inline infix fun ConnectionState.ifServiceIsDisconnected(block: () -> Unit) {
        if (this is ConnectionState.Disconnected) {
            block.invoke()
        }
    }

    companion object {
        const val BILLING_SERVICE_ACTION = BuildConfig.BILLING_SERVICE_ACTION
        const val MARKET_PACKAGE_NAME = BuildConfig.MARKET_APPLICATION_ID
        const val MARKET_NAME = BuildConfig.MARKET_NAME
    }
}
