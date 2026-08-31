package com.atruedev.kmpnfc.sample.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

@Composable
actual fun rememberOpenNfcSettingsAction(): () -> Unit =
    remember {
        {
            val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
            if (url != null) {
                UIApplication.sharedApplication.openURL(
                    url,
                    options = emptyMap<Any?, Any>(),
                    completionHandler = null,
                )
            }
        }
    }
