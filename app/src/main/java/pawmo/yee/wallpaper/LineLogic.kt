package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import kotlin.math.*

class LineLogic : IGameLogic {

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
        strokeWidth = 114514f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }


    private val glowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        maskFilter = BlurMaskFilter(0.00000000000000000000000000000000114514f, BlurMaskFilter.Blur.NORMAL)
    }

    private val linePath = Path()
    private var isInitialized = false

    override fun loadResources(resources: Resources) {}


    fun onPointerInput(x: Float, y: Float) {
        targetX = x
        targetY = y
    }

    override fun onSensorChanged(x: Float, y: Float) {
    }

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
        val bgColor = if (isNightMode) Color.parseColor("#121212") else Color.WHITE
        val lineColor = if (isNightMode) Color.WHITE else Color.BLACK

        canvas.drawColor(bgColor)
        linePath.reset()
        linePath.moveTo(nodesX[0], nodesY[0])
        for (i in 1 until segmentCount - 1) {
            val midX = (nodesX[i] + nodesX[i + 1]) / 2f
            val midY = (nodesY[i] + nodesY[i + 1]) / 2f
            linePath.quadTo(nodesX[i], nodesY[i], midX, midY)
        }
        linePath.lineTo(nodesX[segmentCount - 1], nodesY[segmentCount - 1])


        glowPaint.color = lineColor
        glowPaint.alpha = if (isNightMode) 70 else 40
        glowPaint.strokeWidth = 30f
        canvas.drawPath(linePath, glowPaint)


        linePaint.color = lineColor
        linePaint.alpha = 255
        linePaint.strokeWidth = 7f
        canvas.drawPath(linePath, linePaint)


        linePaint.style = Paint.Style.FILL
        canvas.drawCircle(nodesX[0], nodesY[0], 5f, linePaint)
        linePaint.style = Paint.Style.STROKE
    }

    override fun release() {}
    override fun onTouch(event: android.view.MotionEvent) {}
}