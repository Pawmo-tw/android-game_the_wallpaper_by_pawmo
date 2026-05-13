package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import kotlin.random.Random

class StarFieldLogic : IGameLogic {
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
                    Random.nextFloat() * width,
                    Random.nextFloat() * height,
                    Random.nextFloat() * 6f + 2f,
                    Random.nextFloat() * 3f + 1f,
                    Random.nextFloat() * 0.5f + 0.5f // 隨機漂移係數
                ))
            }
        }

        stars.forEach { star ->
            // 垂直移動
            star.y += star.speed + (tiltY - 9.8f) * 0.5f

            // 水平移動
            star.x += tiltX * star.speed * 0.5f * star.drift

            // 邊界檢查
            handleBounds(star)
        }
    }

    private fun handleBounds(star: Star) {
        if (star.y > height) {
            star.y = -10f
            star.x = Random.nextFloat() * width
        }
        else if (star.y < -10f) {
            star.y = height.toFloat()
            star.x = Random.nextFloat() * width
        }
        if (star.x > width) {
            star.x = 0f
        } else if (star.x < 0f) {
            star.x = width.toFloat()
        }
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {

        canvas.drawColor(Color.BLACK)

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
        }

        stars.forEach { star ->
            paint.alpha = (star.speed * 35).toInt().coerceIn(80, 255)
            canvas.drawCircle(star.x, star.y, star.size, paint)
        }
    }

    override fun onTouch(event: MotionEvent) {}

    override fun release() {
        stars.clear()
    }
}