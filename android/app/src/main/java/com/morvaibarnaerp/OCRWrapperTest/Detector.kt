package com.morvaibarnaerp.OCRWrapperTest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.morvaibarnaerp.OCRWrapperTest.Metadata.extractNamesFromMetadata
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.TransformToGrayscaleOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class Detector(
    private val context: Context,
    private val detectModelPath: String,
    private val recogModelPath: String,
    private val labelPath: String?,
    private val detectorListener: DetectorListener,
    private val message: (String) -> Unit,
) {
    private var result: String
    private var resultPercentage: String
    private var recognitionResult: ByteBuffer
    private var recognitionInterpreter: Interpreter
    private var interpreter: Interpreter
    private var labels = mutableListOf<String>()

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0
    private var countOfMeasure = 0
    private val ocrResults = mutableListOf<String>()

    private var success = false
    private var value: String = ""

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    @Throws(IOException::class)
    private fun loadModelFile(context: Context, modelFile: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFile)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val retFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        fileDescriptor.close()
        return retFile
    }

    init {
        val compatList = CompatibilityList()
        val recOptions = Interpreter.Options().apply {
            this.setNumThreads(numberThreads)
        }
        val options = Interpreter.Options().apply {
            if (compatList.isDelegateSupportedOnThisDevice) {
                val delegateOptions = compatList.bestOptionsForThisDevice
                this.addDelegate(GpuDelegate(delegateOptions))
            } else {
                this.setNumThreads(4)
            }
        }
        val detectModel = FileUtil.loadMappedFile(context, detectModelPath)
        val recognitionModel = FileUtil.loadMappedFile(context, recogModelPath)
        interpreter = Interpreter(detectModel, options)
        recognitionInterpreter = Interpreter(recognitionModel, recOptions)
        result = "0"
        resultPercentage = "0 %"

        val inputShape = interpreter.getInputTensor(0)?.shape()
        val outputShape = interpreter.getOutputTensor(0)?.shape()

        recognitionResult = ByteBuffer.allocateDirect(recognitionModelOutputSize * 8)
        recognitionResult.order(ByteOrder.nativeOrder())

        labels.addAll(extractNamesFromMetadata(detectModel))

        if (inputShape != null) {
            tensorWidth = inputShape[1]
            tensorHeight = inputShape[2]

            // If in case input shape is in format of [1, 3, ..., ...]
            if (inputShape[1] == 3) {
                tensorWidth = inputShape[2]
                tensorHeight = inputShape[3]
            }
        }

        if (outputShape != null) {
            numChannel = outputShape[1]
            numElements = outputShape[2]
        }
    }

    fun clear() {
        result = "0"
        resultPercentage = "0 %"
        countOfMeasure = 0
        ocrResults.clear()
    }

    fun restart() {
        interpreter.close()
        recognitionInterpreter.close()

        val compatList = CompatibilityList()
        val options = Interpreter.Options().apply {
            if (compatList.isDelegateSupportedOnThisDevice) {
                val delegateOptions = compatList.bestOptionsForThisDevice
                this.addDelegate(GpuDelegate(delegateOptions))
            } else {
                this.setNumThreads(4)
            }
        }

        val recOptions = Interpreter.Options().apply {
            this.setNumThreads(numberThreads)
        }

        val model = FileUtil.loadMappedFile(context, detectModelPath)
        val recognition_model = FileUtil.loadMappedFile(context, recogModelPath)
        recognitionInterpreter = Interpreter(recognition_model, recOptions)

        interpreter = Interpreter(model, options)
    }

    fun close() {
        interpreter.close()
        recognitionInterpreter.close()
    }

    fun detect(frame: Bitmap) {
        if (tensorWidth == 0
            || tensorHeight == 0
            || numChannel == 0
            || numElements == 0
        ) {
            return
        }

        var inferenceTime = SystemClock.uptimeMillis()

        val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)

        val tensorImage = TensorImage(INPUT_IMAGE_TYPE)
        tensorImage.load(resizedBitmap)
        val processedImage = imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer

        val output =
            TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter.run(imageBuffer, output.buffer)

        val bestBoxes = bestBox(output.floatArray, frame)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        if (bestBoxes == null) {
            detectorListener.onEmptyDetect()
            return
        } else {


            bestBoxes.sortedByDescending { it.cnf }
            if (countOfMeasure <= 15) {
                val recognitionBitmap = createEmptyBitmap(
                    frame,
                    (frame.width * bestBoxes[0].w).toInt(),
                    (frame.height * bestBoxes[0].h).toInt(),
                    (frame.width * bestBoxes[0].x1).toInt(),
                    (frame.height * bestBoxes[0].y1).toInt(),
                )

                var recognizedText = ""
                if (bestBoxes[0].clsName == "segment") {
                    val recognitionTensorImage = bitmapToTensorImageForRecognition(
                        recognitionBitmap,
                        recognitionImageWidth,
                        recognitionImageHeight,
                        recognitionImageMean,
                        recognitionImageStd
                    )
                    recognitionResult.rewind()
                    recognitionInterpreter.run(recognitionTensorImage.buffer, recognitionResult)

                    for (k in 0 until recognitionModelOutputSize) {
                        val alphabetIndex = recognitionResult.getInt(k * 8)
                        if (alphabetIndex in alphabets.indices) {
                            recognizedText += alphabets[alphabetIndex]
                        }
                    }
                    if (recognizedText.isNotEmpty()) {
                        ocrResults.add(recognizedText)
                        countOfMeasure++
                    }
                } else {
                    Log.e("countOfMeasure", countOfMeasure.toString())

                    runOCR(recognitionBitmap) { recognizedText ->
                        if (recognizedText != "") {
                            Log.e("allas", recognizedText)
                            countOfMeasure++
                            ocrResults.add(recognizedText)
                        }
                    }
                }
            } else {
                if (mostFrequentStringWithPercentage(ocrResults) != null) {
                    success = true
                    value = mostFrequentStringWithPercentage(ocrResults)?.first.toString()
                    result = "$value kWh"
                    resultPercentage =
                        mostFrequentStringWithPercentage(ocrResults)?.second.toString() + "%"
//                        Log.e("kwh",value)
                } else {
                    success = false
                    result = "Nincs találat"
                    resultPercentage = ""
                }
            }
        }
        detectorListener.onDetect(bestBoxes, inferenceTime, result, resultPercentage)
    }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height, matrix, true
        )
    }

    @SuppressLint("SuspiciousIndentation")
    private fun bestBox(array: FloatArray, frame: Bitmap): List<BoundingBox>? {
        val boundingBoxes = mutableListOf<BoundingBox>()

        for (c in 0 until numElements) {
            var maxConf = CONFIDENCE_THRESHOLD
            var maxIdx = -1
            var j = 4
            var arrayIdx = c + numElements * j
            while (j < numChannel) {
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += numElements
            }

            if (maxConf > CONFIDENCE_THRESHOLD) {
                val clsName = labels[maxIdx]
                val cx = array[c] // 0
                val cy = array[c + numElements] // 1
                val w = array[c + numElements * 2]
                val h = array[c + numElements * 3]
                val x1 = cx - (w / 2F)
                val y1 = cy - (h / 2F)
                val x2 = cx + (w / 2F)
                val y2 = cy + (h / 2F)
                if (x1 < 0F || x1 > 1F) continue
                if (y1 < 0F || y1 > 1F) continue
                if (x2 < 0F || x2 > 1F) continue
                if (y2 < 0F || y2 > 1F) continue

                boundingBoxes.add(
                    BoundingBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                        cx = cx, cy = cy, w = w, h = h,
                        cnf = maxConf, cls = maxIdx, clsName = clsName
                    )
                )
            }

        }

        if (boundingBoxes.isEmpty()) return null
        return applyNMS(boundingBoxes)
    }

    private fun runOCR(bitmap: Bitmap, callback: (String) -> Unit) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val resizedBitmap = resize(bitmap, 1000, 2000)
