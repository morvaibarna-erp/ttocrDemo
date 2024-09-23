package com.morvaibarnaerp.OCRWrapperTest

object Constants {
    const val DETECT_MODEL = "two_label_v3_float16.tflite"
    const val RECOGNITION_MODEL = "recognition_v2.tflite"
    val LABELS_PATH: String? = null // provide your labels.txt file if the metadata not present in the model
}