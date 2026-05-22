package pawmo.yee.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var lastClickTime = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { handleCustomMedia(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        val modes = mapOf(
            R.id.llDino to "DINO", R.id.llFlappy to "FLAPPY", R.id.llCoreball to "COREBALL",
            R.id.ll2048 to "2048", R.id.llCrossy to "CROSSY", R.id.llMatrix to "MATRIX",
            R.id.llParticles to "STARS", R.id.llLiquid to "LIQUID", R.id.llAdvanced to "RIPPLE",
            R.id.llLine to "LINE"
        )
        modes.forEach { (id, mode) ->
            findViewById<LinearLayout>(id).setOnClickListener {
                if (System.currentTimeMillis() - lastClickTime < 800) return@setOnClickListener
                lastClickTime = System.currentTimeMillis()

                getSharedPreferences("WallpaperSettings", Context.MODE_PRIVATE).edit().putString("game_mode", mode).apply()
                sendBroadcast(Intent("pawmo.yee.wallpaper.SETTING_CHANGED").apply { setPackage(packageName) })
                showToast(mode)
                openWallpaperSettings()
            }
        }
        findViewById<LinearLayout>(R.id.llUpload).setOnClickListener {
            pickMediaLauncher.launch(arrayOf("video/mp4", "image/gif"))
        }
    }

    private fun handleCustomMedia(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val mode = if (contentResolver.getType(uri)?.contains("video") == true) "VIDEO" else "GIF"

            getSharedPreferences("WallpaperSettings", Context.MODE_PRIVATE).edit().apply {
                putString("game_mode", mode)
                putString("custom_media_uri", uri.toString())
            }.apply()

            sendBroadcast(Intent("pawmo.yee.wallpaper.SETTING_CHANGED").apply { setPackage(packageName) })
            mainHandler.postDelayed({ openWallpaperSettings() }, 150)
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("fail")
        }
    }

    private fun openWallpaperSettings() {
        val comp = ComponentName(this, DinoWallpaperService::class.java)
        val brand = Build.MANUFACTURER.lowercase() + Build.BRAND.lowercase()
        val isXiaomi = brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco")

        if (isXiaomi) {
            // 系統桌布確認套用頁面
            try {
                startActivity(Intent().apply {
                    component = ComponentName("com.android.wallpaper.livepicker", "com.android.wallpaper.livepicker.LiveWallpaperPreview")
                    putExtra("android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT", comp)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 系統動態桌布選擇列表
            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
                showToast("click「game the wallpaper」 pls")
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 原生 Android 邏輯
        try {
            startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, comp)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            } catch (any: Exception) {
                showToast("err :(")
            }
        }
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}