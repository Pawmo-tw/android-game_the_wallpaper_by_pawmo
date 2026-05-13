package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import kotlin.math.abs
class DinoLogic : IGameLogic {

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
        canvas.drawColor(if (isNightMode) Color.parseColor("#202124") else Color.parseColor("#f7f7f7"))
        val groundY = canvas.height * 0.75f

        gamePaint.color = if (isNightMode) Color.GRAY else Color.parseColor("#d3d3d3")
        canvas.drawLine(0f, groundY, canvas.width.toFloat(), groundY, gamePaint)

        val currentDino: Bitmap? = if (isDead || isRespawning) {
            if (isNightMode) dinoDead_black else dinoDead
        } else if (isJumping) {
            if (isNightMode) dinoRun1_black else dinoRun1
        } else {
            if (isNightMode) (if (walkFrame == 0) dinoRun1_black else dinoRun2_black)
            else (if (walkFrame == 0) dinoRun1 else dinoRun2)
        }

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
            canvas.drawBitmap(currentDino, 100f, dinoDrawY, gamePaint)
        }

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
    }
}