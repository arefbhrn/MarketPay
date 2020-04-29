package com.arefbhrn.marketpay.thread

import android.os.Handler
import android.os.HandlerThread

internal class BackgroundThread : HandlerThread("MarketPayThread"), MarketPayThread<Runnable> {

    init {
        start()
    }

    private val threadHandler = Handler(looper)

    override fun execute(task: Runnable) {
        threadHandler.post(task)
    }

    override fun dispose() {
        quit()
    }

}
