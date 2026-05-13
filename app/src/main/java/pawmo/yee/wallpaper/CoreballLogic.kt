package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import kotlin.math.*

class CoreballLogic : IGameLogic {

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


        canvas.drawColor(if (isNightMode) Color.parseColor("#121212") else Color.WHITE)
        if (flashAlpha > 0) {
            canvas.drawColor(Color.argb(flashAlpha, 255, 255, 255))
        }

        val paint = Paint().apply {
            isAntiAlias = true
            strokeWidth = 6f
            color = if (isNightMode) Color.WHITE else Color.BLACK
        }


        for (angle in pinnedAngles) {
            val currentAngle = (angle + rotationAngle)
            val rad = Math.toRadians(currentAngle.toDouble())

            val startX = centerX + coreRadius * cos(rad).toFloat()
            val startY = centerY + coreRadius * sin(rad).toFloat()
            val endX = centerX + (coreRadius + pinLineLength) * cos(rad).toFloat()
            val endY = centerY + (coreRadius + pinLineLength) * sin(rad).toFloat()

            canvas.drawLine(startX, startY, endX, endY, paint)
            canvas.drawCircle(endX, endY, pinHeadRadius, paint)
        }


        if (isLaunching) {
            canvas.drawLine(centerX, launchY, centerX, launchY - pinLineLength, paint)
            canvas.drawCircle(centerX, launchY - pinLineLength, pinHeadRadius, paint)
        }


        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, coreRadius * coreScale, paint)


        paint.color = if (isNightMode) Color.BLACK else Color.WHITE
        paint.textSize = 80f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(score.toString(), centerX, centerY + 28f, paint)


        if (!isGameOver && !isLaunching) {
            paint.color = if (isNightMode) Color.WHITE else Color.BLACK
            val standbyY = screenHeight * 0.85f
            canvas.drawLine(centerX, standbyY, centerX, standbyY - pinLineLength, paint)
            canvas.drawCircle(centerX, standbyY - pinLineLength, pinHeadRadius, paint)
        }

        if (isGameOver) drawGameOverUI(canvas, isNightMode)
    }

    private fun checkCollisionAndPin() {
        isLaunching = false
        val hitAngle = (90f - rotationAngle)

        // 碰撞檢查邏輯
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


        paint.color = Color.WHITE
        paint.textSize = 120f
        paint.isFakeBoldText = true
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

    override fun release() {}
}