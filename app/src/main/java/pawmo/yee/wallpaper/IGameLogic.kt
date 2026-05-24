package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.Canvas
import android.view.MotionEvent

/**
 * 所有桌布遊戲邏輯的統一介面
 */
interface IGameLogic {
    var primaryColor: Int
    var secondaryColor: Int

    /**
     * 初始化與載入資源
     * @param resources 來自 Service 的 resources 物件
     */
    fun loadResources(resources: Resources)

    /**
     * 處理物理運算與座標更新
     * @param width 當前畫布寬度
     * @param height 當前畫布高度
     */
    fun updatePhysics(width: Int, height: Int)

    /**
     * 執行繪圖操作
     * @param canvas 畫布物件
     * @param isNightMode 是否為深色模式（可用於切換配色）
     */
    fun draw(canvas: Canvas, isNightMode: Boolean)

    /**
     * 處理觸控事件
     * 預設為空實作
     */
    fun onTouch(event: MotionEvent) {}

    /**
     * 接收感測器（加速度計）數據
     * @param x 左右傾斜 (正值為左傾, 負值為右傾)
     * @param y 上下傾斜 (正值為直立, 負值為倒立)
     * 預設為空實作
     */
    fun onSensorChanged(x: Float, y: Float) {}

    /**
     * 釋放資源
     */
    fun release()
}