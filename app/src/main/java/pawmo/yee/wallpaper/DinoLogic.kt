package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import kotlin.math.abs

class DinoLogic : IGameLogic {


    private var bgShader: LinearGradient? = null
    private val bgPaint = Paint()
    private var cachedHeight = 0


    private class Star(var x: Float, var y: Float, var size: Float, var speedFactor: Float)
    private val stars = mutableListOf<Star>()


    private class GroundBump(var x: Float, var width: Float, var height: Float)
    private val groundBumps = mutableListOf<GroundBump>()

    override var primaryColor: Int = 0xFF34C759.toInt()
        set(value) {
            if (field != value) {
                field = value
                bgShader = null
            }
        }

    override var secondaryColor: Int = 0xFFFF9500.toInt()
        set(value) {
            if (field != value) {
                field = value
                bgShader = null
            }
        }

    private var isDead = false
    private var isRespawning = false
    private var walkFrame = 0
    private var walkCounter = 0
    private var dinoY = 0f
    private var velocityY = 0f
    private val gravity = 3f
    private var isJumping = false
    private var obstacleX = 0f
    private var gameSpeed = 15f
    private var score = 0
    private val gamePaint = Paint().apply { isAntiAlias = true }
    private var dinoRun1: Bitmap? = null
    private var dinoRun2: Bitmap? = null
    private var dinoDead: Bitmap? = null
    private var dinoRun1_black: Bitmap? = null
    private var dinoRun2_black: Bitmap? = null
    private var dinoDead_black: Bitmap? = null
    private val cactusBitmaps = mutableListOf<Bitmap>()
    private var currentCactusBitmap: Bitmap? = null

