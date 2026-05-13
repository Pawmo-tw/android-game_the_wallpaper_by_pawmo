package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent

class RippleLogic : IGameLogic {
    private var width = 0
    private var height = 0
    private val res = 6
    private var rows = 0
    private var cols = 0

    private lateinit var buffer1: FloatArray
    private lateinit var buffer2: FloatArray

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

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

        // Wave Propagation
        for (y in 1 until rows - 1) {
            for (x in 1 until cols - 1) {
                val idx = y * cols + x

                val valNearby = (buffer1[idx - 1] + buffer1[idx + 1] +
                        buffer1[idx - cols] + buffer1[idx + cols]) / 2f

                var newVal = valNearby - buffer2[idx]
                // 阻尼係數
                newVal *= 0.98f

                buffer2[idx] = newVal
            }
        }

        val temp = buffer1
        buffer1 = buffer2
        buffer2 = temp
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {
        val bgColor = if (isNightMode) Color.BLACK else Color.WHITE
        val waveBaseColor = if (isNightMode) 255 else 0

        canvas.drawColor(bgColor)


        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val valHeight = buffer1[y * cols + x]
                if (Math.abs(valHeight) > 0.5f) {
                    val alpha = (Math.abs(valHeight) * 10).toInt().coerceIn(0, 200)
                    paint.color = Color.argb(alpha, waveBaseColor, waveBaseColor, waveBaseColor)

                    val left = (x * res).toFloat()
                    val top = (y * res).toFloat()

                    canvas.drawRect(left, top, left + res, top + res, paint)
                }
            }
        }
    }

    override fun onTouch(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val centerX = (event.x / res).toInt().coerceIn(1, cols - 2)
            val centerY = (event.y / res).toInt().coerceIn(1, rows - 2)
            buffer1[centerY * cols + centerX] = 255f
            buffer1[(centerY+1) * cols + centerX] = 120f
            buffer1[(centerY-1) * cols + centerX] = 120f
        }
    }

    override fun release() {
    }
}