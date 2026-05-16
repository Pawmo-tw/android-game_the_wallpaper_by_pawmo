package pawmo.yee.wallpaper

import android.content.res.Resources
import android.graphics.*
import android.view.MotionEvent
import kotlin.math.*

class CrossyLogic : IGameLogic {
    enum class TileType { GRASS, ROAD }
    data class Entity(var x: Float, var speed: Float, var type: Int)

    data class Lane(
        val type: TileType,
        val entities: MutableList<Entity> = mutableListOf(),
        val direction: Int = if (Math.random() > 0.5) 1 else -1,
        val seed: Long = (Math.random() * 1000000).toLong()
    )

    private var canvasWidth = 0
    private var canvasHeight = 0
    private var playerX = 5
    private var playerY = 0f
    private var targetX = 5f
    private var targetY = 0f
    private var isDead = false
    private var score = 0

    private val lanes = mutableListOf<Lane>()
    private val mapWidth = 15
    private var startTouchX = 0f
    private var startTouchY = 0f

    private val focalLength = 18f
    private val floorDrawRange = 110
    val curviness = 2
    override fun loadResources(resources: Resources) { resetGame() }

    private fun resetGame() {
        lanes.clear()
        repeat(10) { lanes.add(Lane(TileType.GRASS)) }
        repeat(130) { addLane() }
        playerX = 5; targetX = 5f; playerY = 0f; targetY = 0f; score = 0; isDead = false
    }

    private fun getCurveOffset(px: Float, cx: Float): Float {
        val dist = px - cx
        return dist * dist * 0.0002f
    }

    private fun addLane() {
        val type = if (Math.random() > 0.45) TileType.ROAD else TileType.GRASS
        val lane = Lane(type)
        if (type == TileType.ROAD) {
            repeat((2..3).random()) {
                lane.entities.add(Entity(Math.random().toFloat() * 10, 0.012f + (Math.random() * 0.015f).toFloat(), (0..1).random()))
            }
        } else {
            val obstacleMap = BooleanArray(mapWidth) { false }
            repeat((2..5).random()) { obstacleMap[(0 until mapWidth).random()] = true }
            obstacleMap.forEachIndexed { x, hasObstacle ->
                if (hasObstacle) {
                    val isConnected = (x > 0 && obstacleMap[x-1]) || (x < mapWidth-1 && obstacleMap[x+1])
                    lane.entities.add(Entity(x.toFloat(), 0f, if (isConnected) 3 else 2))
                }
            }
        }
        lanes.add(lane)
    }

    override fun updatePhysics(width: Int, height: Int) {
        canvasWidth = width; canvasHeight = height
        if (isDead) return
        if (abs(playerY - targetY) > 0.01f) playerY += (targetY - playerY) * 0.16f
        if (abs(playerX - targetX) > 0.01f) targetX += (playerX.toFloat() - targetX) * 0.16f

        lanes.forEachIndexed { index, lane ->
            if (lane.type == TileType.ROAD) {
                lane.entities.forEach { car ->
                    car.x += car.speed * lane.direction
                    if (car.x > 12f) car.x = -2f else if (car.x < -2f) car.x = 12f
                    if (index == targetY.toInt() && abs(car.x - targetX) < 0.45f) isDead = true
                }
            }
        }
    }

    private val gamePaint = Paint().apply { isAntiAlias = true }

    override fun draw(canvas: Canvas, isNightMode: Boolean) {
        val cx = canvasWidth / 2f
        val cy = canvasHeight * 0.75f
        val skyPaint = Paint().apply {
            isAntiAlias = true

            val skyColors = if (isNightMode) {
                intArrayOf(
                    Color.parseColor("#0a001a"),
                    Color.parseColor("#31004a"),
                    Color.parseColor("#7b2ff7")
                )
            } else {
                intArrayOf(
                    Color.parseColor("#f39c12"),
                    Color.parseColor("#f1c40f"),
                    Color.parseColor("#fff9c4")
                )
            }
            shader = LinearGradient(0f, 0f, 0f, canvasHeight.toFloat(),
                skyColors, floatArrayOf(0f, 0.4f, 0.8f), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), skyPaint)
        val bs = canvasWidth / 7.5f
        val renderDistance = 45
        for (i in (playerY.toInt() + renderDistance) downTo (playerY.toInt() - 5)) {
            if (i < 0 || i >= lanes.size) continue
            drawTexturedLane(canvas, cx, cy, i - playerY, mapWidth.toFloat(), bs, lanes[i], gamePaint, isNightMode, renderDistance)
        }

        for (i in (playerY.toInt() + 30) downTo (playerY.toInt() - 2)) {
            if (i < 0 || i >= lanes.size) continue
            val lane = lanes[i]
            val ry = i - playerY
            val entities = lane.entities.toMutableList()
            val isPlayerHere = (i == targetY.toInt())

            entities.sortBy { it.x }
            var playerDrawn = false
            var skipCount = 0

            for (idx in entities.indices) {
                if (skipCount > 0) { skipCount--; continue }
                val e = entities[idx]

                if ((e.type == 0 || e.type == 1) && lane.type == TileType.GRASS) continue

                if (isPlayerHere && !playerDrawn && targetX < e.x) {
                    drawFancyPlayer(canvas, cx, cy, bs, gamePaint)
                    playerDrawn = true
                }

                if (e.type == 3) {
                    var width = 1
                    while (idx + width < entities.size && entities[idx + width].type == 3 && entities[idx + width].x == e.x + width) width++
                    drawBigRock(canvas, cx, cy, e.x, ry, width.toFloat(), bs, gamePaint)
                    skipCount = width - 1
                } else {
                    drawDetailedEntity(canvas, cx, cy, e, ry, bs, gamePaint, cx)
                }
            }
            if (isPlayerHere && !playerDrawn) drawFancyPlayer(canvas, cx, cy, bs, gamePaint)
        }
        drawUI(canvas)
    }

