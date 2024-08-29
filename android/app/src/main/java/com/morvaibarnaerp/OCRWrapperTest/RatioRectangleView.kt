package com.morvaibarnaerp.OCRWrapperTest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.sqrt

class RatioRectangleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.white)
        strokeWidth = 20f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var widthRatio: Int = 1
        set(value) {
            field = value
            calculateDimensions()
            invalidate()
        }

    private var heightRatio: Int = 1
        set(value) {
            field = value
            calculateDimensions()
            invalidate()
        }

    private val rectF = RectF()
    private var rectWidth: Float = 0f
    private var rectHeight: Float = 0f
    private var left: Float = 0f
    private var top: Float = 0f
    private val cornerLength = 150f // Length of the corner segments
    private val borderRadius = 30f // Radius for rounded corners

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateDimensions()
    }

    private fun calculateDimensions() {
        val displayMetrics = context.resources.displayMetrics
        val yinch = displayMetrics.heightPixels / displayMetrics.ydpi
        val xinch = displayMetrics.widthPixels / displayMetrics.xdpi

        val diagonalinch = sqrt((xinch * xinch + yinch * yinch).toDouble())
        if (diagonalinch >= 7) {
            val viewWidth = width.toFloat()
            rectWidth = (viewWidth * 0.5f)
            Log.e("width", viewWidth.toString())
            rectHeight = rectWidth * (heightRatio.toFloat() / widthRatio.toFloat())
            left = (viewWidth - rectWidth) / 2
            top = (height.toFloat() - rectHeight) / 2
            rectF.set(left, top, left + rectWidth, top + rectHeight)
        } else {
            val viewWidth = width.toFloat()
            rectWidth = (viewWidth * 0.8f)
            Log.e("width", viewWidth.toString())
            rectHeight = rectWidth * (heightRatio.toFloat() / widthRatio.toFloat())
            left = (viewWidth - rectWidth) / 2
            top = (height.toFloat() - rectHeight) / 2
            rectF.set(left, top, left + rectWidth, top + rectHeight)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw rounded corners
        drawRoundedCorner(canvas, left, top) // Top-left
        drawRoundedCorner(canvas, left + rectWidth, top, isTopRight = true) // Top-right
        drawRoundedCorner(canvas, left, top + rectHeight, isBottomLeft = true) // Bottom-left
        drawRoundedCorner(canvas, left + rectWidth, top + rectHeight, isBottomRight = true) // Bottom-right
    }

    private fun drawRoundedCorner(
        canvas: Canvas, cx: Float, cy: Float,
        isTopRight: Boolean = false, isBottomLeft: Boolean = false, isBottomRight: Boolean = false
    ) {
        // Top-left and Bottom-right corners
        if (!isTopRight && !isBottomLeft && !isBottomRight) {
            canvas.drawArc(
                RectF(cx, cy, cx + 2 * borderRadius, cy + 2 * borderRadius),
                180f, 90f, false, paint
            )
            canvas.drawLine(cx + borderRadius, cy, cx + borderRadius + cornerLength, cy, paint) // horizontal
            canvas.drawLine(cx, cy + borderRadius, cx, cy + borderRadius + cornerLength, paint) // vertical
        }

        // Top-right corner
        if (isTopRight) {
            canvas.drawArc(
                RectF(cx - 2 * borderRadius, cy, cx, cy + 2 * borderRadius),
                270f, 90f, false, paint
            )
            canvas.drawLine(cx - borderRadius, cy, cx - borderRadius - cornerLength, cy, paint) // horizontal
            canvas.drawLine(cx, cy + borderRadius, cx, cy + borderRadius + cornerLength, paint) // vertical
        }

        // Bottom-left corner
        if (isBottomLeft) {
            canvas.drawArc(
                RectF(cx, cy - 2 * borderRadius, cx + 2 * borderRadius, cy),
                90f, 90f, false, paint
            )
            canvas.drawLine(cx + borderRadius, cy, cx + borderRadius + cornerLength, cy, paint) // horizontal
            canvas.drawLine(cx, cy - borderRadius, cx, cy - borderRadius - cornerLength, paint) // vertical
        }

        // Bottom-right corner
        if (isBottomRight) {
            canvas.drawArc(
                RectF(cx - 2 * borderRadius, cy - 2 * borderRadius, cx, cy),
                0f, 90f, false, paint
            )
            canvas.drawLine(cx - borderRadius, cy, cx - borderRadius - cornerLength, cy, paint) // horizontal
            canvas.drawLine(cx, cy - borderRadius, cx, cy - borderRadius - cornerLength, paint) // vertical
        }
    }

    // Getter function to retrieve the rectangle as a Rect object
    fun getRectangle(): Rect {

        val rect = Rect()
        val targetWidth = 720
        val targetHeight = 1280

        val rectWidth = targetWidth * 0.8f

        // Calculate the rectangle height based on the aspect ratio
        val rectHeight = rectWidth * (heightRatio.toFloat() / widthRatio.toFloat())

        // Calculate the top-left corner to center the rectangle within the 1280x720 target
        val left = (targetWidth - rectWidth) / 2
        val top = (targetHeight - rectHeight) / 2

        rect.set(
            (left).toInt(),//72->100
            (top).toInt(),//352->387
            (rectWidth).toInt(),//576->523
            (rectHeight).toInt()
        )

        return rect
    }

    fun setHRatio(value: Int) {
        this.heightRatio = value
    }

    fun setWRatio(value: Int) {
        this.widthRatio = value
    }
}
