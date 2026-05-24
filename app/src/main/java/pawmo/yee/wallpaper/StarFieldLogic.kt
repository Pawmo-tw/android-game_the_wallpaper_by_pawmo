package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import kotlin.random.Random

class StarFieldLogic : IGameLogic {
    override var primaryColor: Int = 0xFF34C759.toInt()
    override var secondaryColor: Int = 0xFFFF9500.toInt()
    private var width = 0
    private var height = 0
    private var tiltX = 0f
    private var tiltY = 9.8f

    private data class Star(
        var x: Float,
        var y: Float,
        var speed: Float,
        var size: Float,
        var drift: Float
    )
    private val stars = mutableListOf<Star>()
    private val bgPaint = Paint().apply { isAntiAlias = true }
    private val starPaint = Paint().apply { isAntiAlias = true }
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
        this.width = width
        this.height = height

        if (stars.isEmpty()) {
            repeat(150) {
                stars.add(Star(
                    Random.nextFloat() * (width + 40f) - 20f,
                    Random.nextFloat() * (height + 40f) - 20f,
                    Random.nextFloat() * 6f + 2f,
                    Random.nextFloat() * 3f + 1f,
                    Random.nextFloat() * 0.5f + 0.5f
                ))
            }
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
                    darkenColor(primaryColor, 0.3f),
                    darkenColor(secondaryColor, 0.3f),
                    Shader.TileMode.CLAMP
                )
            }
        }

        stars.forEach { star ->
            star.y += star.speed + (tiltY - 9.8f) * 0.5f
            star.x += tiltX * star.speed * 0.5f * star.drift
            handleBounds(star)
        }
    }

    private fun handleBounds(star: Star) {
        val margin = 20f
        if (star.y > height + margin) {
            star.y = -margin
            star.x = Random.nextFloat() * width
        } else if (star.y < -margin) {
            star.y = height + margin
            star.x = Random.nextFloat() * width
        }

        if (star.x > width + margin) {
            star.x = -margin
        } else if (star.x < -margin) {
            star.x = width + margin
        }
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {
        if (width <= 0 || height <= 0) return

        bgPaint.shader = backgroundShader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        stars.forEach { star ->
            val starAlpha = (star.speed * 35).toInt().coerceIn(80, 255)

            val speedFactor = ((star.speed - 2f) / 6f).coerceIn(0f, 1f)

            val r = (Color.red(primaryColor) * (1f - speedFactor) + Color.red(secondaryColor) * speedFactor).toInt()
            val g = (Color.green(primaryColor) * (1f - speedFactor) + Color.green(secondaryColor) * speedFactor).toInt()
            val b = (Color.blue(primaryColor) * (1f - speedFactor) + Color.blue(secondaryColor) * speedFactor).toInt()

            starPaint.color = Color.rgb(r, g, b)
            starPaint.alpha = starAlpha

            canvas.drawCircle(star.x, star.y, star.size, starPaint)
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

    override fun onTouch(event: MotionEvent) {}

    override fun release() {
        stars.clear()
    }
}