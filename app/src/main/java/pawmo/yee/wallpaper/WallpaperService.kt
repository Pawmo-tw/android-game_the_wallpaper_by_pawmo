package pawmo.yee.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
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
        private var currentMode = "DINO"

        private val drawRunnable = object : Runnable {
            override fun run() {
                if (!isVisible) return
                var canvas: Canvas? = null
                try {
                    canvas = surfaceHolder.lockCanvas()
                    canvas?.let {
                        if (currentMode == "VIDEO") {
                            // 清空畫布維持 Surface 活躍
                            it.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
                        } else {
                            val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                            gameLogic?.apply {
                                onSensorChanged(gravityX, gravityY)
                                updatePhysics(it.width, it.height)
                                draw(it, isNight)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    canvas?.let { surfaceHolder.unlockCanvasAndPost(it) }
                }
                handler.postDelayed(this, if (currentMode == "VIDEO") 100L else 12L)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
        }

        // 小米 系統 Tap 指令
        override fun onCommand(action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean): Bundle? {
            if (action == WallpaperManager.COMMAND_TAP) {
                dispatchVirtualTouch(x.toFloat(), y.toFloat())
            }
            return super.onCommand(action, x, y, z, extras, resultRequested)
        }

        override fun onTouchEvent(event: MotionEvent?) {
            event?.let { dispatchTouchLogic(it) }
            super.onTouchEvent(event)
        }

        // 統一處理虛擬與原生觸摸事件
        private fun dispatchVirtualTouch(x: Float, y: Float) {
            val down = MotionEvent.obtain(System.currentTimeMillis(), System.currentTimeMillis(), MotionEvent.ACTION_DOWN, x, y, 0)
            val isShortcut = dispatchTouchLogic(down)
            if (!isShortcut) {
                val up = MotionEvent.obtain(System.currentTimeMillis(), System.currentTimeMillis(), MotionEvent.ACTION_UP, x, y, 0)
                gameLogic?.onTouch(up)
                up.recycle()
            }
            down.recycle()
        }

        private fun dispatchTouchLogic(e: MotionEvent): Boolean {
            if (e.action == MotionEvent.ACTION_DOWN && e.x in 20f..250f && e.y in 20f..100f) {
                cycleGameMode()
                return true
            }
            gameLogic?.onTouch(e)
            return false
        }

        private fun cycleGameMode() {
            val prefs = getSharedPreferences("WallpaperSettings", Context.MODE_PRIVATE)
            val nextMode = if (currentMode == "LIQUID") "LINE" else "LIQUID"
            prefs.edit().putString("game_mode", nextMode).apply()
            initGameLogic()
        }

        private fun initGameLogic() {
            gameLogic?.release()
            handler.removeCallbacks(drawRunnable)

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
            }.apply { loadResources(resources) }

            if (isVisible) handler.post(drawRunnable)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.isVisible = visible
            if (visible) {
                sensorManager?.registerListener(this@DinoWallpaperService, accelerometer, SensorManager.SENSOR_DELAY_GAME)
                initGameLogic()
            } else {
                cleanUp()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            cleanUp()
        }

        override fun onDestroy() {
            super.onDestroy()
            cleanUp()
        }

        private fun cleanUp() {
            this.isVisible = false
            sensorManager?.unregisterListener(this@DinoWallpaperService)
            handler.removeCallbacks(drawRunnable)
            gameLogic?.release()
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