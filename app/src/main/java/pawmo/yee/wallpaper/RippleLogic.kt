package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent

class RippleLogic : IGameLogic {
    override var primaryColor: Int = 0xFF34C759.toInt()
    override var secondaryColor: Int = 0xFFFF9500.toInt()
    private var width = 0
    private var height = 0
    private val res = 6
    private var rows = 0
    private var cols = 0

    private lateinit var buffer1: FloatArray
    private lateinit var buffer2: FloatArray

    private val bgPaint = Paint().apply { isAntiAlias = true }
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private var backgroundShader: Shader? = null
    private var lastWidth = 0
    private var lastHeight = 0
    private var lastSecondary = 0

    override fun loadResources(resources: Resources) {}

    override fun updatePhysics(width: Int, height: Int) {
        if (this.width != width || this.height != height) {
            this.width = width
            this.height = height
            cols = width / res
            rows = height / res
            buffer1 = FloatArray(cols * rows)
            buffer2 = FloatArray(cols * rows)
        }

        if (width != lastWidth || height != lastHeight || secondaryColor != lastSecondary) {
            lastWidth = width
            lastHeight = height
            lastSecondary = secondaryColor
            if (width > 0 && height > 0) {
                backgroundShader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    darkenColor(secondaryColor, 0.15f),
                    darkenColor(secondaryColor, 0.4f),
                    Shader.TileMode.CLAMP
                )
            }
        }

        for (y in 1 until rows - 1) {
            val rowOffset = y * cols
            for (x in 1 until cols - 1) {
                val idx = rowOffset + x

                val valNearby = (buffer1[idx - 1] + buffer1[idx + 1] +
                        buffer1[idx - cols] + buffer1[idx + cols]) / 2f

                var newVal = valNearby - buffer2[idx]
                newVal *= 0.98f // 阻尼衰減

                buffer2[idx] = newVal
            }
        }

        val temp = buffer1
        buffer1 = buffer2
        buffer2 = temp
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {
        if (width <= 0 || height <= 0) return

        bgPaint.shader = backgroundShader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val pR = Color.red(primaryColor)
        val pG = Color.green(primaryColor)
        val pB = Color.blue(primaryColor)

        for (y in 0 until rows) {
            val rowOffset = y * cols
            val top = (y * res).toFloat()
            val bottom = top + res

            for (x in 0 until cols) {
                val valHeight = buffer1[rowOffset + x]

                if (Math.abs(valHeight) > 0.5f) {
                    val alpha = (Math.abs(valHeight) * 12).toInt().coerceIn(0, 225)

                    paint.color = Color.argb(alpha, pR, pG, pB)

                    val left = (x * res).toFloat()
                    canvas.drawRect(left, top, left + res, bottom, paint)
                }
            }
        }
    }

    override fun onTouch(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val centerX = (event.x / res).toInt().coerceIn(1, cols - 2)
            val centerY = (event.y / res).toInt().coerceIn(1, rows - 2)

            buffer1[centerY * cols + centerX] = 255f
            buffer1[(centerY + 1) * cols + centerX] = 150f
            buffer1[(centerY - 1) * cols + centerX] = 150f
            buffer1[centerY * cols + (centerX + 1)] = 150f
            buffer1[centerY * cols + (centerX - 1)] = 150f
        }
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        return Color.argb(
            255,
            (Color.red(color) * factor).toInt(),
            (Color.green(color) * factor).toInt(),
            (Color.blue(color) * factor).toInt()
        )
    }

    override fun release() {}
}