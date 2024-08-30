package com.morvaibarnaerp.OCRWrapperTest

import android.R.attr.button
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.vectordrawable.graphics.drawable.AnimationUtilsCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.UiThreadUtil.runOnUiThread
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.uimanager.ThemedReactContext
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.morvaibarnaerp.OCRWrapperTest.Constants.DETECT_MODEL
import com.morvaibarnaerp.OCRWrapperTest.Constants.LABELS_PATH
import com.morvaibarnaerp.OCRWrapperTest.Constants.RECOGNITION_MODEL
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class OCRView(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : Detector.DetectorListener, FrameLayout(context, attrs, defStyleAttr) {

    private val isFrontCamera = false

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var detector: Detector? = null
    private var barcode: String? = null
    private var expectedBarCode: String? = null
    private var ocrRunTime = SystemClock.elapsedRealtime()
    private var firstRun = true
    private var showToast = true
    private var ocrTimeOut = 10000
    private var scannerTimeOut = 10000
    private var success = false

    private var cameraExecutor: ExecutorService

    private var viewFinder: PreviewView

    //    private var overlay: OverlayView
    private var ratioRectangleView: RatioRectangleView

    private var initProgressBar: ProgressBar
    private var initTickImageView: ImageView
    private var initXImageView: ImageView

    private var kwhProgressBar: ProgressBar
    private var kwhTickImageView: ImageView
    private var kwhXImageView: ImageView

    private var barCodeProgressBar: ProgressBar
    private var barCodeTickImageView: ImageView
    private var barCodeXImageView: ImageView

    private var initStatusBar: LinearLayout
    private var statusBar: LinearLayout

    private var barCodeValue: TextView
    private var kwhValue: TextView
    private var capsuleText: TextView

    private var torchButton: ImageButton
    private var isTorchOn: Boolean = false
    private var closeButton: FrameLayout


    private var gyariSzamTalalat = false

    fun setRatioH(value: Int) {
        ratioRectangleView.setHRatio(value)
    }

    fun setRatioW(value: Int) {
        ratioRectangleView.setWRatio(value)
    }

    fun setExpectedBarCode(value: String) {
        this.expectedBarCode = value
    }

    fun setOcrTimeOut(value: Int) {
        this.ocrTimeOut = value
    }

    fun setScannerTimeOut(value: Int) {
        this.scannerTimeOut = value
    }

    private fun sendEventToReactNative(eventName: String, message: WritableMap) {
        val reactContext = context as ReactContext
        val emitter = reactContext.getJSModule(
            DeviceEventManagerModule.RCTDeviceEventEmitter::class.java
        )
        emitter.emit(eventName, message)
    }


    init {
        LayoutInflater.from(context).inflate(R.layout.camera_view, this, true)
//        animation = AnimationUtils.

        viewFinder = findViewById(R.id.view_finder)
//        overlay = findViewById(R.id.overlay)
        ratioRectangleView = findViewById(R.id.ratioRectangleView)
        capsuleText = findViewById(R.id.capsuleText)

        initProgressBar = findViewById(R.id.initProgressBar)
        initTickImageView = findViewById(R.id.initTickImageView)
        initXImageView = findViewById(R.id.initXImageView)

        kwhProgressBar = findViewById(R.id.kwhProgressBar)
        kwhTickImageView = findViewById(R.id.kwhTickImageView)
        kwhXImageView = findViewById(R.id.kwhXImageView)

        barCodeProgressBar = findViewById(R.id.barCodeProgressBar)
        barCodeTickImageView = findViewById(R.id.barCodeTickImageView)
        barCodeXImageView = findViewById(R.id.barCodeXImageView)

        initStatusBar = findViewById(R.id.initCapsuleContainer)
        statusBar = findViewById(R.id.capsuleContainer)

        barCodeValue = findViewById(R.id.barCodeValue)
        kwhValue = findViewById(R.id.kwhValue)

        torchButton = findViewById(R.id.torchButton)
        closeButton = findViewById(R.id.closeButton)

        closeButton.setOnClickListener() {
            runOnUiThread {
                closeButton.animate().apply {
                    duration = 50
                    scaleX(0.8f)
                    scaleY(0.8f)
                }.withEndAction {
                    closeButton.animate().apply {
                        duration = 50
                        scaleX(1f)
                        scaleY(1f)
                    }
                }.start()
            }
            val dataToSend: WritableMap = Arguments.createMap()
            dataToSend.putString("value", "false")
            sendEventToReactNative("NoBarCode", dataToSend)
        }

        torchButton.setOnClickListener {

            if (camera!!.cameraInfo.hasFlashUnit() and !isTorchOn) {
                camera!!.cameraControl.enableTorch(true)
            } else {
                camera!!.cameraControl.enableTorch(false)
            }
            runOnUiThread {
                torchButton.animate().apply {
                    duration = 50
                    scaleX(0.8f)
                    scaleY(0.8f)
                }.withEndAction {
                    torchButton.animate().apply {
                        duration = 50
                        scaleX(1f)
                        scaleY(1f)
                    }
                }.start()
                if (isTorchOn) {
                    torchButton.setImageResource(R.drawable.flashlight_off)
                } else
                    torchButton.setImageResource(R.drawable.flashlight_on)
            }
            isTorchOn = !isTorchOn


//

        }

        ratioRectangleView.invalidate()
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraExecutor.execute {
            detector = Detector(context, DETECT_MODEL, RECOGNITION_MODEL, LABELS_PATH, this) {
                toast(it)
            }
        }
        startCamera()
        installHierarchyFitter(viewFinder)
        showProgress(1)
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
//            overlay.clear()
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
//                setResults(boundingBoxes)
                invalidate()
            }
        }
    }

    private fun installHierarchyFitter(view: ViewGroup) {
        if (context is ThemedReactContext) { // only react-native setup
            view.setOnHierarchyChangeListener(object : OnHierarchyChangeListener {
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

        preview = Preview.Builder().setTargetRotation(rotation)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9).build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }
        imageAnalyzer = ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build()
        val cropRect: Rect = ratioRectangleView.getRectangle()
        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
//            imageProxy.setCropRect(cropRect)
            val bitmapBuffer = Bitmap.createBitmap(
                imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
            )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()
            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

                if (isFrontCamera) {
                    postScale(
                        -1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat()
                    )
                }
            }

            val rotated2Bitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true
            )
            val rotatedBitmap = Bitmap.createBitmap(
                rotated2Bitmap, cropRect.left, cropRect.top, cropRect.right, cropRect.bottom
            )

            runOnUiThread() {
                barCodeValue.text = expectedBarCode
            }
            if (barcode == expectedBarCode && !success) {
                showProgress(3)
                if (detector?.getSuccess() == true) {
                    detector?.setSuccess(false)
                    var value = detector?.getResult()
                    if (value != null) {
                        showTick(3)
                        
                        val file = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                            (0..1000).random().toString() + ".jpg"
                        )
                        if (!file.exists()) {
                            try {
                                FileOutputStream(file).use { out ->
                                    rotatedBitmap.compress(
                                        Bitmap.CompressFormat.JPEG, 100, out
                                    )
                                    out.flush()
                                    out.close()
                                }
                                Log.e("file", file.toString())

                                val dataToSend: WritableMap = Arguments.createMap()
                                dataToSend.putString("savedImagePath", file.toString())
                                dataToSend.putString("allas", value)
                                sendEventToReactNative("displayHit", dataToSend)
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }

                    }
                    overlay.clear()
                    success = true
                } else {
                    detector?.detect(rotatedBitmap)
                }
            } else if (!success) {
                if (firstRun) {
                    ocrRunTime = SystemClock.elapsedRealtime()
                    firstRun = false
                }
//                capsuleText.text = "Gyári szám keresése"
                showStatusBar()
                showProgress(2)

                if (SystemClock.elapsedRealtime() - ocrRunTime <= this.ocrTimeOut && !this.gyariSzamTalalat) {
                    expectedBarCode?.let { getGyariSzam(rotatedBitmap, it) }
                } else if (SystemClock.elapsedRealtime() - ocrRunTime <= (this.ocrTimeOut + this.scannerTimeOut) && !this.gyariSzamTalalat) {
                    expectedBarCode?.let { scanBarcodes(rotatedBitmap, it) }
                } else {
                    showX(2)
                    if (showToast) {
                        runOnUiThread {
                            AlertDialog.Builder(context).setTitle("Nem található gyári szám!")
                                .setMessage("Kérjük ellenőrizze, hogy a mérőóra teljesen benne van-e a keretben és a gyári szám, valamint a vonalkód jól olvasható!") // Specifying a listener allows you to take an action before dismissing the dialog.
                                .setCancelable(false).setNeutralButton(
                                    "Újra"
                                ) { _, _ ->
                                    val dataToSend: WritableMap = Arguments.createMap()
                                    dataToSend.putString("value", "false")
                                    sendEventToReactNative("NoBarCode", dataToSend)
                                }.show()
                        }
                        showToast = false
                    }
                }
            }
        }
        try {
            Handler(Looper.getMainLooper()).post {
                val lifecycleOwner = ViewTreeLifecycleOwner.get(this)
                if (lifecycleOwner != null) {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, imageAnalyzer
                    )

                }
//            preview?.setSurfaceProvider(viewFinder.surfaceProvider)
            }
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun showStatusBar() {
        runOnUiThread() {

            statusBar.visibility = VISIBLE
            initStatusBar.visibility = INVISIBLE
        }
    }

    private fun showProgress(type: Int) {
        //1: init, 2: barcode, 3: kwh

        runOnUiThread() {

            when (type) {
                1 -> {
                    initProgressBar.visibility = VISIBLE
                    initTickImageView.setVisibility(INVISIBLE)
                    initXImageView.setVisibility(INVISIBLE)
                }

                2 -> {
                    barCodeProgressBar.visibility = VISIBLE
                    barCodeTickImageView.setVisibility(INVISIBLE)
                    barCodeXImageView.setVisibility(INVISIBLE)
                }

                else -> {
                    kwhProgressBar.visibility = VISIBLE
                    kwhTickImageView.setVisibility(INVISIBLE)
                    kwhXImageView.setVisibility(INVISIBLE)
                }
            }
        }
    }

    private fun showTick(type: Int) {
        //1: init, 2: barcode, 3: kwh

        runOnUiThread() {

            when (type) {
                1 -> {

                    initProgressBar.visibility = INVISIBLE
                    initTickImageView.setVisibility(VISIBLE)
                    initXImageView.setVisibility(INVISIBLE)
                }

                2 -> {
                    barCodeProgressBar.visibility = INVISIBLE
                    barCodeTickImageView.setVisibility(VISIBLE)
                    barCodeXImageView.setVisibility(INVISIBLE)
                }

                else -> {
                    kwhProgressBar.visibility = INVISIBLE
                    kwhTickImageView.setVisibility(VISIBLE)
                    kwhXImageView.setVisibility(INVISIBLE)
                }
            }
        }
    }

    private fun showX(type: Int) {
        //1: init, 2: barcode, 3: kwh

        runOnUiThread() {

            when (type) {
                1 -> {

                    initProgressBar.visibility = INVISIBLE
                    initTickImageView.setVisibility(INVISIBLE)
                    initXImageView.setVisibility(VISIBLE)
                }

                2 -> {
                    barCodeProgressBar.visibility = INVISIBLE
                    barCodeTickImageView.setVisibility(INVISIBLE)
                    barCodeXImageView.setVisibility(VISIBLE)
                }

                else -> {
                    kwhProgressBar.visibility = INVISIBLE
                    kwhTickImageView.setVisibility(INVISIBLE)
                    kwhXImageView.setVisibility(VISIBLE)
                }
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun scanBarcodes(bitmap: Bitmap, expectedBarCode: String) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val options = BarcodeScannerOptions.Builder().enableAllPotentialBarcodes()
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
//                Log.e(TAG, valueType.toString())
                if (expectedBarCode == rawValue) {
                    this.barcode = rawValue
                    //callback to react native
                    this.gyariSzamTalalat = true
                    showTick(2)
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
                    this.gyariSzamTalalat = true
                    showTick(2)
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
//        detector?.close()
    }

    companion object {
        private const val TAG = "Camera"
    }
}
