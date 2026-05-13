package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import  android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
class FlappyLogic : IGameLogic {
    private var score = -1
    private var isGameOver = false
    private var scoreCounted = false
    private var flappyY = 500f
    private var velocityY = 0f
    private val gravity = 2.2f
    private val jumpStrength = -40f
    private var pipeX = 0f
    private var pipeGapY = 500f
    private val pipeWidth = 200f
    private val pipeGapHeight = 600f
    private var gameSpeed = 12f
    private var bgX = 0f
    private var flappyBitmap: Bitmap? = null
    private var bgBitmap: Bitmap? = null
    private var pipeUpperBitmap: Bitmap? = null
    private var pipeLowerBitmap: Bitmap? = null
    private val paint = Paint().apply { isAntiAlias = true }
    private val deathSlogans = listOf(
        "BRUH",
        "Nahh",
        "not even close baby \n(technoblade never die!!!",
        "Déjà vu",
        "MAN!",
        "tap to restart",
        "Maybe this is what this digital culture is.",
        "you can just quit the game,\n and don't worry, \n I'll save your progress always,\n even your mistakes.",
        "You could have refused but you didn't",
        "manbo",
        "mamba out",
    )
    private var currentSlogan = deathSlogans[0]
    override fun loadResources(resources: Resources) {
        try {
            val birdOrg = BitmapFactory.decodeResource(resources, R.drawable.flappy_bird)
            flappyBitmap = birdOrg?.let { Bitmap.createScaledBitmap(it, 160, 160, true) }

            bgBitmap = BitmapFactory.decodeResource(resources, R.drawable.flappy_bg)

            val targetPipeWidth = 160
            val upOrg = BitmapFactory.decodeResource(resources, R.drawable.pipe_upper)
            pipeUpperBitmap = upOrg?.let {
                val scale = targetPipeWidth.toFloat() / it.width
                val targetHeight = (it.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(it, targetPipeWidth, targetHeight, true)
            }

            val lowOrg = BitmapFactory.decodeResource(resources, R.drawable.pipe_lower)
            pipeLowerBitmap = lowOrg?.let {
                val scale = targetPipeWidth.toFloat() / it.width
                val targetHeight = (it.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(it, targetPipeWidth, targetHeight, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun updatePhysics(width: Int, height: Int) {
        if (!isGameOver) {
            velocityY += gravity
            flappyY += velocityY
            if (pipeX <= -pipeWidth) {
                pipeX = width.toFloat()
                val minY = height * 0.15f
                val maxY = height * 0.55f
                pipeGapY = minY + (java.util.Random().nextFloat() * (maxY - minY))
                scoreCounted = false
            } else {
                pipeX -= gameSpeed
            }
            //score
            if (pipeX + pipeWidth < 200f && !scoreCounted) {
                score++
                scoreCounted = true
            }

            // backdrop
            bgBitmap?.let {
                bgX -= gameSpeed * 0.5f
                if (bgX <= -it.width) bgX = 0f
            }

            // 碰撞偵測
            val birdRect = RectF(220f, flappyY + 30f, 340f, flappyY + 130f)
            val upperRect = RectF(pipeX, 0f, pipeX + pipeWidth, pipeGapY)
            val lowerRect = RectF(pipeX, pipeGapY + pipeGapHeight, pipeX + pipeWidth, height.toFloat())

            if (RectF.intersects(birdRect, upperRect) ||
                RectF.intersects(birdRect, lowerRect) ||
                flappyY > height || flappyY < -200f) {
                if (!isGameOver) {
                    isGameOver = true
                    velocityY = -10f

                    currentSlogan = deathSlogans.random()
                }
                isGameOver = true
                velocityY = -10f

            }
        } else {

            if (flappyY < height) {
                velocityY += gravity
                flappyY += velocityY
            } else {
                velocityY = 0f
                flappyY = height.toFloat()
            }
        }


        bgBitmap?.let {
            if (it.height != height && height > 0) {
                val scale = height.toFloat() / it.height.toFloat()
                val newWidth = (it.width * scale).toInt().coerceAtLeast(1)
                bgBitmap = Bitmap.createScaledBitmap(it, newWidth, height, true)
            }
        }
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {
        val viewWidth = canvas.width
        val viewHeight = canvas.height
        if (viewWidth <= 0 || viewHeight <= 0) return



        bgBitmap?.let { bmt ->
            canvas.drawBitmap(bmt, bgX, 0f, null)
            if (bgX + bmt.width < viewWidth) {
                canvas.drawBitmap(bmt, bgX + bmt.width, 0f, null)
            }
        }


        val upPipe = pipeUpperBitmap
        val lowPipe = pipeLowerBitmap
        val pWidth = upPipe?.width?.toFloat() ?: 160f

        upPipe?.let {
            canvas.save()
            canvas.clipRect(pipeX, 0f, pipeX + pWidth, pipeGapY)
            canvas.drawBitmap(it, pipeX, pipeGapY - it.height, null)
            canvas.restore()
        }
        lowPipe?.let {
            canvas.save()
            canvas.clipRect(pipeX, pipeGapY + pipeGapHeight, pipeX + pWidth, viewHeight.toFloat())
            canvas.drawBitmap(it, pipeX, pipeGapY + pipeGapHeight, null)
            canvas.restore()
        }

        // flappy繪製
        flappyBitmap?.let { bird ->
            val matrix = Matrix()
            matrix.setTranslate(200f, flappyY)

            val rotation = if (!isGameOver) {
                (velocityY * 1.8f).coerceIn(-30f, 30f)
            } else {
                80f
            }
            matrix.postRotate(rotation, 200f + bird.width / 2f, flappyY + bird.height / 2f)
            canvas.drawBitmap(bird, matrix, paint)
        }

        // 繪製分數
        paint.color = Color.WHITE
        paint.textSize = 100f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(score.toString(), viewWidth / 2f, 200f, paint)

        if (isGameOver) {
            // GAME OVER
            paint.textAlign = Paint.Align.CENTER // 置中
            paint.color = Color.WHITE
            paint.textSize = 100f
            canvas.drawText("GAME OVER", viewWidth / 2f, viewHeight / 2f, paint)

            // TextPaint
            val textPaint = TextPaint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 45f
            }

            val staticLayout = StaticLayout.Builder.obtain(
                currentSlogan,
                0,
                currentSlogan.length,
                textPaint,
                viewWidth
            ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

            canvas.save()


            canvas.translate(0f, viewHeight / 2f + 120f)

            staticLayout.draw(canvas)
            canvas.restore()
        }
    }

    override fun onTouch(event: MotionEvent) {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (!isGameOver) {
                velocityY = jumpStrength
            } else {

                resetGame(screenWidth = 1080)
            }
        }
    }

    private fun resetGame(screenWidth: Int) {
        flappyY = 500f
        velocityY = 0f
        pipeX = screenWidth.toFloat()
        score = 0
        isGameOver = false
        scoreCounted = false
    }

    override fun release() {
        flappyBitmap?.recycle()
        bgBitmap?.recycle()
        pipeUpperBitmap?.recycle()
        pipeLowerBitmap?.recycle()
        flappyBitmap = null
        bgBitmap = null
        pipeUpperBitmap = null
        pipeLowerBitmap = null
    }
}