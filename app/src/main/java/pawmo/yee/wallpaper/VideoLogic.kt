package pawmo.yee.wallpaper

import android.content.Context
import android.content.res.Resources
import android.util.Log
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

        if (!holder.surface.isValid) {
            Log.e("VideoLogic", "Surface is not valid yet.")
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(uriString))
                setSurface(holder.surface)
                isLooping = true
                setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)

                setOnPreparedListener { mp ->
                    Log.d("VideoLogic", "MediaPlayer prepared, starting video...")
                    mp.start()
                }

                setOnErrorListener { mp, what, extra ->
                    Log.e("VideoLogic", "MediaPlayer error: what=$what, extra=$extra")
                    release()
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("VideoLogic", "Media failed to initialize", e)
            e.printStackTrace()
            release()
        }
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {}
    override fun updatePhysics(w: Int, h: Int) {}
    override fun onSensorChanged(gx: Float, gy: Float) {}
    override fun onTouch(e: MotionEvent) {}

    override fun release() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e("VideoLogic", "Error during release", e)
        } finally {
            mediaPlayer = null
        }
    }
}