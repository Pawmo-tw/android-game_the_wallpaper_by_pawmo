package pawmo.yee.wallpaper

import android.content.Context
import android.graphics.*
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface

class VideoBgPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    private var frameBitmap: Bitmap? = null
    private var lastCanvas: Canvas? = null

    // 用於捕捉影片畫面的變數
    private val textureId = 101

    fun setSource(uri: Uri) {
        release()
        try {
            // 初始化一個隱形的 SurfaceTexture 來接收影片幀
            surfaceTexture = SurfaceTexture(textureId)
            surface = Surface(surfaceTexture)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setSurface(surface)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateFrame(width: Int, height: Int) {
        if (mediaPlayer == null || !mediaPlayer!!.isPlaying) return

        // 建立或更新用於存儲影片幀的 Bitmap
        if (frameBitmap == null || frameBitmap!!.width != width) {
            frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            lastCanvas = Canvas(frameBitmap!!)
        }

        // 這裡需要注意的是，Canvas API 無法直接從 SurfaceTexture 抓圖
        // 實務上，動態桌布通常會透過 WallpaperService 的 Surface 播放影片。
        // 使用 Movie 類別會簡單得多。如果是 MP4，則改為下方邏輯。
    }

    fun draw(canvas: Canvas, width: Int, height: Int) {
        frameBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
    }

    fun release() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        surface?.release()
        surfaceTexture?.release()
    }
}