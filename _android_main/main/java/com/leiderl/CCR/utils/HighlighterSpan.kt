package com.leiderl.CCR.utils

import android.content.Context
import android.graphics.*
import android.text.style.LineBackgroundSpan
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.leiderl.CCR.R

class HighlighterSpan(
    private val context: Context,
    private val color: Int
) : LineBackgroundSpan {

    // Paint propio — nunca tocamos el Paint del sistema
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var cachedShader: BitmapShader? = null
    private var cachedLineHeight: Float = 0f

    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lineNumber: Int
    ) {
        val padV = 6f
        val padH = 9f
        val radius = 6f

        val lineHeight = (bottom - top).toFloat()

        if (cachedShader == null || cachedLineHeight != lineHeight) {
            cachedLineHeight = lineHeight
            cachedShader = buildShader(lineHeight + padV * 2)
        }

        val lineText = text.subSequence(start, end).trimEnd()
        val textWidth = paint.measureText(lineText.toString())

        val rect = RectF(
            left - padH,
            top + padV,
            left + textWidth + padH,
            bottom - padV
        )

        val shader = cachedShader
        if (shader != null) {
            val matrix = Matrix()
            matrix.setTranslate(left.toFloat(), top + padV)
            shader.setLocalMatrix(matrix)
            bgPaint.shader = shader
            bgPaint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
        } else {
            bgPaint.shader = null
            bgPaint.colorFilter = null
            bgPaint.color = color
        }

        canvas.drawRoundRect(rect, radius, radius, bgPaint)
    }

    private fun buildShader(height: Float): BitmapShader? {
        return try {
            val drawable = ContextCompat.getDrawable(context, R.drawable.highlighter_texture)
                ?: return null
            val bmp = drawable.toBitmap(
                width = 800,
                height = height.toInt().coerceAtLeast(1)
            )
            BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP)
        } catch (e: Exception) {
            null
        }
    }
}