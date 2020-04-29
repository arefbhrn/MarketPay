package com.arefbhrn.marketpay

sealed class ConnectionState {

    object Connected : ConnectionState()

    object FailedToConnect : ConnectionState()

    object Disconnected : ConnectionState()

}