//        inverseBitmapColors(bitmap)
        val image = InputImage.fromBitmap(resizedBitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                var resultText = ""

                for (block in visionText.textBlocks) {
                    var blockText = block.text

                    if (blockText.contains(" ")) {
                        blockText = blockText.replace(" ", "")
                    }
                    if (blockText.contains("O")) {
                        blockText = blockText.replace("O", "0")
                    }
                    if (blockText.contains("o")) {
                        blockText = blockText.replace("o", "0")
                    }
                    if (blockText.contains("D")) {
                        blockText = blockText.replace("D", "0")
                    }
                    if (blockText.contains("I")) {
                        blockText = blockText.replace("I", "1")
                    }
                    if (blockText.contains("i")) {
                        blockText = blockText.replace("i", "1")
                    }
                    if (blockText.contains("S")) {
                        blockText = blockText.replace("S", "5")
                    }
                    if (blockText.contains("s")) {
                        blockText = blockText.replace("s", "5")
                    }
                    if (blockText.contains("Z")) {
                        blockText = blockText.replace("Z", "7")
                    }
                    if (blockText.contains("z")) {
                        blockText = blockText.replace("z", "7")
                    }
                    if (blockText.contains("B")) {
                        blockText = blockText.replace("B", "8")
                    }
                    if (blockText.contains("G")) {
                        blockText = blockText.replace("G", "6")
                    }
                    if (blockText.contains("g")) {
                        blockText = blockText.replace("g", "9")
                    }
                    if (blockText.contains("A")) {
                        blockText = blockText.replace("A", "4")
                    }
                    if (blockText.contains("E")) {
                        blockText = blockText.replace("E", "3")
                    }

                    resultText += blockText
                    Log.e("allas", resultText)
                }
                callback(resultText)
            }
            .addOnFailureListener { e ->
                callback("")  // Return empty text in case of failure
            }
    }

    fun resize(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val aspectRatio: Float = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxWidth
            newHeight = (newWidth / aspectRatio).toInt()
        } else {
            newHeight = maxHeight
            newWidth = (newHeight * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)
    }


    private fun inverseBitmapColors(bitmap: Bitmap) {
        for (i in 0 until bitmap.width) {
            for (j in 0 until bitmap.height) {
                bitmap.setPixel(i, j, bitmap.getPixel(i, j) xor 0x00ffffff)
            }
        }
    }

    fun getResult(): String {
        return value
    }

    fun getSuccess(): Boolean {
        return success
    }

    fun setSuccess(success: Boolean) {
        this.success = success
    }

    private fun mostFrequentStringWithPercentage(strings: MutableList<String>): Pair<String, Double>? {
        if (strings.isEmpty()) return null

        // Step 1: Count occurrences of each string
        val stringCounts = strings.groupingBy { it }.eachCount()

        // Step 2: Find the string with the maximum count
        val mostFrequentString = stringCounts.maxByOrNull { it.value }?.key ?: return null

        // Step 3: Calculate the percentage
        val totalStrings = strings.size
        val mostFrequentCount = stringCounts[mostFrequentString] ?: 0
        val percentage = (mostFrequentCount.toDouble() / totalStrings) * 100

        return Pair(mostFrequentString, percentage)
    }

    private fun createEmptyBitmap(
        frame: Bitmap,
        imageWidth: Int,
        imageHeigth: Int,
        x: Int,
        y: Int,
    ): Bitmap {
        val ret = Bitmap.createBitmap(frame, x, y, imageWidth, imageHeigth)
        return ret
    }

    private fun bitmapToTensorImageForRecognition(
        bitmapIn: Bitmap,
        width: Int,
        height: Int,
        mean: Float,
        std: Float
    ): TensorImage {
        val imageProcessor =
            ImageProcessor.Builder()
                .add(ResizeOp(height, width, ResizeOp.ResizeMethod.BILINEAR))
                .add(TransformToGrayscaleOp())
                .add(NormalizeOp(mean, std))
                .build()
        var tensorImage = TensorImage(DataType.FLOAT32)

        tensorImage.load(bitmapIn)
        tensorImage = imageProcessor.process(tensorImage)

        return tensorImage
    }

    private fun applyNMS(boxes: List<BoundingBox>): MutableList<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while (sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }

    interface DetectorListener {
        fun onEmptyDetect()
        fun onDetect(
            boundingBoxes: List<BoundingBox>,
            inferenceTime: Long,
            result: String,
            resultPercentage: String
        )
    }


    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.6f
        private const val IOU_THRESHOLD = 0.5F
        private const val recognitionModelOutputSize = 48
        private const val recognitionImageHeight = 31
        private const val recognitionImageWidth = 200
        private const val recognitionImageMean = 0f
        private const val recognitionImageStd = 255f
        private const val numberThreads = 4
        private const val alphabets = "0123456789."
    }
}