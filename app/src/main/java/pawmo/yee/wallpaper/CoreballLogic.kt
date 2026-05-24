package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import kotlin.math.*

class CoreballLogic : IGameLogic {

    override var primaryColor: Int = 0xFF34C759.toInt()
    override var secondaryColor: Int = 0xFFFF9500.toInt()

    private var score = 0
    private var isGameOver = false
    private var rotationAngle = 0f
    private val baseRotationSpeed = 2.5f

    private var isLaunching = false
    private var launchY = 0f
    private var flashAlpha = 0
    private var coreScale = 1.0f

    private var screenWidth = 0f
    private var screenHeight = 0f
    private var centerX = 0f
    private var centerY = 0f
    private val coreRadius = 100f
    private val pinLineLength = 280f
    private val pinHeadRadius = 25f

    private val pinnedAngles = mutableListOf<Float>()

    override fun loadResources(resources: Resources) {}

    override fun updatePhysics(width: Int, height: Int) {
        screenWidth = width.toFloat()
        screenHeight = height.toFloat()
        centerX = screenWidth / 2f
        centerY = screenHeight * 0.4f

        if (!isGameOver) {
            val currentSpeed = (baseRotationSpeed + (score / 12f)).coerceAtMost(7.0f)
            rotationAngle = (rotationAngle + currentSpeed) % 360f

            if (isLaunching) {
                launchY -= 150f
                if (launchY <= centerY + coreRadius + pinLineLength) {
                    checkCollisionAndPin()
                }
            }

            if (coreScale > 1.0f) coreScale -= 0.05f
        }

        if (flashAlpha > 0) flashAlpha -= 10
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {
        if (screenWidth <= 0f || screenHeight <= 0f) return

        val bgPaint = Paint().apply { isAntiAlias = true }
        val bgStart = if (isNightMode) darkenColor(secondaryColor, 0.15f) else lightenColor(secondaryColor, 0.85f)
        val bgEnd = if (isNightMode) darkenColor(primaryColor, 0.1f) else lightenColor(primaryColor, 0.9f)

        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, screenHeight,
            bgStart, bgEnd,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, bgPaint)

        if (flashAlpha > 0) {
            canvas.drawColor(Color.argb(flashAlpha, 255, 255, 255))
        }

        val paint = Paint().apply {
            isAntiAlias = true
            strokeWidth = 6f
        }

        for (angle in pinnedAngles) {
            val currentAngle = (angle + rotationAngle)
            val rad = Math.toRadians(currentAngle.toDouble())

            val startX = centerX + coreRadius * cos(rad).toFloat()
            val startY = centerY + coreRadius * sin(rad).toFloat()
            val endX = centerX + (coreRadius + pinLineLength) * cos(rad).toFloat()
            val endY = centerY + (coreRadius + pinLineLength) * sin(rad).toFloat()

            paint.shader = LinearGradient(
                startX, startY,
                endX, endY,
                primaryColor, secondaryColor,
                Shader.TileMode.CLAMP
            )

            paint.style = Paint.Style.STROKE
            canvas.drawLine(startX, startY, endX, endY, paint)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(endX, endY, pinHeadRadius, paint)
        }

        if (isLaunching) {
            paint.shader = LinearGradient(
                centerX, launchY - pinLineLength,
                centerX, launchY,
                primaryColor, secondaryColor,
                Shader.TileMode.CLAMP
            )

            paint.style = Paint.Style.STROKE
            canvas.drawLine(centerX, launchY, centerX, launchY - pinLineLength, paint)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(centerX, launchY - pinLineLength, pinHeadRadius, paint)
        }

        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = primaryColor
        canvas.drawCircle(centerX, centerY, coreRadius * coreScale, paint)

        paint.color = secondaryColor
        paint.textSize = 80f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(score.toString(), centerX, centerY + 28f, paint)

        if (!isGameOver && !isLaunching) {
            val standbyY = screenHeight * 0.85f

            paint.shader = LinearGradient(
                centerX, standbyY - pinLineLength,
                centerX, standbyY,
                primaryColor, secondaryColor,
                Shader.TileMode.CLAMP
            )

            paint.style = Paint.Style.STROKE
            canvas.drawLine(centerX, standbyY, centerX, standbyY - pinLineLength, paint)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(centerX, standbyY - pinLineLength, pinHeadRadius, paint)
        }

        if (isGameOver) {
            paint.shader = null
            drawGameOverUI(canvas, isNightMode)
        }
    }

    private fun checkCollisionAndPin() {
        isLaunching = false
        val hitAngle = (90f - rotationAngle)

        for (angle in pinnedAngles) {
            val diff = abs(((hitAngle % 360 + 360) % 360) - ((angle % 360 + 360) % 360))
            val normalizedDiff = if (diff > 180) 360 - diff else diff

            if (normalizedDiff < 12f) {
                triggerGameOver()
                return
            }
        }

        pinnedAngles.add(hitAngle)
        score++
        coreScale = 1.2f
    }

    private fun triggerGameOver() {
        isGameOver = true
        flashAlpha = 150
    }

    private fun drawGameOverUI(canvas: Canvas, isNightMode: Boolean) {
        val paint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        paint.color = primaryColor
        paint.textSize = 120f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("GAME OVER", screenWidth / 2f, screenHeight / 2f, paint)
    }

    override fun onTouch(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (!isGameOver) {
                if (!isLaunching) launchPin()
            } else {
                resetGame()
            }
        }
    }

    private fun launchPin() {
        isLaunching = true
        launchY = screenHeight * 0.85f
    }

    private fun resetGame() {
        score = 0
        isGameOver = false
        isLaunching = false
        coreScale = 1.0f
        flashAlpha = 0
        pinnedAngles.clear()
        rotationAngle = 0f
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        return Color.argb(
            255,
            (Color.red(color) * factor).toInt(),
            (Color.green(color) * factor).toInt(),
            (Color.blue(color) * factor).toInt()
        )
    }

    private fun lightenColor(color: Int, factor: Float): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.argb(
            255,
            (r + (255 - r) * factor).toInt(),
            (g + (255 - g) * factor).toInt(),
            (b + (255 - b) * factor).toInt()
        )
    }

    override fun release() {}
}