package com.morvaibarnaerp.OCRWrapperTest

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import com.facebook.react.uimanager.ThemedReactContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val previewView: PreviewView
    private val cameraExecutor: ExecutorService

    init {
        LayoutInflater.from(context).inflate(R.layout.camera_view, this, true)
        previewView = findViewById(R.id.view_finder)
        cameraExecutor = Executors.newSingleThreadExecutor()

//        previewView = PreviewView(context)
//        previewView.layoutParams = LayoutParams(
//            LayoutParams.MATCH_PARENT,
//            LayoutParams.MATCH_PARENT
//        )
//        addView(previewView, 0)
//
//        previewView.post {
//        }
        startCamera()
        installHierarchyFitter(previewView) // this was the fix
    }

    private fun installHierarchyFitter(view: ViewGroup) {
        if (context is ThemedReactContext) { // only react-native setup
            view.setOnHierarchyChangeListener(object : OnHierarchyChangeListener{
                override fun onChildViewRemoved(parent: View?, child: View?) = Unit
                override fun onChildViewAdded(parent: View?, child: View?) {
                    parent?.measure(
                        MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
                    )
                    parent?.layout(0, 0, parent.measuredWidth, parent.measuredHeight)
                }
            })
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                val lifecycleOwner = ViewTreeLifecycleOwner.get(this)
                if (lifecycleOwner != null) {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                retryCameraInit()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun retryCameraInit() {
        postDelayed({
            startCamera()
        }, 1000) // Retry after 1 second
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cameraExecutor.shutdown()
    }


}
