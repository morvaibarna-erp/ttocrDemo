package com.morvaibarnaerp.OCRWrapperTest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class RatioRectangleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.red)
        strokeWidth = 8F
        style = Paint.Style.STROKE
    }

    private var widthRatio: Int = 3
    private var heightRatio: Int = 4

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Calculate the rectangle width as 80% of the view width
        val viewWidth = width.toFloat()
        val rectWidth = viewWidth * 0.8f

        // Calculate the rectangle height based on the width and the aspect ratio
        val rectHeight = rectWidth * (heightRatio.toFloat() / widthRatio.toFloat())

        // Calculate the top-left corner to center the rectangle vertically and horizontally
        val left = (viewWidth - rectWidth) / 2
        val top = (height.toFloat() - rectHeight) / 2

        // Draw the rectangle
        canvas.drawRect(left, top, left + rectWidth, top + rectHeight, paint)
    }

    fun setHeightRatio(value: Int) {
        this.heightRatio = value
    }
    fun setWidthRatio(value: Int) {
        this.widthRatio = value
    }
}
