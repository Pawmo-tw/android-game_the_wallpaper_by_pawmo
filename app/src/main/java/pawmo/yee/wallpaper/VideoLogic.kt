package pawmo.yee.wallpaper

import android.content.Context
import android.content.res.Resources
import  android.util.Log
import android.media.MediaPlayer
import android.net.Uri
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.graphics.Canvas
class VideoLogic(private val context: Context, private val holder: SurfaceHolder) : IGameLogic {
    private var mediaPlayer: MediaPlayer? = null

    override fun loadResources(res: Resources) {
        val prefs = context.getSharedPreferences("WallpaperSettings", Context.MODE_PRIVATE)
        val uriString = prefs.getString("custom_media_uri", null) ?: return

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(uriString))
                setSurface(holder.surface)
                isLooping = true
                // 設置縮放模式
                setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("VideoLogic", "Media failed", e)
            e.printStackTrace()
        }
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {}

    override fun updatePhysics(w: Int, h: Int) {}

    override fun onSensorChanged(gx: Float, gy: Float) {}

    override fun onTouch(e: MotionEvent) {}
    override fun release() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}