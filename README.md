<p align="center">
    <img src="./asset/logo_name.png" width="300" align="center"/>
</p>

[![](https://jitpack.io/v/arefbhrn/MarketPay.svg)](https://jitpack.io/#arefbhrn/MarketPay)
-
Android In-App Billing SDK for [Bazaar](https://cafebazaar.ir/?l=en), [Myket](https://myket.ir/?lang=en) and [IranApps](https://myket.ir/?lang=en) App Stores.
## Getting Started
To start working with MarketPay, you need to add it's dependency:
### Dependency
Add into your project-level `build.gradle` file
```groovy
allprojects {
    repositories {
        //...
        maven { url "https://jitpack.io" }
    }
}
```
Add into your app-level `build.gradle` file
```groovy
dependencies {
    //...
    implementation "com.github.arefbhrn:marketpay:[latest_version]:[market_name]@aar"
}
```
### How to use
For more information regarding the usage of MarketPay, Please check out the [wiki](https://github.com/arefbhrn/MarketPay/wiki) page.
### Sample
There is a fully functional sample application which demonstrates the usage of MarketPay, all you have to do is cloning the project and running the [app](https://github.com/arefbhrn/MarketPay/tree/master/app) module.
### Reactive Extension Support
Yes, you've read that right! MarketPay supports Reactive Extension framework. Just add it's dependency in your `build.gradle` file:
```groovy
dependencies {
    // RxJava 3
    implementation "com.github.arefbhrn:marketpay-rx3:[latest_version]:[market_name]@aar"
    // RxJava 2
    implementation "com.github.arefbhrn:marketpay-rx:[latest_version]:[market_name]@aar"
}
```
And instead of using MarketPay's callbacks, use the reactive fuctions:
```kotlin
payment.getPurchasedProducts()
    .subscribe({ purchasedProducts ->
        //...
    }, { throwable ->
        //...
    })
```
---
This library is an extension of [Mahdi Nouri](https://github.com/PHELAT)'s nice library [Poolaky](https://github.com/PHELAT/Poolakey).
Thank you Mahdi! ;)
