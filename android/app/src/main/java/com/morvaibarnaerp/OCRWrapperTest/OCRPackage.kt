package com.morvaibarnaerp.OCRWrapperTest

// replace with your package

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class OCRPackage : ReactPackage {
    override fun createNativeModules(
        reactContext: ReactApplicationContext
    ): MutableList<NativeModule> = listOf(OCRViewManager(reactContext)).toMutableList()

    override fun createViewManagers(
        reactContext: ReactApplicationContext
    ) = listOf(OCRViewManager(reactContext))
}