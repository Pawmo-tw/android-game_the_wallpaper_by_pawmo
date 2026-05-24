package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import kotlin.math.*
import kotlin.random.Random

class LiquidLogic : IGameLogic {
    override var primaryColor: Int = 0xFF34C759.toInt()
    override var secondaryColor: Int = 0xFFFF9500.toInt()
    private var canvasWidth = 0
    private var canvasHeight = 0

    private var tiltX = 0f
    private var tiltY = 0f

    private val particleCount = 180
    private val radius = 20f // 物理半徑
    private val visualRadius = radius * 18f // 渲染半徑

    private val particlesX = FloatArray(particleCount)
    private val particlesY = FloatArray(particleCount)
    private val vx = FloatArray(particleCount)
    private val vy = FloatArray(particleCount)

    private val friction = 1f
    private val gravityMult = 0.514f
    private val attractionForce = 1f

    private val bgPaint = Paint().apply { isAntiAlias = true }
    private val clothPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        pathEffect = CornerPathEffect(radius * 3f)
    }

    private val clothPath = Path()
    private var isInitialized = false

    private var backgroundShader: Shader? = null

    private var liquidShader: Shader? = null

    private var lastWidth = 0
    private var lastHeight = 0
    private var lastPrimary = 0
    private var lastSecondary = 0

    override fun loadResources(resources: Resources) {}

    override fun onSensorChanged(x: Float, y: Float) {
        tiltX = tiltX * 0.9f + x * 0.1f
        tiltY = tiltY * 0.9f + y * 0.1f
    }

    override fun updatePhysics(width: Int, height: Int) {
        if (!isInitialized || canvasWidth != width) {
            canvasWidth = width
            canvasHeight = height
            val r = Random(System.currentTimeMillis())
            for (i in 0 until particleCount) {
                particlesX[i] = r.nextFloat() * width
                particlesY[i] = r.nextFloat() * height
            }
            isInitialized = true
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
                    darkenColor(primaryColor, 0.5f),
                    darkenColor(secondaryColor, 0.5f),
                    Shader.TileMode.CLAMP
                )

                val mixedColor = mixColors(primaryColor, secondaryColor, 0.999999f)

                liquidShader = LinearGradient(
                    0f, 0f,
                    0f, height.toFloat(),
                    mixedColor,
                    primaryColor,
                    Shader.TileMode.CLAMP
                )
            }
        }

        val ax = -tiltX * gravityMult
        val ay = tiltY * gravityMult

        val minDist = radius * 9f
        val minDistSq = minDist * minDist
        val attractionMaxDistSq = minDistSq * 1.5f

        for (i in 0 until particleCount) {
            vx[i] = (vx[i] + ax) * friction
            vy[i] = (vy[i] + ay) * friction

            for (j in 0 until particleCount) {
                if (i == j) continue

                val dx = particlesX[j] - particlesX[i]
                val dy = particlesY[j] - particlesY[i]
                val distSq = dx * dx + dy * dy

                if (distSq < attractionMaxDistSq) {
                    val dist = sqrt(distSq)
                    if (dist < 0.01f) continue

                    val nx = dx / dist
                    val ny = dy / dist

                    if (distSq < minDistSq) {
                        val relVx = vx[j] - vx[i]
                        val relVy = vy[j] - vy[i]
                        val velAlongNormal = relVx * nx + relVy * ny

                        if (velAlongNormal < 0) {
                            vx[i] += nx * velAlongNormal * 0.5f
                            vy[i] += ny * velAlongNormal * 0.5f
                        }
                    } else {
                        val pull = attractionForce * (1f - dist / (minDist * 0.8f))
                        vx[i] += nx * pull
                        vy[i] += ny * pull
                    }
                }
            }

            particlesX[i] += vx[i]
            particlesY[i] += vy[i]

            if (particlesX[i] < radius) { particlesX[i] = radius; vx[i] = 0f }
            else if (particlesX[i] > width - radius) { particlesX[i] = width - radius; vx[i] = 0f }
            if (particlesY[i] < radius) { particlesY[i] = radius; vy[i] = 0f }
            else if (particlesY[i] > height - radius) { particlesY[i] = height - radius; vy[i] = 0f }
        }
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {
        if (canvasWidth <= 0 || canvasHeight <= 0) return

        bgPaint.shader = backgroundShader
        canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), bgPaint)

        clothPath.reset()
        for (i in 0 until particleCount) {
            clothPath.addCircle(particlesX[i], particlesY[i], visualRadius, Path.Direction.CW)
        }

        clothPaint.shader = liquidShader
        canvas.drawPath(clothPath, clothPaint)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        return Color.argb(
            255,
            (Color.red(color) * factor).toInt(),
            (Color.green(color) * factor).toInt(),
            (Color.blue(color) * factor).toInt()
        )
    }


    private fun mixColors(color1: Int, color2: Int, ratio: Float): Int {
        val r = (Color.red(color1) * (1f - ratio) + Color.red(color2) * ratio).toInt()
        val g = (Color.green(color1) * (1f - ratio) + Color.green(color2) * ratio).toInt()
        val b = (Color.blue(color1) * (1f - ratio) + Color.blue(color2) * ratio).toInt()
        return Color.rgb(r, g, b)
    }

    override fun onTouch(event: MotionEvent) {}

    override fun release() {
        clothPath.reset()
    }
}