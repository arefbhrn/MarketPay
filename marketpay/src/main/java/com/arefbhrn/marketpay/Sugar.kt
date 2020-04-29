package com.arefbhrn.marketpay

internal inline fun <T> T.takeIf(thisIsTrue: (T) -> Boolean, andIfNot: () -> Unit): T? {
    return if (thisIsTrue.invoke(this)) {
        this
    } else {
        andIfNot.invoke()
        null
    }
}
