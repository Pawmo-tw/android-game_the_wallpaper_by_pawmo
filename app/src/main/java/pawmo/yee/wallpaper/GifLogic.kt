package pawmo.yee.wallpaper

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.MotionEvent

class GifLogic(private val context: Context) : IGameLogic {
    private var drawable: AnimatedImageDrawable? = null

    override fun loadResources(res: Resources) {
        val prefs = context.getSharedPreferences("WallpaperSettings", Context.MODE_PRIVATE)
        val uriString = prefs.getString("custom_media_uri", null) ?: return

        try {
            val source = ImageDecoder.createSource(context.contentResolver, Uri.parse(uriString))
            val decoded = ImageDecoder.decodeDrawable(source)
            if (decoded is AnimatedImageDrawable) {
                drawable = decoded
                // 設置 GIF 的大小
                drawable?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {
        drawable?.let {
            // 縮放邏輯
            val scale = Math.max(
                canvas.width.toFloat() / it.intrinsicWidth,
                canvas.height.toFloat() / it.intrinsicHeight
            )
            val drawWidth = (it.intrinsicWidth * scale).toInt()
            val drawHeight = (it.intrinsicHeight * scale).toInt()
            val left = (canvas.width - drawWidth) / 2
            val top = (canvas.height - drawHeight) / 2

            it.setBounds(left, top, left + drawWidth, top + drawHeight)
            it.draw(canvas)
        }
    }

    override fun updatePhysics(w: Int, h: Int) {}
    override fun onSensorChanged(gx: Float, gy: Float) {}
    override fun onTouch(e: MotionEvent) {}

    override fun release() {
        drawable?.stop()
        drawable = null
    }
}