package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import kotlin.math.*

class LineLogic : IGameLogic {
    override var primaryColor: Int = 0xFF34C759.toInt()
    override var secondaryColor: Int = 0xFFFF9500.toInt()
    private var canvasWidth = 0
    private var canvasHeight = 0

    private val segmentCount = 20
    private val nodesX = FloatArray(segmentCount)
    private val nodesY = FloatArray(segmentCount)
    private var targetX = 0f
    private var targetY = 0f

    private val stiffness = 0.5f
    private val friction = 0.5f
    private val velocitiesX = FloatArray(segmentCount)
    private val velocitiesY = FloatArray(segmentCount)

    private val linePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val glowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        maskFilter = BlurMaskFilter(25f, BlurMaskFilter.Blur.NORMAL)
    }

    private val bgPaint = Paint().apply { isAntiAlias = true }
    private val linePath = Path()
    private var isInitialized = false

    private var backgroundShader: Shader? = null
    private var lastWidth = 0
    private var lastHeight = 0
    private var lastSecondary = 0

    override fun loadResources(resources: Resources) {}

    fun onPointerInput(x: Float, y: Float) {
        targetX = x
        targetY = y
    }

    override fun onSensorChanged(x: Float, y: Float) {}

    override fun updatePhysics(width: Int, height: Int) {
        if (!isInitialized || canvasWidth != width) {
            canvasWidth = width
            canvasHeight = height
            for (i in 0 until segmentCount) {
                nodesX[i] = width / 2f
                nodesY[i] = height / 2f
            }
            targetX = width / 2f
            targetY = height / 2f
            isInitialized = true
        }

        if (width != lastWidth || height != lastHeight || secondaryColor != lastSecondary) {
            lastWidth = width
            lastHeight = height
            lastSecondary = secondaryColor
            if (width > 0 && height > 0) {
                backgroundShader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    darkenColor(secondaryColor, 0.12f),
                    darkenColor(secondaryColor, 0.35f),
                    Shader.TileMode.CLAMP
                )
            }
        }

        nodesX[0] += (targetX - nodesX[0]) * 0.3f
        nodesY[0] += (targetY - nodesY[0]) * 0.3f

        for (i in 1 until segmentCount) {
            val dx = (nodesX[i - 1] - nodesX[i])
            val dy = (nodesY[i - 1] - nodesY[i])

            velocitiesX[i] = (velocitiesX[i] + dx * stiffness) * friction
            velocitiesY[i] = (velocitiesY[i] + dy * stiffness) * friction

            nodesX[i] += velocitiesX[i]
            nodesY[i] += velocitiesY[i]

            if (abs(velocitiesX[i]) < 0.05f) velocitiesX[i] = 0f
            if (abs(velocitiesY[i]) < 0.05f) velocitiesY[i] = 0f
        }
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {
        if (canvasWidth <= 0 || canvasHeight <= 0) return

        bgPaint.shader = backgroundShader
        canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), bgPaint)

        linePath.reset()
        linePath.moveTo(nodesX[0], nodesY[0])
        for (i in 1 until segmentCount - 1) {
            val midX = (nodesX[i] + nodesX[i + 1]) / 2f
            val midY = (nodesY[i] + nodesY[i + 1]) / 2f
            linePath.quadTo(nodesX[i], nodesY[i], midX, midY)
        }
        linePath.lineTo(nodesX[segmentCount - 1], nodesY[segmentCount - 1])

        glowPaint.color = primaryColor
        glowPaint.alpha = if (isNightMode) 140 else 90
        glowPaint.strokeWidth = 35f
        canvas.drawPath(linePath, glowPaint)

        linePaint.style = Paint.Style.STROKE
        linePaint.color = primaryColor
        linePaint.alpha = 255
        linePaint.strokeWidth = 8f
        canvas.drawPath(linePath, linePaint)

        linePaint.style = Paint.Style.FILL
        canvas.drawCircle(nodesX[0], nodesY[0], 6f, linePaint)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        return Color.argb(
            255,
            (Color.red(color) * factor).toInt(),
            (Color.green(color) * factor).toInt(),
            (Color.blue(color) * factor).toInt()
        )
    }

    override fun onTouch(event: MotionEvent) {
        targetX = event.x
        targetY = event.y
    }

    override fun release() {
        linePath.reset()
    }
}