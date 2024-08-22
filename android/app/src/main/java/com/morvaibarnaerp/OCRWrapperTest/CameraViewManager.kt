package com.morvaibarnaerp.OCRWrapperTest

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class CameraViewManager(reactContext: ReactApplicationContext) : SimpleViewManager<CameraView>() {

    companion object {
        const val REACT_CLASS = "CameraView"
    }

    override fun getName(): String {
        return REACT_CLASS
    }

    override fun createViewInstance(reactContext: ThemedReactContext): CameraView {
        return CameraView(reactContext)
    }
}
