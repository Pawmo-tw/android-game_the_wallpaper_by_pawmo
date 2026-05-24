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
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
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
        setupNavigation()   // 啟用分頁滑動動畫
        setupSettingsPanel() // 啟用設定面板監聽
    }

    private fun setupNavigation() {
        val wp = findViewById<View>(R.id.layoutWallpapers)
        val st = findViewById<View>(R.id.layoutSettings)
        st.visibility = View.INVISIBLE

        // 切換回桌布選單
        findViewById<ImageButton>(R.id.btnTabWallpapers).setOnClickListener {
            if (wp.visibility == View.VISIBLE) return@setOnClickListener

            val exactWidth = wp.width.toFloat()
            wp.translationX = -exactWidth
            wp.visibility = View.VISIBLE

            st.animate().translationX(exactWidth).setDuration(250).start()
            wp.animate().translationX(0f).setDuration(250).withEndAction {
                st.visibility = View.INVISIBLE
            }.start()
        }

        // 切換到自訂設定
        findViewById<ImageButton>(R.id.btnTabSettings).setOnClickListener {
            if (st.visibility == View.VISIBLE) return@setOnClickListener

            val exactWidth = wp.width.toFloat()
            st.translationX = exactWidth
            st.visibility = View.VISIBLE

            wp.animate().translationX(-exactWidth).setDuration(250).start()
            st.animate().translationX(0f).setDuration(250).withEndAction {
                wp.visibility = View.INVISIBLE
            }.start()
        }
    }

    // 自訂義面板元件監聽
    private fun setupSettingsPanel() {
        val tvFps = findViewById<TextView>(R.id.tvFpsLabel)
        val sbFps = findViewById<SeekBar>(R.id.sbFps)
        val vPrimaryPreview = findViewById<View>(R.id.vPrimaryColorPreview)
        val vSecondaryPreview = findViewById<View>(R.id.vSecondaryColorPreview)

        val prefs = getSharedPreferences("WallpaperSettings", Context.MODE_PRIVATE)

        // 初始化 FPS
        val savedFps = prefs.getInt("fps", 60)
        sbFps.progress = savedFps
        tvFps.text = "Frame Rate: $savedFps FPS"

        // 初始化顏色預覽外觀
        val primaryColor = prefs.getInt("primary_color", 0xFF34C759.toInt())
        val secondaryColor = prefs.getInt("secondary_color", 0xFFFF9500.toInt())
        updatePreviewCircle(vPrimaryPreview, primaryColor)
        updatePreviewCircle(vSecondaryPreview, secondaryColor)

        // 主色調色盤
        findViewById<Button>(R.id.btnPrimaryColor).setOnClickListener {
            val currentPrimary = prefs.getInt("primary_color", 0xFF34C759.toInt())
            showColorPickerDialog("primary_color", currentPrimary) { selectedColor ->
                prefs.edit().putInt("primary_color", selectedColor).apply()
                updatePreviewCircle(vPrimaryPreview, selectedColor)
                sendBroadcast(Intent("pawmo.yee.wallpaper.SETTING_CHANGED").apply { setPackage(packageName) })
            }
        }

        // 輔色調色盤
        findViewById<Button>(R.id.btnSecondaryColor).setOnClickListener {
            val currentSecondary = prefs.getInt("secondary_color", 0xFFFF9500.toInt())
            showColorPickerDialog("secondary_color", currentSecondary) { selectedColor ->
                prefs.edit().putInt("secondary_color", selectedColor).apply()
                updatePreviewCircle(vSecondaryPreview, selectedColor)
                sendBroadcast(Intent("pawmo.yee.wallpaper.SETTING_CHANGED").apply { setPackage(packageName) })
            }
        }

        // FPS
        sbFps.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                tvFps.text = "Frame Rate: $progress FPS"
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}

            override fun onStopTrackingTouch(sb: SeekBar?) {
                val currentFps = sb?.progress ?: 60
                prefs.edit().putInt("fps", currentFps).apply()
                sendBroadcast(Intent("pawmo.yee.wallpaper.SETTING_CHANGED").apply { setPackage(packageName) })
                showToast("updated to $currentFps fps")
            }
        })
    }

    // 預覽視窗
    private fun updatePreviewCircle(view: View, color: Int) {
        view.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(color)
            setStroke((1.5f * resources.displayMetrics.density).toInt(), 0xFFE5E5EA.toInt()) // 細緻灰邊框
        }
    }

    // 調色盤
    private fun showColorPickerDialog(title: String, currentColor: Int, onColorSelected: (Int) -> Unit) {
        val density = resources.displayMetrics.density

        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(currentColor, hsv)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1A1A1A.toInt())
            val p = (24 * density).toInt()
            setPadding(p, p, p, p)
        }

        // 標題
        val tvTitle = TextView(this).apply {
            text = title
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, (16 * density).toInt())
        }
        container.addView(tvTitle)
        val previewLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, (20 * density).toInt())
        }

        val colorPreviewBlock = View(this).apply {
            layoutParams = LinearLayout.LayoutParams((50 * density).toInt(), (50 * density).toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 8 * density
                setColor(currentColor)
                setStroke((1 * density).toInt(), 0x44FFFFFF)
            }
        }

        val tvHexCode = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins((16 * density).toInt(), 0, 0, 0)
            }
            text = String.format("#%06X", 0xFFFFFF and currentColor)
            setTextColor(0xFFE0E0E0.toInt())
            textSize = 16f
            setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
        }
        previewLayout.addView(colorPreviewBlock)
        previewLayout.addView(tvHexCode)
        container.addView(previewLayout)
        fun getSelectedColor(): Int = android.graphics.Color.HSVToColor(hsv)

        fun updateDialogPreview() {
            val updatedColor = getSelectedColor()
            (colorPreviewBlock.background as android.graphics.drawable.GradientDrawable).setColor(updatedColor)
            tvHexCode.text = String.format("#%06X", 0xFFFFFF and updatedColor)
        }

        //色相
        val tvHue = TextView(this).apply { text = "Hue"; setTextColor(0xFF888888.toInt()); textSize = 12f }
        val sbHue = SeekBar(this).apply {
            max = 360
            progress = hsv[0].toInt()
            val rainbow = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xFFFF0000.toInt(), 0xFFFFFF00.toInt(), 0xFF00FF00.toInt(), 0xFF00FFFF.toInt(), 0xFF0000FF.toInt(), 0xFFFF00FF.toInt(), 0xFFFF0000.toInt())
            ).apply { cornerRadius = 4 * density }
            background = rainbow
            setPadding((10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt())
        }

        //飽和度
        val tvSat = TextView(this).apply { text = "Saturation"; setTextColor(0xFF888888.toInt()); textSize = 12f; setPadding(0, (12 * density).toInt(), 0, 0) }
        val sbSat = SeekBar(this).apply {
            max = 100
            progress = (hsv[1] * 100).toInt()
            setPadding((10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt())
        }

        //亮度
        val tvVal = TextView(this).apply { text = "Brightness"; setTextColor(0xFF888888.toInt()); textSize = 12f; setPadding(0, (12 * density).toInt(), 0, 0) }
        val sbVal = SeekBar(this).apply {
            max = 100
            progress = (hsv[2] * 100).toInt()
            setPadding((10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt())
        }

        fun updateSeekBarBackgrounds() {
            val currentHue = hsv[0]

            //飽和度
            sbSat.background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(android.graphics.Color.HSVToColor(floatArrayOf(currentHue, 0f, 1f)), android.graphics.Color.HSVToColor(floatArrayOf(currentHue, 1f, 1f)))
            ).apply { cornerRadius = 4 * density }

            //亮度
            sbVal.background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xFF000000.toInt(), android.graphics.Color.HSVToColor(floatArrayOf(currentHue, 1f, 1f)))
            ).apply { cornerRadius = 4 * density }
        }

        updateSeekBarBackgrounds()

        //滑桿事件監聽
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                when (seekBar) {
                    sbHue -> hsv[0] = progress.toFloat()
                    sbSat -> hsv[1] = progress / 100f
                    sbVal -> hsv[2] = progress / 100f
                }
                updateSeekBarBackgrounds()
                updateDialogPreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        sbHue.setOnSeekBarChangeListener(listener)
        sbSat.setOnSeekBarChangeListener(listener)
        sbVal.setOnSeekBarChangeListener(listener)

        container.addView(tvHue)
        container.addView(sbHue)
        container.addView(tvSat)
        container.addView(sbSat)
        container.addView(tvVal)
        container.addView(sbVal)

        //視窗
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(container)
            .setPositiveButton("apply") { _, _ -> onColorSelected(getSelectedColor()) }
            .setNegativeButton("cancel", null)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.show()
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(0xFF34C759.toInt())
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF888888.toInt())
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
            try {
                startActivity(Intent().apply {
                    component = ComponentName("com.android.wallpaper.livepicker", "com.android.wallpaper.livepicker.LiveWallpaperPreview")
                    putExtra("android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT", comp)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
                return
            } catch (e: Exception) { e.printStackTrace() }

            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
                showToast("click「game the wallpaper」 pls")
                return
            } catch (e: Exception) { e.printStackTrace() }
        }

        try {
            startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, comp)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            } catch (any: Exception) { showToast("err :(") }
        }
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}