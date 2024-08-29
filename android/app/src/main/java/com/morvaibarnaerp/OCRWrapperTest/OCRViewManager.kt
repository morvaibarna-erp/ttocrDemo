package com.morvaibarnaerp.OCRWrapperTest

// replace with your package

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.facebook.react.uimanager.events.RCTModernEventEmitter


class OCRViewManager(reactContext: ReactApplicationContext) : SimpleViewManager<OCRView>() {

    private var ocrView: OCRView? = null
    companion object {
        const val REACT_CLASS = "OCRViewManager"
    }

    override fun getName(): String {
        return REACT_CLASS
    }

    override fun createViewInstance(reactContext: ThemedReactContext): OCRView {
        val view = OCRView(reactContext)
        return view
    }

    override fun getExportedCustomBubblingEventTypeConstants(): Map<String, Any> {
        return mapOf(
            "topChange" to mapOf(
                "phasedRegistrationNames" to mapOf(
                    "bubbled" to "onChange"
                )
            )
        )
    }

    @ReactProp(name="heightRatio")
    fun setRatioH(view:OCRView, value:Int){
        view.setRatioH(value)
    }
    @ReactProp(name="widthRatio")
    fun setRatioW(view:OCRView, value:Int){
        view.setRatioW(value)
    }
    @ReactProp(name="ocrTimeOut")
    fun setOcrTimeOut(view:OCRView, value:Int){
        view.setOcrTimeOut(value*1000)
        view.setScannerTimeOut(value*1000)
    }

    @ReactProp(name = "expectedBarCode")
    fun setExpectedBarCode(view: OCRView, value: String) {
        view.setExpectedBarCode(value)
    }
}