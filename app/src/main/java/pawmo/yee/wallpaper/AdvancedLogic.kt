package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import kotlin.math.*
import kotlin.random.Random

class AdvancedLogic : IGameLogic {
    override var primaryColor: Int = 0xFF34C759.toInt()
    override var secondaryColor: Int = 0xFFFF9500.toInt()

    private var canvasWidth = 0
    private var canvasHeight = 0
    private var tiltX = 0f
    private var tiltY = 0f
    private var coreX = 0f
    private var coreY = 0f
    private var isTouching = false
    private var corePulse = 0f
    private val particleCount = 200
    private val px = FloatArray(particleCount)
    private val py = FloatArray(particleCount)
    private val pvx = FloatArray(particleCount)
    private val pvy = FloatArray(particleCount)
    private val pSize = FloatArray(particleCount)
    private val pSpeedFactor = FloatArray(particleCount)
    private val bgPaint = Paint().apply { isAntiAlias = true }
    private val particlePaint = Paint().apply { isAntiAlias = true }
    private val gridPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private var backgroundShader: Shader? = null
    private var lastWidth = 0
    private var lastHeight = 0
    private var lastSecondary = 0
    private var isInitialized = false

    override fun loadResources(resources: Resources) {
    }

    override fun onSensorChanged(x: Float, y: Float) {

        tiltX = tiltX * 0.92f + (-x) * 0.08f
        tiltY = tiltY * 0.92f + y * 0.08f
    }

    override fun updatePhysics(width: Int, height: Int) {
        if (!isInitialized || canvasWidth != width) {
            canvasWidth = width
            canvasHeight = height
            coreX = width / 2f
            coreY = height / 2f

            val rand = Random(System.currentTimeMillis())
            for (i in 0 until particleCount) {
                px[i] = rand.nextFloat() * width
                py[i] = rand.nextFloat() * height
                pvx[i] = rand.nextFloat() * 4f - 2f
                pvy[i] = rand.nextFloat() * 4f - 2f
                pSize[i] = rand.nextFloat() * 4f + 2f
                pSpeedFactor[i] = rand.nextFloat() * 0.4f + 0.8f
            }
            isInitialized = true
        }

        if (width != lastWidth || height != lastHeight || secondaryColor != lastSecondary) {
            lastWidth = width
            lastHeight = height
            lastSecondary = secondaryColor
            if (width > 0 && height > 0) {
                backgroundShader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    darkenColor(secondaryColor, 0.08f), // 極黑輔色
                    darkenColor(secondaryColor, 0.22f), // 暗輔色
                    Shader.TileMode.CLAMP
                )
            }
        }

        corePulse += 0.07f
        if (corePulse > Math.PI * 2) corePulse = 0f

        if (!isTouching) {
            val targetCenterX = (width / 2f) + tiltX * 30f
            val targetCenterY = (height / 2f) + tiltY * 30f
            coreX += (targetCenterX - coreX) * 0.1f
            coreY += (targetCenterY - coreY) * 0.1f
        }

        for (i in 0 until particleCount) {
            val dx = coreX - px[i]
            val dy = coreY - py[i]
            val distSq = dx * dx + dy * dy
            val dist = sqrt(distSq).coerceAtLeast(10f)

            val gravityPull = if (isTouching) 8.5f else 2.0f

            val force = (gravityPull * pSpeedFactor[i]) / (1f + distSq * 0.00002f)

            val rotateX = -dy / dist
            val rotateY = dx / dist
            val rotationForce = if (isTouching) 3.0f else 0.5f

            pvx[i] += (dx / dist) * force + rotateX * rotationForce + tiltX * 0.05f
            pvy[i] += (dy / dist) * force + rotateY * rotationForce + tiltY * 0.05f

            pvx[i] *= 0.96f
            pvy[i] *= 0.96f

            px[i] += pvx[i]
            py[i] += pvy[i]

            if (px[i] < -20f) { px[i] = width + 20f; py[i] = Random.nextFloat() * height }
            else if (px[i] > width + 20f) { px[i] = -20f; py[i] = Random.nextFloat() * height }

            if (py[i] < -20f) { py[i] = height + 20f; px[i] = Random.nextFloat() * width }
            else if (py[i] > height + 20f) { py[i] = -20f; px[i] = Random.nextFloat() * width }
        }
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {
        if (canvasWidth <= 0 || canvasHeight <= 0) return


        bgPaint.shader = backgroundShader
        canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), bgPaint)

        drawSpaceGrid(canvas)

        val coreRadius = (if (isTouching) 25f else 12f) + sin(corePulse).toFloat() * 4f
        particlePaint.color = primaryColor
        particlePaint.alpha = if (isTouching) 255 else 130
        canvas.drawCircle(coreX, coreY, coreRadius, particlePaint)

        val pR = Color.red(primaryColor)
        val pG = Color.green(primaryColor)
        val pB = Color.blue(primaryColor)

        for (i in 0 until particleCount) {

            val speed = sqrt(pvx[i] * pvx[i] + pvy[i] * pvy[i])
            val alpha = (speed * 20 + 80).toInt().coerceIn(80, 255)

            particlePaint.color = Color.argb(alpha, pR, pG, pB)
            canvas.drawCircle(px[i], py[i], pSize[i], particlePaint)
        }
    }

    private fun drawSpaceGrid(canvas: Canvas) {
        val sR = Color.red(secondaryColor)
        val sG = Color.green(secondaryColor)
        val sB = Color.blue(secondaryColor)

        gridPaint.color = Color.argb(22, sR, sG, sB)
        gridPaint.strokeWidth = 2f

        val step = 150f

        var y = 0f
        while (y < canvasHeight) {
            canvas.drawLine(0f, y + tiltY * 5f, canvasWidth.toFloat(), y - tiltY * 5f, gridPaint)
            y += step
        }

        var x = 0f
        while (x < canvasWidth) {
            canvas.drawLine(x + tiltX * 5f, 0f, x - tiltX * 5f, canvasHeight.toFloat(), gridPaint)
            x += step
        }
    }

    override fun onTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                coreX = event.x
                coreY = event.y
                isTouching = true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouching = false

                val rand = Random(System.currentTimeMillis())
                for (i in 0 until particleCount) {
                    val angle = rand.nextFloat() * Math.PI * 2
                    val speed = rand.nextFloat() * 15f + 5f
                    pvx[i] = cos(angle).toFloat() * speed
                    pvy[i] = sin(angle).toFloat() * speed
                }
            }
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

    override fun release() {
        isInitialized = false
    }
}