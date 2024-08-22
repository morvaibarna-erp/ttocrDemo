package com.morvaibarnaerp.OCRWrapperTest

// replace with your package

import android.util.Log
import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.annotations.ReactPropGroup

class OCRViewManager(reactContext: ReactApplicationContext) : SimpleViewManager<OCRView>() {

    private var ocrView: OCRView? = null
    companion object {
        const val REACT_CLASS = "OCRViewManager"
    }

    override fun getName(): String {
        return REACT_CLASS
    }

    override fun createViewInstance(reactContext: ThemedReactContext): OCRView {
        return OCRView(reactContext)
    }

    @ReactProp(name="heightRatio")
    fun setRatioH(view:OCRView, value:Int){
        view.setRatioH(value)
    }
    @ReactProp(name="widthRatio")
    fun setRatioW(view:OCRView, value:Int){
        view.setRatioW(value)
    }
    @ReactProp(name = "expectedBarCode")
    fun setExpectedBarCode(view: OCRView,value: String){
        view.setExpectedBarCode(value)
    }
}