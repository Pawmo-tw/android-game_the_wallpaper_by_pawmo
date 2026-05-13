package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import kotlin.random.Random

class MatrixLogic : IGameLogic {

    private val fontSize = 30f
    private val columnGap = 35
    private val characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ"

    private var columns = 0
    private var canvasWidth = 0
    private var canvasHeight = 0

    private lateinit var dropPositions: IntArray
    private lateinit var speeds: IntArray

    // 重力感應變數
    private var tiltX = 0f
    private var tiltY = 0f

    private var scanLineY = 0f
    private val geoPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val paint = Paint().apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
        textSize = fontSize
        isAntiAlias = true
    }

    override fun loadResources(resources: Resources) {}

    override fun onSensorChanged(x: Float, y: Float) {

        tiltX = tiltX * 0.9f + (-x) * 0.1f
        tiltY = tiltY * 0.9f + y * 0.1f
    }

    override fun updatePhysics(width: Int, height: Int) {
        if (canvasWidth != width || canvasHeight != height) {
            canvasWidth = width
            canvasHeight = height
            columns = (width / columnGap) + 10
            dropPositions = IntArray(columns) { Random.nextInt(height / fontSize.toInt()) }
            speeds = IntArray(columns) { Random.nextInt(2, 5) }
        }

        for (i in 0 until columns) {
            dropPositions[i] += speeds[i]
            if (dropPositions[i] * fontSize > canvasHeight * 1.5) {
                if (Random.nextFloat() > 0.95f) {
                    dropPositions[i] = -5
                    speeds[i] = Random.nextInt(2, 5)
                }
            }
        }

        scanLineY += 8f
        if (scanLineY > canvasHeight) scanLineY = 0f
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {
        // 1. 半透明背景
        canvas.drawColor(Color.argb(45, 0, 0, 0))

        canvas.save()

        canvas.skew(tiltX * 0.05f, 0f)


        canvas.translate(-tiltX * (canvasHeight / 20f), 0f)


        drawGeometry(canvas)


        for (i in 0 until columns) {
            val char = characters[Random.nextInt(characters.length)].toString()
            val x = (i * columnGap).toFloat() - (columnGap * 5)
            val y = dropPositions[i] * fontSize


            paint.color = Color.WHITE
            paint.setShadowLayer(10f, 0f, 0f, Color.GREEN)
            canvas.drawText(char, x, y, paint)
            paint.clearShadowLayer()


            paint.color = Color.rgb(0, 255, 65)
            canvas.drawText(char, x, y - fontSize, paint)


            if (Random.nextFloat() > 0.995f) {
                canvas.drawText(char, x, Random.nextInt(canvasHeight).toFloat(), paint)
            }
        }

        canvas.restore()
    }

    private fun drawGeometry(canvas: Canvas) {
        geoPaint.color = Color.argb(30, 0, 255, 65)
        geoPaint.strokeWidth = 3f
        canvas.drawLine(-canvasWidth.toFloat(), scanLineY, canvasWidth.toFloat() * 2, scanLineY, geoPaint)

        geoPaint.color = Color.argb(15, 0, 255, 65)
        geoPaint.strokeWidth = 1f
        val gridStep = 200f
        var curX = -gridStep * 5
        while (curX < canvasWidth + gridStep * 5) {
            canvas.drawLine(curX, 0f, curX, canvasHeight.toFloat(), geoPaint)
            curX += gridStep
        }
        var curY = 0f
        while (curY < canvasHeight) {
            canvas.drawLine(-canvasWidth.toFloat(), curY, canvasWidth.toFloat() * 2, curY, geoPaint)
            curY += gridStep
        }

        if (Random.nextFloat() > 0.95f) {
            val rx = Random.nextFloat() * canvasWidth
            val ry = Random.nextFloat() * canvasHeight
            geoPaint.color = Color.argb(40, 0, 255, 65)
            canvas.drawCircle(rx, ry, Random.nextFloat() * 100f + 50f, geoPaint)
        }
    }

    override fun onTouch(event: MotionEvent) {}

    override fun release() {}
}