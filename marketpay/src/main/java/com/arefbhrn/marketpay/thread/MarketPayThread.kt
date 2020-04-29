package com.arefbhrn.marketpay.thread

internal interface MarketPayThread<TaskType> {

    fun execute(task: TaskType)

    fun dispose()

}
