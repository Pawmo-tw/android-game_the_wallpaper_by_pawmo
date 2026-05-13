package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.random.Random

class Game2048Logic : IGameLogic {
    data class Tile(
        var value: Int,
        var r: Int, var c: Int,
        var oldR: Int = -1, var oldC: Int = -1,
        var isNew: Boolean = true
    )

    private val gridSize = 4
    private var tiles = mutableListOf<Tile>()
    private var score = 0

    private var isAnimating = false
    private var animProgress = 0f
    private val animSpeed = 0.15f

    private var startX = 0f
    private var startY = 0f
    private val minDistance = 100f

    private var canvasWidth = 0
    private var canvasHeight = 0
    private var cellSize = 0f
    private var gridLeft = 0f
    private var gridTop = 0f
    private val resetBtnRect = RectF()

    override fun loadResources(resources: Resources) {
        resetGame()
    }

    private fun resetGame() {
        tiles.clear()
        score = 0
        spawnNumber()
        spawnNumber()
    }

    private fun spawnNumber() {
        val occupied = tiles.map { it.r to it.c }.toSet()
        val empty = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (r to c !in occupied) empty.add(r to c)
            }
        }
        if (empty.isNotEmpty()) {
            val (r, c) = empty.random()
            tiles.add(Tile(value = if (Random.nextFloat() < 0.9) 2 else 4, r = r, c = c))
        }
    }

    override fun updatePhysics(width: Int, height: Int) {
        canvasWidth = width
        canvasHeight = height
        cellSize = width / 5.5f
        gridLeft = (width - cellSize * gridSize) / 2f
        gridTop = (height - cellSize * gridSize) / 2f

        val btnW = 300f
        resetBtnRect.set(
            canvasWidth / 2f - btnW / 2f,
            gridTop - 250f, // 放在分數上方一點
            canvasWidth / 2f + btnW / 2f,
            gridTop - 150f
        )

        if (isAnimating) {
            animProgress += animSpeed
            if (animProgress >= 1f) {
                isAnimating = false
                animProgress = 0f
                tiles.forEach { it.oldR = -1; it.oldC = -1; it.isNew = false }
            }
        }
    }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {
        canvas.drawColor(if (isNightMode) Color.parseColor("#121212") else Color.parseColor("#FAF8EF"))
        val paint = Paint().apply { isAntiAlias = true }

        paint.color = Color.parseColor("#8F7A66")
        canvas.drawRoundRect(resetBtnRect, 10f, 10f, paint)

        paint.color = Color.WHITE
        paint.textSize = 45f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        val btnBaseline = resetBtnRect.centerY() - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2
        canvas.drawText("RESTART", resetBtnRect.centerX(), btnBaseline, paint)

        // 繪製背景與網格
        paint.color = Color.parseColor("#BBADA0")
        val bgRect = RectF(gridLeft - 15, gridTop - 15, gridLeft + cellSize * gridSize + 15, gridTop + cellSize * gridSize + 15)
        canvas.drawRoundRect(bgRect, 25f, 25f, paint)

        paint.color = Color.parseColor("#CDC1B4")
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val x = gridLeft + c * cellSize + 10
                val y = gridTop + r * cellSize + 10
                canvas.drawRoundRect(RectF(x, y, x + cellSize - 20, y + cellSize - 20), 15f, 15f, paint)
            }
        }

        // 繪製方塊
        for (tile in tiles) {
            val drawR = if (isAnimating && tile.oldR != -1) tile.oldR + (tile.r - tile.oldR) * animProgress else tile.r.toFloat()
            val drawC = if (isAnimating && tile.oldC != -1) tile.oldC + (tile.c - tile.oldC) * animProgress else tile.c.toFloat()
            val scale = if (isAnimating && tile.isNew) animProgress else 1f

            drawValueTile(canvas, drawR, drawC, tile.value, scale, paint)
        }

        // 繪製分數
        paint.color = if (isNightMode) Color.WHITE else Color.parseColor("#776E65")
        paint.textSize = 70f
        canvas.drawText("SCORE: $score", canvasWidth / 2f, gridTop - 60f, paint)
    }

    private fun drawValueTile(canvas: Canvas, r: Float, c: Float, value: Int, scale: Float, paint: Paint) {
        val centerX = gridLeft + c * cellSize + cellSize / 2
        val centerY = gridTop + r * cellSize + cellSize / 2
        val s = (cellSize - 20) / 2 * scale
        val rect = RectF(centerX - s, centerY - s, centerX + s, centerY + s)

        paint.color = getTileColor(value)
        canvas.drawRoundRect(rect, 15f, 15f, paint)

        paint.color = if (value <= 4) Color.parseColor("#776E65") else Color.WHITE
        paint.textSize = (cellSize * 0.4f) * scale
        val baseline = centerY - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2
        canvas.drawText(value.toString(), centerX, baseline, paint)
    }

    override fun onTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                if (resetBtnRect.contains(event.x, event.y)) {
                    resetGame()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isAnimating) return
                val dx = event.x - startX
                val dy = event.y - startY
                if (abs(dx) > minDistance || abs(dy) > minDistance) {
                    if (abs(dx) > abs(dy)) {
                        if (dx > 0) move(0, 1) else move(0, -1)
                    } else {
                        if (dy > 0) move(1, 0) else move(-1, 0)
                    }
                }
            }
        }
    }

    private fun move(dr: Int, dc: Int) {
        var movedAny = false
        val merged = mutableSetOf<Tile>()
        tiles.forEach { it.oldR = it.r; it.oldC = it.c; it.isNew = false }

        var loop: Boolean
        do {
            loop = false
            val rowRange = if (dr > 0) (gridSize - 1 downTo 0) else (0 until gridSize)
            val colRange = if (dc > 0) (gridSize - 1 downTo 0) else (0 until gridSize)

            for (r in rowRange) {
                for (c in colRange) {
                    val tile = tiles.find { it.r == r && it.c == c } ?: continue
                    val tr = tile.r + dr
                    val tc = tile.c + dc

                    if (tr in 0 until gridSize && tc in 0 until gridSize) {
                        val target = tiles.find { it.r == tr && it.c == tc }
                        if (target == null) {
                            tile.r = tr; tile.c = tc; movedAny = true; loop = true
                        } else if (target.value == tile.value && target !in merged && tile !in merged) {
                            tile.r = tr; tile.c = tc; tile.value *= 2
                            score += tile.value; tiles.remove(target); merged.add(tile)
                            movedAny = true; loop = true
                        }
                    }
                }
            }
        } while (loop)

        if (movedAny) {
            isAnimating = true; animProgress = 0f; spawnNumber()
        }
    }

    private fun getTileColor(value: Int): Int = when (value) {
        2 -> Color.parseColor("#EEE4DA")
        4 -> Color.parseColor("#EDE0C8")
        8 -> Color.parseColor("#F2B179")
        16 -> Color.parseColor("#F59563")
        32 -> Color.parseColor("#F67C5F")
        64 -> Color.parseColor("#F65E3B")
        128 -> Color.parseColor("#EDCF72")
        256 -> Color.parseColor("#EDCC61")
        512 -> Color.parseColor("#EDC850")
        1024 -> Color.parseColor("#EDC53F")
        2048 -> Color.parseColor("#EDC22E")
        else -> Color.parseColor("#CDC1B4")
    }

    override fun onSensorChanged(x: Float, y: Float) {}
    override fun release() {}
}