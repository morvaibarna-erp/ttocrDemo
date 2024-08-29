package com.morvaibarnaerp.OCRWrapperTest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
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
        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE

    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results.forEach {
            val left = it.x1 * width - it.x1 * 50f
            val top = it.y1 * height - it.y1 * 50f
            val right = it.x2 * width + it.x2 * 50f
            val bottom = it.y2 * height + it.y2 * 50f
            val borderRadius = 30f
            val rect = RectF(left, top, right, bottom)
            canvas.drawRoundRect(rect, borderRadius, borderRadius, boxPaint)
//            val drawableText = (round(it.cnf * 100)).toString() + "%"
//
//            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
//
//            canvas.drawText(drawableText, left, top - bounds.height(), textPaint)

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