    private fun drawTexturedLane(
        canvas: Canvas, cx: Float, cy: Float, ry: Float, w: Float, s: Float,
        lane: Lane, paint: Paint, isNightMode: Boolean, maxDist: Int,
    ) {
        // 取得當前行與下一行的垂直位置
        val baseY0 = getPY(ry, cy, s)
        val baseY1 = getPY(ry + 1, cy, s) - 1f

        // 如果超出螢幕範圍則不繪製
        if (baseY1 > baseY0 || baseY0 < 0) return

        // --- 計算淡出 Alpha ---
        // 當 ry 超過 maxDist 的一半時開始線性淡出
        val fadeStart = maxDist * 0.5f
        val alphaPercent = 1f - ((ry - fadeStart) / (maxDist - fadeStart)).coerceIn(0f, 1f)
        val finalAlpha = (alphaPercent * 255).toInt()

        if (finalAlpha <= 0) return

        // 計算四個角落的投影位置
        val xL0 = getPX(0f, ry, cx, s); val xR0 = getPX(w, ry, cx, s)
        val xL1 = getPX(0f, ry + 1, cx, s); val xR1 = getPX(w, ry + 1, cx, s)
        val xM0 = (xL0 + xR0) / 2f; val xM1 = (xL1 + xR1) / 2f

        // 建立地板路徑，並套用曲率
        val path = Path().apply {
            val offL1 = getCurveOffset(xL1, cx) * curviness
            moveTo(xL1, baseY1 + offL1)

            val offM1 = getCurveOffset(xM1, cx) * curviness
            val offR1 = getCurveOffset(xR1, cx) * curviness
            quadTo(xM1, baseY1 + offM1, xR1, baseY1 + offR1)

            val offR0 = getCurveOffset(xR0, cx) * curviness
            lineTo(xR0, baseY0 + offR0)

            val offM0 = getCurveOffset(xM0, cx) * curviness
            val offL0 = getCurveOffset(xL0, cx) * curviness
            quadTo(xM0, baseY0 + offM0, xL0, baseY0 + offL0)

            close()
        }

        // 設定基礎底色
        val baseColorStr = if (lane.type == TileType.ROAD) {
            if(isNightMode) "#1e293b" else "#334155"
        } else {
            if(isNightMode) "#064e3b" else "#22c55e"
        }

        val colorInt = Color.parseColor(baseColorStr)

        paint.color = Color.argb(120, Color.red(colorInt), Color.green(colorInt), Color.blue(colorInt))
        paint.alpha = ((finalAlpha / 150f) * 120).toInt()

        canvas.drawPath(path, paint)

        // 繪製細節
        if (lane.type == TileType.GRASS) {
            paint.color = Color.argb((finalAlpha * 0.2f).toInt(), 255, 255, 255)
            val random = java.util.Random(lane.seed)
            repeat(8) {
                val gx = random.nextFloat() * w
                val gpx = getPX(gx, ry + 0.5f, cx, s)
                val gpy = getPY(ry + 0.5f, cy, s) + getCurveOffset(gpx, cx) * curviness
                canvas.drawRect(gpx, gpy, gpx + 4f, gpy - 8f * getScale(ry), paint)
            }
        } else {
            paint.color = Color.argb((finalAlpha * 0.4f).toInt(), 255, 255, 255)
            val mx = getPX(w/2, ry + 0.5f, cx, s)
            val my = getPY(ry + 0.5f, cy, s) + getCurveOffset(mx, cx) * curviness
            canvas.drawRect(mx - 2f, my - 5f, mx + 2f, my + 5f, paint)
        }

        // 繪製格線邊框（增加立體感）
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.argb((finalAlpha * 0.1f).toInt(), 0, 0, 0)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawContactShadow(canvas: Canvas, px: Float, py: Float, w: Float, s: Float) {
        val shadowPaint = Paint().apply { color = Color.argb(60, 0, 0, 0); isAntiAlias = true }
        val sw = w * 0.8f
        val sh = s * 0.2f
        canvas.drawOval(px - sw/2, py - sh/2, px + sw/2, py + sh/2, shadowPaint)
    }

    private fun drawDetailedEntity(canvas: Canvas, cx: Float, cy: Float, e: Entity, ry: Float, s: Float, paint: Paint, centerX: Float) {
        val sc = getScale(ry)
        val px = getPX(e.x + 0.5f, ry, cx, s)
        val py = getPY(ry, cy, s) + getCurveOffset(px, cx) * 2.2f

        drawContactShadow(canvas, px, py, s * sc, s * sc)

        if (e.type == 2) { // 樹
            drawFancyBlock(canvas, px, py, 0f, s * 0.25f * sc, s * 0.7f * sc, Color.parseColor("#451a03"), paint, cx)
            drawFancyBlock(canvas, px, py, s * 0.5f * sc, s * 0.8f * sc, s * 0.8f * sc, Color.parseColor("#15803d"), paint, cx)
        } else { // 車
            val col = Color.parseColor(if(e.type == 0) "#b91c1c" else "#1d4ed8")
            drawFancyBlock(canvas, px, py, 0f, s * 0.9f * sc, s * 0.4f * sc, col, paint, cx)
            // 車窗
            drawFancyBlock(canvas, px, py, s * 0.35f * sc, s * 0.6f * sc, s * 0.25f * sc, Color.parseColor("#94a3b8"), paint, cx)
        }
    }

    private fun drawBigRock(canvas: Canvas, cx: Float, cy: Float, ix: Float, ry: Float, w: Float, s: Float, paint: Paint) {
        val sc = getScale(ry)
        val px = getPX(ix + w / 2f, ry, cx, s)
        val py = getPY(ry, cy, s) + getCurveOffset(px, cx) * 2.2f
        drawContactShadow(canvas, px, py, s * w * sc, s * sc)
        drawFancyBlock(canvas, px, py, 0f, s * w * 0.95f * sc, s * 0.55f * sc, Color.parseColor("#64748b"), paint, cx)
    }

    private fun drawFancyPlayer(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        val ry = targetY - playerY
        val sc = getScale(ry)
        val px = getPX(targetX + 0.5f, ry, cx, s)


        val curviness = 3.5f
        val py = getPY(ry, cy, s) + getCurveOffset(px, cx) * curviness


        val jump = abs(sin((playerY % 1.0) * PI)).toFloat() * (s * 0.6f) * sc
        val bodyY = py - jump


        val shadowAlpha = (60 * (1f - (jump / (s * sc)).coerceIn(0f, 1f))).toInt()
        paint.color = Color.argb(shadowAlpha, 0, 0, 0)
        canvas.drawOval(px - s*0.35f*sc, py - s*0.1f*sc, px + s*0.35f*sc, py + s*0.1f*sc, paint)

        val w = s * 0.55f * sc
        val h = s * 0.6f * sc


        drawFancyBlock(canvas, px, bodyY, 0f, w, h, Color.parseColor("#2980b9"), paint, cx)


        val bagW = w * 0.8f
        val bagH = h * 0.7f

        drawFancyBlock(canvas, px, bodyY, h * 0.15f, bagW, bagH, Color.parseColor("#d35400"), paint, cx)


        val headSize = w * 0.75f
        drawFancyBlock(canvas, px, bodyY, h, headSize, headSize, Color.parseColor("#f3e5ab"), paint, cx)

        val capSize = headSize * 1.1f
        val capHeight = headSize * 0.4f
        drawFancyBlock(canvas, px, bodyY, h + headSize * 0.7f, capSize, capHeight, Color.parseColor("#e74c3c"), paint, cx)

        val brimW = capSize * 1.2f
        val brimH = capSize * 0.15f
        drawFancyBlock(canvas, px, bodyY, h + headSize * 0.75f, brimW, brimH, Color.parseColor("#c0392b"), paint, cx)
    }

    private fun drawFancyBlock(canvas: Canvas, px: Float, py: Float, z: Float, w: Float, h: Float, col: Int, paint: Paint, cx: Float) {
        val left = px - w / 2; val right = px + w / 2
        val curviness = 2f
        val offMid = getCurveOffset(px, cx); val offL = getCurveOffset(left, cx); val offR = getCurveOffset(right, cx)

        val baseY = py - (offMid * curviness) - z
        val botY_L = baseY + (offL * curviness); val botY_R = baseY + (offR * curviness); val botY_M = py - z
        val topY_L = botY_L - h; val topY_R = botY_R - h; val topY_M = botY_M - h


        val frontPath = Path().apply {
            moveTo(left, topY_L); lineTo(right, topY_R); lineTo(right, botY_R)
            quadTo(px, botY_M, left, botY_L); close()
        }
        val gradient = LinearGradient(0f, topY_M, 0f, botY_M, col, darkenColor(col, 0.7f), Shader.TileMode.CLAMP)
        paint.shader = gradient
        canvas.drawPath(frontPath, paint)
        paint.shader = null


        val thickness = h * 0.15f; val slant = (px - cx) * 0.08f
        val topPath = Path().apply {
            moveTo(left, topY_L); lineTo(right, topY_R)
            lineTo(right - slant, topY_R - thickness); lineTo(left - slant, topY_L - thickness); close()
        }
        paint.color = lightenColor(col, 1.2f)
        canvas.drawPath(topPath, paint)


        if (abs(px - cx) > 10f) {
            val sidePath = Path()
            if (px > cx) {
                sidePath.moveTo(left, topY_L); sidePath.lineTo(left - slant, topY_L - thickness)
                sidePath.lineTo(left - slant, botY_L - thickness); sidePath.lineTo(left, botY_L)
            } else {
                sidePath.moveTo(right, topY_R); sidePath.lineTo(right - slant, topY_R - thickness)
                sidePath.lineTo(right - slant, botY_R - thickness); sidePath.lineTo(right, botY_R)
            }
            paint.color = darkenColor(col, 0.5f)
            canvas.drawPath(sidePath, paint)
        }
    }

    private fun lightenColor(c: Int, f: Float) = Color.argb(255, min(255, (Color.red(c)*f).toInt()), min(255, (Color.green(c)*f).toInt()), min(255, (Color.blue(c)*f).toInt()))
    private fun darkenColor(c: Int, f: Float) = Color.argb(255, (Color.red(c)*f).toInt(), (Color.green(c)*f).toInt(), (Color.blue(c)*f).toInt())
    private fun getScale(ry: Float) = focalLength / (focalLength + ry + 0.5f)
    private fun getPX(ix: Float, ry: Float, cx: Float, s: Float) = cx + (ix - mapWidth/2f) * s * 1.35f * getScale(ry)

    private fun getPY(ry: Float, cy: Float, s: Float) = cy - (ry * s * 0.75f) * getScale(ry)

    private fun drawUI(canvas: Canvas) {
        val p = Paint().apply { isAntiAlias = true; color = Color.WHITE; textSize = 60f; typeface = Typeface.DEFAULT_BOLD; setShadowLayer(8f, 0f, 4f, Color.BLACK) }
        canvas.drawText("SCORE: $score", 60f, 120f, p)
        if (isDead) {
            p.color = Color.argb(180, 0, 0, 0); canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), p)
            p.textAlign = Paint.Align.CENTER; p.textSize = 100f; p.color = Color.WHITE; p.setShadowLayer(0f, 0f, 0f, 0)
            canvas.drawText("GAME OVER", canvasWidth/2f, canvasHeight/2f - 40f, p)
            p.textSize = 50f; canvas.drawText("TAP TO RESTART", canvasWidth/2f, canvasHeight/2f + 80f, p)
        }
    }

    override fun onTouch(event: MotionEvent) {
        if (isDead) { if (event.action == MotionEvent.ACTION_DOWN) resetGame(); return }
        if (event.action == MotionEvent.ACTION_DOWN) { startTouchX = event.x; startTouchY = event.y }
        else if (event.action == MotionEvent.ACTION_UP) {
            val dx = event.x - startTouchX; val dy = event.y - startTouchY
            if (abs(dx) < 60 && abs(dy) < 60) tryMove(0, 1)
            else if (abs(dx) > abs(dy)) tryMove(if(dx > 0) 1 else -1, 0)
            else if (dy < -60) tryMove(0, 1)
        }
    }

    private fun tryMove(dx: Int, dy: Int) {
        val nX = playerX + dx; val nY = (targetY + dy).toInt()
        if (nX in 0 until mapWidth && nY < lanes.size) {
            if (lanes[nY].entities.none { (it.type == 2 || it.type == 3) && it.x.toInt() == nX }) {
                playerX = nX; targetY = nY.toFloat()
                if (dy > 0) { score++; if (lanes.size - targetY < 60) repeat(20) { addLane() } }
            }
        }
    }
    override fun onSensorChanged(x: Float, y: Float) {}
    override fun release() {}
}