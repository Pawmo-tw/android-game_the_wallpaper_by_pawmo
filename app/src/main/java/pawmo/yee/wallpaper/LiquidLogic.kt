package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import kotlin.math.*
import kotlin.random.Random

class LiquidLogic : IGameLogic {

    private var canvasWidth = 0
    private var canvasHeight = 0
    private var tiltX = 0f
    private var tiltY = 0f

    // --- 物理參數 ---
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

    // --- 渲染組件 ---
    private val clothPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        style = Paint.Style.FILL
        pathEffect = CornerPathEffect(radius * 3f)
    }

    private val clothPath = Path()
    private var isInitialized = false

    override fun loadResources(resources: Resources) {}

    override fun onSensorChanged(x: Float, y: Float) {
        tiltX = x
        tiltY = y
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

        val ax = -tiltX * gravityMult
        val ay = tiltY * gravityMult

        for (i in 0 until particleCount) {
            vx[i] = (vx[i] + ax) * friction
            vy[i] = (vy[i] + ay) * friction

            for (j in 0 until particleCount) {
                if (i == j) continue

                val dx = particlesX[j] - particlesX[i]
                val dy = particlesY[j] - particlesY[i]
                val distSq = dx * dx + dy * dy
                val minDist = radius * 9f
                val minDistSq = minDist * minDist


                if (distSq < minDistSq) {
                    val dist = sqrt(distSq)
                    val nx = dx / dist
                    val ny = dy / dist
                    val relVx = vx[j] - vx[i]
                    val relVy = vy[j] - vy[i]
                    val velAlongNormal = relVx * nx + relVy * ny

                    if (velAlongNormal < 0) {
                        vx[i] += nx * velAlongNormal * 0.5f
                        vy[i] += ny * velAlongNormal * 0.5f
                    }
                } else if (distSq < minDistSq * 1.5f) {
                    // 吸引力
                    val dist = sqrt(distSq)
                    val pull = attractionForce * (1f - dist / (minDist * 0.8f))
                    vx[i] += (dx / dist) * pull
                    vy[i] += (dy / dist) * pull
                }
            }

            particlesX[i] += vx[i]
            particlesY[i] += vy[i]

            // 邊界
            if (particlesX[i] < radius) { particlesX[i] = radius; vx[i] = 0f }
            else if (particlesX[i] > width - radius) { particlesX[i] = width - radius; vx[i] = 0f }
            if (particlesY[i] < radius) { particlesY[i] = radius; vy[i] = 0f }
            else if (particlesY[i] > height - radius) { particlesY[i] = height - radius; vy[i] = 0f }
        }
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {
        canvas.drawColor(if (isNightMode) Color.BLACK else Color.GRAY)
        clothPath.reset()

        for (i in 0 until particleCount) {
            clothPath.addCircle(particlesX[i], particlesY[i], visualRadius, Path.Direction.CW)
        }

        canvas.drawPath(clothPath, clothPaint)
    }

    override fun release() {}
}