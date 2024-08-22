package com.morvaibarnaerp.OCRWrapperTest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.round

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = listOf<BoundingBox>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()
    private var rectColor = Paint()

    private var bounds = Rect()

    init {
        initPaints()
    }

    fun clear() {
        results = listOf()
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        rectColor.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE

        rectColor.color = ContextCompat.getColor(context!!, R.color.red)
        rectColor.strokeWidth = 8F
        rectColor.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

//        canvas.let {
//            val centerX = width / 2
//            val centerY = height / 2
//            val rectWidth = width * 0.8f
//            val rectHeight = height * 0.8f
//
//            val rect = Rect(
//                (centerX - rectWidth / 2).toInt(),
//                (centerY - rectHeight / 2).toInt(),
//                (centerX + rectWidth / 2).toInt(),
//                (centerY + rectHeight / 2).toInt()
//            )
//
//            it.drawRect(rect, rectColor)
//        }

        // Calculate the maximum rectangle size that fits within the view bounds
//        val viewWidth = width * 0.9f
//        val viewHeight = height.toFloat()
//        val viewAspectRatio = viewWidth / viewHeight
//        val rectAspectRatio = ratioW.toFloat() / ratioH.toFloat()
//
//        val rectWidth: Float
//        val rectHeight: Float
//
//        if (rectAspectRatio > viewAspectRatio) {
//            // Rectangle is wider than the view, match width and adjust height
//            rectWidth = viewWidth
//            rectHeight = viewWidth / rectAspectRatio
//        } else {
//            // Rectangle is taller than the view, match height and adjust width
//            rectHeight = viewHeight
//            rectWidth = viewHeight * rectAspectRatio
//        }
//
//        // Calculate the top-left corner to center the rectangle
//        val left = (viewWidth - rectWidth) / 2
//        val top = (viewHeight - rectHeight) / 2
//
//        // Draw the rectangle
//        canvas.drawRect(left, top, left + rectWidth, top + rectHeight, rectColor)

        results.forEach {
            val left = it.x1 * width
            val top = it.y1 * height
            val right = it.x2 * width
            val bottom = it.y2 * height

            canvas.drawRect(left, top, right, bottom, boxPaint)
            val drawableText = (round(it.cnf * 100)).toString() + "%"

            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)

            canvas.drawText(drawableText, left, top - bounds.height(), textPaint)

        }
    }

    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes
        invalidate()
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}