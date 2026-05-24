package pawmo.yee.wallpaper

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.service.wallpaper.WallpaperService

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
        private var isReceiverRegistered = false

        private var frameDelay = 16L

        private fun syncColorsToLogic() {
            gameLogic?.let { logic ->
                val prefs = getSharedPreferences("WallpaperSettings", Context.MODE_PRIVATE)
                logic.primaryColor = prefs.getInt("primary_color", 0xFF34C345.toInt())
                logic.secondaryColor = prefs.getInt("secondary_color", 0xFFFF6343.toInt())
            }
        }

        // 監聽來自 MainActivity 的設定廣播
        private val settingReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "pawmo.yee.wallpaper.SETTING_CHANGED") {
                    updateFpsConfig() // 即時更新 FPS 延遲時間

                    val prefs = getSharedPreferences("WallpaperSettings", Context.MODE_PRIVATE)
                    val newMode = prefs.getString("game_mode", "LIQUID") ?: "LIQUID"
                    if (newMode != currentMode) {
                        initGameLogic()
                    } else {
                        syncColorsToLogic()
                    }
                }
            }
        }

        private val drawRunnable = object : Runnable {
            override fun run() {
                if (!isVisible) return
                var canvas: Canvas? = null
                try {
                    canvas = surfaceHolder.lockCanvas()
                    canvas?.let {
                        if (currentMode == "VIDEO") {
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
                handler.postDelayed(this, if (currentMode == "VIDEO") 100L else frameDelay)
            }
        }

        private fun updateFpsConfig() {
            val prefs = getSharedPreferences("WallpaperSettings", Context.MODE_PRIVATE)
            val fps = prefs.getInt("fps", 60)
            frameDelay = 1000L / fps.coerceAtLeast(1)
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
        }

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
            val prefs = getSharedPreferences("WallpaperSettings", Context.MODE_PRIVATE)
            val targetMode = prefs.getString("game_mode", "LIQUID") ?: "LIQUID"

            if (gameLogic != null && currentMode == targetMode) {
                updateFpsConfig()
                syncColorsToLogic()
                if (isVisible) {
                    handler.removeCallbacks(drawRunnable)
                    handler.post(drawRunnable)
                }
                return
            }

            gameLogic?.release()
            handler.removeCallbacks(drawRunnable)
            currentMode = targetMode

            updateFpsConfig()

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

            syncColorsToLogic()

            if (isVisible) handler.post(drawRunnable)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.isVisible = visible
            if (visible) {
                sensorManager?.registerListener(this@DinoWallpaperService, accelerometer, SensorManager.SENSOR_DELAY_GAME)

                if (!isReceiverRegistered) {
                    val filter = IntentFilter("pawmo.yee.wallpaper.SETTING_CHANGED")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        registerReceiver(settingReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                    } else {
                        registerReceiver(settingReceiver, filter)
                    }
                    isReceiverRegistered = true
                }
                initGameLogic()
            } else {

                pauseEngine()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            releaseEngine()
        }

        override fun onDestroy() {
            super.onDestroy()
            releaseEngine()
        }

        private fun pauseEngine() {
            sensorManager?.unregisterListener(this@DinoWallpaperService)
            handler.removeCallbacks(drawRunnable)
        }

        private fun releaseEngine() {
            this.isVisible = false
            pauseEngine()

            if (isReceiverRegistered) {
                try {
                    unregisterReceiver(settingReceiver)
                } catch (e: Exception) {

                }
                isReceiverRegistered = false
            }

            gameLogic?.release()
            gameLogic = null
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