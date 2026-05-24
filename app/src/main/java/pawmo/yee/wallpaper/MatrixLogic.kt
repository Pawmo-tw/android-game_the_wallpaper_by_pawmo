package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import kotlin.random.Random

class MatrixLogic : IGameLogic {
    override var primaryColor: Int = 0xFF34C759.toInt()
    override var secondaryColor: Int = 0xFFFF9500.toInt()
    private val fontSize = 30f
    private val columnGap = 35
    private val characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ"

    private var columns = 0
    private var canvasWidth = 0
    private var canvasHeight = 0

    private lateinit var dropPositions: IntArray
    private lateinit var speeds: IntArray
    private var tiltX = 0f
    private var tiltY = 0f

    private var scanLineY = 0f

    private val geoPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val paint = Paint().apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
        textSize = fontSize
        isAntiAlias = true
    }

    private var backgroundShader: Shader? = null
    private var lastWidth = 0
    private var lastHeight = 0
    private var lastPrimary = 0
    private var lastSecondary = 0

    override fun loadResources(resources: Resources) {}

    override fun onSensorChanged(x: Float, y: Float) {
        tiltX = tiltX * 0.9f + (-x) * 0.1f
        tiltY = tiltY * 0.9f + y * 0.1f
    }

    override fun updatePhysics(width: Int, height: Int) {
        if (canvasWidth != width || canvasHeight != height) {
            canvasWidth = width
            canvasHeight = height

            columns = (width * 3 / columnGap) + 2
            dropPositions = IntArray(columns) { Random.nextInt(height / fontSize.toInt()) }
            speeds = IntArray(columns) { Random.nextInt(2, 5) }
        }

        if (width != lastWidth || height != lastHeight || primaryColor != lastPrimary || secondaryColor != lastSecondary) {
            lastWidth = width
            lastHeight = height
            lastPrimary = primaryColor
            lastSecondary = secondaryColor
            if (width > 0 && height > 0) {
                backgroundShader = LinearGradient(
                    0f, 0f,
                    0f, height.toFloat(),
                    primaryColor, secondaryColor,
                    Shader.TileMode.CLAMP
                )
            }
        }

        for (i in 0 until columns) {
            dropPositions[i] += speeds[i]
            if (dropPositions[i] * fontSize > canvasHeight * 1.5) {
                if (Random.nextFloat() > 0.95f) {
                    dropPositions[i] = -5
                    speeds[i] = Random.nextInt(2, 5)
                }
            }
        }

        scanLineY += 8f
        if (scanLineY > canvasHeight) scanLineY = 0f
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {
        if (canvasWidth <= 0 || canvasHeight <= 0) return

        paint.reset()
        paint.isAntiAlias = true
        paint.shader = backgroundShader
        paint.alpha = 45
        canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), paint)
        paint.shader = null

        canvas.save()
        canvas.skew(tiltX * 0.05f, 0f)
        canvas.translate(-tiltX * (canvasHeight / 20f), 0f)

        drawGeometry(canvas)

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.MONOSPACE
        paint.textSize = fontSize

        for (i in 0 until columns) {
            val x = (i * columnGap).toFloat() - canvasWidth
            val y = dropPositions[i] * fontSize

            for (j in 0 until 5) {
                val trailY = y - (j * fontSize)
                if (trailY < -fontSize || trailY > canvasHeight + fontSize) continue

                val randIdx = (Random.nextInt(characters.length) + j) % characters.length

                if (j == 0) {

                    paint.color = secondaryColor
                    paint.setShadowLayer(14f, 0f, 0f, primaryColor)
                    canvas.drawText(characters, randIdx, randIdx + 1, x, trailY, paint)
                    paint.clearShadowLayer()
                } else {

                    paint.color = primaryColor
                    paint.alpha = ((1.0f - (j / 5f)) * 255).toInt().coerceIn(0, 255)
                    canvas.drawText(characters, randIdx, randIdx + 1, x, trailY, paint)
                }
            }
            paint.alpha = 255

            if (Random.nextFloat() > 0.995f) {
                val randomY = Random.nextInt(canvasHeight).toFloat()
                val randBgIdx = Random.nextInt(characters.length)
                paint.color = secondaryColor
                paint.alpha = 140
                canvas.drawText(characters, randBgIdx, randBgIdx + 1, x, randomY, paint)
                paint.alpha = 255
            }
        }

        canvas.restore()
    }

    private fun drawGeometry(canvas: Canvas) {
        val pR = Color.red(primaryColor)
        val pG = Color.green(primaryColor)
        val pB = Color.blue(primaryColor)

        val sR = Color.red(secondaryColor)
        val sG = Color.green(secondaryColor)
        val sB = Color.blue(secondaryColor)

        geoPaint.color = Color.argb(30, sR, sG, sB)
        geoPaint.strokeWidth = 3f
        canvas.drawLine(-canvasWidth.toFloat(), scanLineY, canvasWidth.toFloat() * 2, scanLineY, geoPaint)

        geoPaint.color = Color.argb(15, pR, pG, pB)
        geoPaint.strokeWidth = 1f
        val gridStep = 200f
        var curX = -canvasWidth.toFloat()
        while (curX < canvasWidth * 2) {
            canvas.drawLine(curX, 0f, curX, canvasHeight.toFloat(), geoPaint)
            curX += gridStep
        }
        var curY = 0f
        while (curY < canvasHeight) {
            canvas.drawLine(-canvasWidth.toFloat(), curY, canvasWidth.toFloat() * 2, curY, geoPaint)
            curY += gridStep
        }

        if (Random.nextFloat() > 0.95f) {
            val rx = Random.nextFloat() * (canvasWidth * 3) - canvasWidth
            val ry = Random.nextFloat() * canvasHeight
            geoPaint.color = Color.argb(40, sR, sG, sB)
            canvas.drawCircle(rx, ry, Random.nextFloat() * 100f + 50f, geoPaint)
        }
    }

    override fun onTouch(event: MotionEvent) {}

    override fun release() {}
}