package com.morvaibarnaerp.OCRWrapperTest

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewTreeLifecycleOwner
import com.facebook.react.bridge.UiThreadUtil.runOnUiThread
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.morvaibarnaerp.OCRWrapperTest.Constants.DETECT_MODEL
import com.morvaibarnaerp.OCRWrapperTest.Constants.LABELS_PATH
import com.morvaibarnaerp.OCRWrapperTest.Constants.RECOGNITION_MODEL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class OCRView(context: Context,
              attrs: AttributeSet? = null,
              defStyleAttr: Int = 0
) :Detector.DetectorListener, FrameLayout(context, attrs, defStyleAttr) {

    private val isFrontCamera = false

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var detector: Detector? = null
    private var barcode: String? = null
    private var expectedBarCode: String = ""
    private var ocrRunTime = SystemClock.elapsedRealtime()
    private var firstRun = true
    private var showToast = true

    private lateinit var cameraExecutor: ExecutorService

    private var viewFinder: PreviewView
    private var overlay: OverlayView
    private var ratioRectangleView: RatioRectangleView

    fun setRatioH(value:Int){
        ratioRectangleView.setHeightRatio(value)
    }
    fun setRatioW(value:Int){
        ratioRectangleView.setWidthRatio(value)
    }
    fun setExpectedBarCode(value:String){
        this.expectedBarCode = value
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.camera_view, this, true)
        viewFinder = findViewById(R.id.view_finder)
        overlay = findViewById(R.id.overlay)
        ratioRectangleView = findViewById(R.id.ratioRectangleView)
        ratioRectangleView.invalidate()

        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraExecutor.execute {
            detector = Detector(context, DETECT_MODEL, RECOGNITION_MODEL, LABELS_PATH, this) {
                toast(it)
            }
        }
        startCamera()
        installHierarchyFitter(viewFinder) // this was the fix
    }

    private fun retryCameraInit() {
        postDelayed({
            startCamera()
        }, 1000) // Retry after 1 second
    }

    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(context))
    }
    private fun toast(message: String) {
        runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    override fun onEmptyDetect() {
        runOnUiThread {
            overlay.clear()
        }
    }
    override fun onDetect(
        boundingBoxes: List<BoundingBox>,
        inferenceTime: Long,
        result: String,
        resultPercentage: String
    ) {
        runOnUiThread {
//            inferenceTime.text = "${inferenceTime}ms"
//            resultText.text = "${result}"
//            resultPercentage.text = "${resultPercentage}"
            overlay.apply {
                setResults(boundingBoxes)
                invalidate()
            }
        }
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

    private fun bindCameraUseCases() {

        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = viewFinder.display.rotation

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

//        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
//            .setTargetRotation(rotation).build()

        preview = Preview.Builder().setTargetRotation(rotation).setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
            .also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            val bitmapBuffer =
                Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

                if (isFrontCamera) {
                    postScale(
                        -1f,
                        1f,
                        imageProxy.width.toFloat(),
                        imageProxy.height.toFloat()
                    )
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )


            if (barcode == expectedBarCode) {
                detector?.detect(rotatedBitmap)
                if (detector?.getSuccess() == true) {
                    detector?.setSuccess(false)
//                    openConfirm(detector?.getResult().toString())
                }
            } else {
                if (firstRun) {
                    ocrRunTime = SystemClock.elapsedRealtime()
                    firstRun = false
                }
                if (SystemClock.elapsedRealtime() - ocrRunTime <= 10000) {
                    expectedBarCode?.let { getGyariSzam(rotatedBitmap, it) }
//                    Log.e(TAG, (SystemClock.elapsedRealtime() - ocrRunTime).toString())
                } else if (SystemClock.elapsedRealtime() - ocrRunTime <= 20000) {
                    expectedBarCode?.let { scanBarcodes(rotatedBitmap, it) }
//                    Log.e(TAG, (SystemClock.elapsedRealtime() - ocrRunTime).toString())
                } else {
                    if (showToast) {
                        toast("Nincs találat a megadott gyári számra, kérjük adjon meg újat!")
                        showToast = false
                    }
                }
            }


//            cameraProvider.unbindAll()
        }
        try {
            Handler(Looper.getMainLooper()).post{
            val lifecycleOwner = ViewTreeLifecycleOwner.get(this)
            if (lifecycleOwner != null) {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            }
//            preview?.setSurfaceProvider(viewFinder.surfaceProvider)
        }
    }
        catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
    }

    @SuppressLint("SetTextI18n")
    private fun scanBarcodes(bitmap: Bitmap, expectedBarCode: String) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val options =
            BarcodeScannerOptions.Builder().enableAllPotentialBarcodes()
//            setBarcodeFormats(
//            Barcode.FORMAT_CODE_128,
//            Barcode.FORMAT_EAN_13,
//            Barcode.FORMAT_UPC_E,
//            Barcode.FORMAT_UPC_A
                //            )
                .build()
        val scanner = BarcodeScanning.getClient()

        scanner.process(image).addOnSuccessListener { barcodes ->
            for (barcode in barcodes) {
                val rawValue = barcode.rawValue
                val valueType = barcode.format
                Log.e(TAG, valueType.toString())
                if (expectedBarCode == rawValue) {
                    this.barcode = rawValue
//                    binding.gyariSzam.text = "Kapott: ${this.barcode}"
                    Log.e("Gyari szam", this.barcode!!)
                    break
                }
            }
        }.addOnFailureListener {}

        // [END run_detector]
    }

    @SuppressLint("SetTextI18n")
    private fun getGyariSzam(bitmap: Bitmap, expectedBarCode: String) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image).addOnSuccessListener { visionText ->
            for (block in visionText.textBlocks) {
                val text = block.text
                if (text.contains(expectedBarCode)) {
                    this.barcode = expectedBarCode
                    Log.e("Gyari szam", this.barcode!!)
//                    binding.gyariSzam.text = "Kapott: ${this.barcode}"
                    break
                }
            }

        }.addOnFailureListener { e ->
            // Task failed with an exception
            // ...

        }


    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "Camera"
    }
}