    override fun loadResources(resources: Resources) {
        try {
            dinoRun1 = decodeAndScale(resources, R.drawable.dino_run1, 200, 200)
            dinoRun2 = decodeAndScale(resources, R.drawable.dino_run2, 200, 200)
            dinoDead = decodeAndScale(resources, R.drawable.dino_dead, 200, 200)

            dinoRun1_black = decodeAndScale(resources, R.drawable.dino_run1_black, 200, 200)
            dinoRun2_black = decodeAndScale(resources, R.drawable.dino_run2_black, 200, 200)
            dinoDead_black = decodeAndScale(resources, R.drawable.dino_dead_black, 200, 200)

            val c1 = decodeAndScale(resources, R.drawable.cactus1, 100, 170)
            val c2 = decodeAndScale(resources, R.drawable.cactus2, 100, 170)
            val c3 = decodeAndScale(resources, R.drawable.cactus3, 140, 170)
            if (c1 != null) cactusBitmaps.add(c1)
            if (c2 != null) cactusBitmaps.add(c2)
            if (c3 != null) cactusBitmaps.add(c3)
            if (cactusBitmaps.isNotEmpty()) currentCactusBitmap = cactusBitmaps.random()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun decodeAndScale(res: Resources, resId: Int, w: Int, h: Int): Bitmap? {
        val b = BitmapFactory.decodeResource(res, resId)
        return if (b != null) Bitmap.createScaledBitmap(b, w, h, true) else null
    }

    override fun updatePhysics(width: Int, height: Int) {
        val groundY = height * 0.75f
        if (dinoY == 0f) dinoY = groundY


        if (stars.isEmpty() && width > 0 && height > 0) {
            val maxStarY = height * 0.4f
            for (i in 0 until 30) {
                val randomSpeedFactor = kotlin.random.Random.nextFloat() * (0.25f - 0.05f) + 0.05f
                stars.add(Star(
                    x = (0..width).random().toFloat(),
                    y = (0..maxStarY.toInt()).random().toFloat(),
                    size = (6..12).random().toFloat(),
                    speedFactor = randomSpeedFactor
                ))
            }
        }

        // 初始化俗頭
        if (groundBumps.isEmpty() && width > 0) {
            val segment = width / 5f
            for (i in 0 until 5) {
                groundBumps.add(GroundBump(
                    x = i * segment + (0..100).random(),
                    width = (15..40).random().toFloat(),
                    height = (6..15).random().toFloat()
                ))
            }
        }

        // 星星獨立飄移
        if (width > 0) {
            stars.forEach { star ->
                star.x -= gameSpeed * star.speedFactor
                if (star.x < -30f) {
                    star.x = width.toFloat() + (10..200).random()
                    star.y = (0..(height * 0.4f).toInt()).random().toFloat()
                    star.speedFactor = kotlin.random.Random.nextFloat() * (0.25f - 0.05f) + 0.05f
                }
            }
        }

        if (isDead) {
            dinoY += 40f
            if (dinoY > height + 200) {
                isDead = false
                isRespawning = true
                dinoY = -500f
                velocityY = 0f
            }
            return
        }

        if (isRespawning) {
            velocityY += gravity
            dinoY += velocityY
            if (dinoY >= groundY) { dinoY = groundY; velocityY = 0f; isRespawning = false; isJumping = false }
            return
        }

        walkCounter++
        if (walkCounter > 6) { walkFrame = if (walkFrame == 0) 1 else 0; walkCounter = 0 }

        dinoY += velocityY
        if (dinoY < groundY) { velocityY += gravity } else { dinoY = groundY; isJumping = false; velocityY = 0f }


        groundBumps.forEach { bump ->
            bump.x -= gameSpeed
            if (bump.x < -60f) {
                bump.x = width.toFloat() + (20..300).random()
                bump.width = (15..40).random().toFloat()
                bump.height = (6..15).random().toFloat()
            }
        }

        if (obstacleX < -200) {
            obstacleX = width.toFloat() + (100..800).random()
            score++; gameSpeed += 0.2f
            if (cactusBitmaps.isNotEmpty()) currentCactusBitmap = cactusBitmaps.random()
        } else {
            obstacleX -= gameSpeed
        }

        val dinoRect = RectF(120f, dinoY - 130f, 230f, dinoY - 10f)
        val cWidth = currentCactusBitmap?.width?.toFloat() ?: 60f
        val cactusRect = RectF(obstacleX + 10f, groundY - 110f, obstacleX + cWidth - 10f, groundY)
        if (RectF.intersects(dinoRect, cactusRect)) {
            android.util.Log.d("DINO_DEBUG", "DEAD")
            isDead = true
            score = 0
            gameSpeed = 15f
            obstacleX = width.toFloat() + 500
        }
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {
        val groundY = canvas.height * 0.75f

        // 繪製天空
        if (bgShader == null || cachedHeight != canvas.height) {
            cachedHeight = canvas.height
            bgShader = LinearGradient(
                0f, 0f, 0f, cachedHeight.toFloat(),
                primaryColor, secondaryColor,
                Shader.TileMode.CLAMP
            )
            bgPaint.shader = bgShader
        }
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), bgPaint)

        // 繪製星星
        gamePaint.color = secondaryColor
        gamePaint.style = Paint.Style.FILL
        stars.forEach { star ->
            canvas.drawRect(star.x, star.y, star.x + star.size, star.y + star.size, gamePaint)
        }

        gamePaint.color = primaryColor

        // 俗頭
        gamePaint.style = Paint.Style.FILL
        groundBumps.forEach { bump ->
            canvas.drawRect(bump.x, groundY - bump.height, bump.x + bump.width, groundY + 4f, gamePaint)
        }

        // 地面線
        gamePaint.style = Paint.Style.STROKE
        gamePaint.strokeWidth = 8f
        canvas.drawLine(0f, groundY, canvas.width.toFloat(), groundY, gamePaint)
        gamePaint.strokeWidth = 0f // 重置粗度

        val currentDino: Bitmap? = if (isDead || isRespawning) {
            if (isNightMode) dinoDead_black else dinoDead
        } else if (isJumping) {
            if (isNightMode) dinoRun1_black else dinoRun1
        } else {
            if (isNightMode) (if (walkFrame == 0) dinoRun1_black else dinoRun2_black)
            else (if (walkFrame == 0) dinoRun1 else dinoRun2)
        }

        // 繪製小恐龍
        if (currentDino != null) {
            val dinoDrawY = dinoY - currentDino.height
            if (!isDead && (abs(velocityY) > 10f || gameSpeed > 15f)) {
                val blurSteps = 2
                for (i in 1..blurSteps) {
                    gamePaint.alpha = 60 / i
                    val offsetX = 100f - (gameSpeed * i * 0.4f)
                    val offsetY = dinoDrawY - (velocityY * i * 0.2f)
                    canvas.drawBitmap(currentDino, offsetX, offsetY, gamePaint)
                }
            }
            gamePaint.alpha = 255
            gamePaint.style = Paint.Style.FILL
            canvas.drawBitmap(currentDino, 100f, dinoDrawY, gamePaint)
        }

        // 繪製仙人掌
        if (currentCactusBitmap != null) {
            val cactusDrawY = groundY - currentCactusBitmap!!.height
            if (gameSpeed > 15f && !isDead) {
                val blurSteps = 4
                for (i in 1..blurSteps) {
                    gamePaint.alpha = 50 / i
                    canvas.drawBitmap(currentCactusBitmap!!, obstacleX + (gameSpeed * i * 0.5f), cactusDrawY, gamePaint)
                }
            }
            gamePaint.alpha = 255
            gamePaint.style = Paint.Style.FILL
            canvas.drawBitmap(currentCactusBitmap!!, obstacleX, cactusDrawY, gamePaint)
        }

        // 分數繪製
        gamePaint.color = if (isNightMode) Color.WHITE else Color.BLACK
        gamePaint.textSize = 60f
        gamePaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("SCORE: $score", 50f, 150f, gamePaint)
    }

    override fun onTouch(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN && !isDead && !isRespawning) {
            if (!isJumping) { velocityY = -45f; isJumping = true }
        }
    }

    override fun release() {
        dinoRun1?.recycle(); dinoRun2?.recycle(); dinoDead?.recycle()
        dinoRun1_black?.recycle(); dinoRun2_black?.recycle(); dinoDead_black?.recycle()
        cactusBitmaps.forEach { it.recycle() }
        stars.clear()
        groundBumps.clear()
    }
}