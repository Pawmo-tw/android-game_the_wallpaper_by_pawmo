package pawmo.yee.wallpaper

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder

class DinoWallpaperService : WallpaperService(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gravityX = 0f
    private var gravityY = 0f

    override fun onCreateEngine(): Engine = PEngine()

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    inner class PEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var gameLogic: IGameLogic? = null
        private var isVisible = false
        private var currentMode: String = "LIQUID"

        private val drawRunnable = object : Runnable {
            override fun run() {
                if (!isVisible) return

                if (currentMode == "VIDEO") {
                    handler.postDelayed(this, 100)
                    return
                }

                val holder = surfaceHolder
                var canvas: Canvas? = null
                try {
                    canvas = holder.lockCanvas()
                    if (canvas != null) {
                        val isNightMode = (resources.configuration.uiMode and
                                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

                        gameLogic?.apply {
                            onSensorChanged(gravityX, gravityY)
                            updatePhysics(canvas.width, canvas.height)
                            draw(canvas, isNightMode)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    canvas?.let { holder.unlockCanvasAndPost(it) }
                }

                handler.postDelayed(this, 12)
            }
        }

        override fun onTouchEvent(event: MotionEvent?) {
            val e = event ?: return
            if (e.action == MotionEvent.ACTION_DOWN) {
                if (e.x in 20f..250f && e.y in 20f..100f) {
                    cycleGameMode()
                    return
                }
            }
            gameLogic?.onTouch(e)
        }

        private fun cycleGameMode() {
            val prefs = getSharedPreferences("WallpaperSettings", Context.MODE_PRIVATE)
            val nextMode = if (currentMode == "LIQUID") "LINE" else "LIQUID"
            prefs.edit().putString("game_mode", nextMode).apply()
            initGameLogic()
        }

        private fun initGameLogic() {
            // 釋放舊資源
            gameLogic?.release()

            val prefs = getSharedPreferences("WallpaperSettings", Context.MODE_PRIVATE)
            currentMode = prefs.getString("game_mode", "LIQUID") ?: "LIQUID"

            gameLogic = when (currentMode) {
                "DINO" -> DinoLogic()
                "FLAPPY" -> FlappyLogic()
                "COREBALL" -> CoreballLogic()
                "2048" -> Game2048Logic()
                "CROSSY" -> CrossyLogic()
                "MATRIX" -> MatrixLogic()
                "STARS" -> StarFieldLogic()
                "LIQUID" -> LiquidLogic()
                "LINE" -> LineLogic()
                "RIPPLE" -> RippleLogic()
                "VIDEO" -> VideoLogic(applicationContext, surfaceHolder)
                "GIF" -> GifLogic(applicationContext)
                else -> DinoLogic()
            }

            gameLogic?.loadResources(resources)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.isVisible = visible
            if (visible) {
                registerSensor()
                initGameLogic()
                handler.post(drawRunnable)
            } else {
                unregisterSensor()
                handler.removeCallbacks(drawRunnable)
                gameLogic?.release() // 隱藏時立即暫停影片
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            this.isVisible = false
            unregisterSensor()
            handler.removeCallbacks(drawRunnable)
            gameLogic?.release()
        }

        override fun onDestroy() {
            super.onDestroy()
            unregisterSensor()
            gameLogic?.release()
        }

        private fun registerSensor() {
            sensorManager?.registerListener(this@DinoWallpaperService, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        private fun unregisterSensor() {
            sensorManager?.unregisterListener(this@DinoWallpaperService)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            gravityX = gravityX * 0.8f + event.values[0] * 0.2f
            gravityY = gravityY * 0.8f + event.values[1] * 0.2f
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}