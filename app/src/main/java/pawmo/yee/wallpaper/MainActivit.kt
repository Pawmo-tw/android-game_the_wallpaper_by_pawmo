package pawmo.yee.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {


    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            handleCustomMedia(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        findViewById<LinearLayout>(R.id.llDino).setOnClickListener { handleModeSelection("DINO", "Dino!") }
        findViewById<LinearLayout>(R.id.llFlappy).setOnClickListener { handleModeSelection("FLAPPY", "Flappy Bird!") }
        findViewById<LinearLayout>(R.id.llCoreball).setOnClickListener { handleModeSelection("COREBALL", "Coreball!") }
        findViewById<LinearLayout>(R.id.ll2048).setOnClickListener { handleModeSelection("2048", "2048!") }
        findViewById<LinearLayout>(R.id.llCrossy).setOnClickListener { handleModeSelection("CROSSY", "Crossy Road!") }


        findViewById<LinearLayout>(R.id.llMatrix).setOnClickListener { handleModeSelection("MATRIX", "Matrix!") }
        findViewById<LinearLayout>(R.id.llParticles).setOnClickListener { handleModeSelection("STARS", "Star!") }
        findViewById<LinearLayout>(R.id.llLiquid).setOnClickListener { handleModeSelection("LIQUID", "Liquid!") }
        findViewById<LinearLayout>(R.id.llAdvanced).setOnClickListener { handleModeSelection("RIPPLE", "Ripple!") }
        findViewById<LinearLayout>(R.id.llLine).setOnClickListener { handleModeSelection("LINE", "Line Ribbon!") }

        // --- 上傳 ---
        findViewById<LinearLayout>(R.id.llUpload).setOnClickListener {
            // 指定支援的檔案類型
            pickMediaLauncher.launch(arrayOf("video/mp4", "image/gif"))
        }
    }

    private fun handleCustomMedia(uri: Uri) {
        try {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)

            val mimeType = contentResolver.getType(uri)
            val prefs = getSharedPreferences("WallpaperSettings", Context.MODE_PRIVATE)

            val mode = when {
                mimeType?.contains("video") == true -> "VIDEO"
                mimeType?.contains("gif") == true -> "GIF"
                else -> {
                    showToast("Unsupported format: $mimeType")
                    return
                }
            }

            prefs.edit().apply {
                putString("game_mode", mode)
                putString("custom_media_uri", uri.toString())
                apply()
            }

            showToast(":D Successfully Loaded $mode!")
            openWallpaperSettings()

        } catch (e: SecurityException) {
            e.printStackTrace()
            showToast("Security Error: This file source doesn't support persistable permissions.")
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(" :( Failed to load file: ${e.message}")
        }
    }

    private fun handleModeSelection(mode: String, toastMsg: String) {
        saveMode(mode)
        showToast(toastMsg)
        openWallpaperSettings()
    }

    private fun saveMode(mode: String) {
        val prefs = getSharedPreferences("WallpaperSettings", Context.MODE_PRIVATE)
        prefs.edit().putString("game_mode", mode).apply()
    }

    private fun openWallpaperSettings() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@MainActivity, DinoWallpaperService::class.java))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}