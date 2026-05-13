package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.Canvas
import android.view.MotionEvent


class AdvancedLogic : IGameLogic {

    override fun loadResources(resources: Resources) {
        // 初始化資源
    }

    override fun updatePhysics(width: Int, height: Int) {
        // 物理計算
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {
        // 繪圖邏輯
    }

    override fun onTouch(event: MotionEvent) {
        // 觸控事件
    }

    override fun onSensorChanged(x: Float, y: Float) {
        // 感應傾斜
    }

    override fun release() {
        // 釋放資源
    }
}