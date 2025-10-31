package com.justplay.coloringgame.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.toColorInt
import kotlin.math.abs
import kotlin.math.min

class ColoringView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    /**
     * 外框 Bitmap
     */
    private var outlineBitmap: Bitmap? = null

    /**
     * 繪圖區 Bitmap
     */
    private var regionIdMap: Bitmap? = null

    /**
     * 上色圖層的 Bitmap
     */
    private var colorLayer: Bitmap? = null

    /**
     * 上色圖層的畫布
     */
    private var colorLayerCanvas: Canvas? = null

    /**
     * 縮放、平移的疊加 Matrix
     */
    private val drawMatrix = Matrix()

    /**
     * [drawMatrix] 的反矩陣，用於進行像素座標的定位轉換
     */
    private val invDrawMatrix = Matrix()

    /**
     * 要被填充的顏色
     */
    private var selectedColor: Int = "#F94144".toColorInt()

    /**
     * 縮放感測器
     */
    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                drawMatrix.postScale(
                    detector.scaleFactor, detector.scaleFactor,
                    detector.focusX, detector.focusY
                )
                invalidate()
                return true
            }
        }
    )

    /**
     * 手勢感測器
     */
    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
                drawMatrix.postTranslate(-dx, -dy)
                invalidate()
                return true
            }
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                handleTapEvent(e.x, e.y)
                return true
            }
        }
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        fitCenter()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.concat(drawMatrix) // 在既有變化下再疊加
        colorLayer?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        canvas.drawBitmap(outlineBitmap!!, 0f, 0f, null) // 將填色部分繪製後再重新外框
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val scaleEvent = scaleDetector.onTouchEvent(event)
        val gestureEvent = gestureDetector.onTouchEvent(event)
        return scaleEvent || gestureEvent || super.onTouchEvent(event)
    }

    /**
     * 設定 BitMap
     * @param outline 外框圖片，圖片必須是黑框線加透明背景
     * @param regionId 繪圖區圖片，與 [outline] 大小需一致
     */
    fun setBitmaps(outline: Bitmap, regionId: Bitmap) {
        require(outline.width == regionId.width && outline.height == regionId.height) {
            "outline and regionIdMap must be same size"
        }
        outlineBitmap = outline.copy(Bitmap.Config.ARGB_8888, false)
        regionIdMap = regionId.copy(Bitmap.Config.ARGB_8888, false)
        colorLayer = createBitmap(outline.width, outline.height)
        colorLayerCanvas = Canvas(colorLayer!!)
        fitCenter()
        invalidate()
    }

    /**
     * 設定填充顏色
     */
    fun setSelectedColor(@ColorInt color: Int) {
        selectedColor = color
    }

    /**
     * 畫面置中
     */
    private fun fitCenter() {
        val bmp = outlineBitmap ?: return
        if (width == 0 || height == 0) return
        drawMatrix.reset()
        val s = min(width.toFloat() / bmp.width, height.toFloat() / bmp.height)
        drawMatrix.setScale(s, s)
        drawMatrix.postTranslate(
            (width - bmp.width * s) / 2f,
            (height - bmp.height * s) / 2f
        )
    }

    /**
     * View 上的觸控點反推成 BitMap 的像素座標
     * @param x 觸控點的 x 座標
     * @param y 觸控點的 y 座標
     */
    private fun tapPointToBitmapXY(x: Float, y: Float): Pair<Int, Int>? {
        val bmp = regionIdMap ?: return null
        /**
         * 計算出 drawMatrix 的反矩陣到 inverseMatrix
         * 如果 drawMatrix 不可逆，會回傳 false
         */
        if (!drawMatrix.invert(invDrawMatrix)) return null
        val pts = floatArrayOf(x, y)
        invDrawMatrix.mapPoints(pts) // 標出其像素座標
        val ix = pts[0].toInt()
        val iy = pts[1].toInt()
        if (ix !in 0 until bmp.width || iy !in 0 until bmp.height) return null
        return ix to iy
    }

    private fun filterAlphaRgb(c: Int) = c and 0x00FFFFFF

    private fun sameRGB(a: Int, b: Int, tol: Int = TOL): Boolean {
        val ar = (a ushr 16) and 0xFF; val ag = (a ushr 8) and 0xFF; val ab = a and 0xFF
        val br = (b ushr 16) and 0xFF; val bg = (b ushr 8) and 0xFF; val bb = b and 0xFF
        return  abs(ar - br) <= tol &&
                abs(ag - bg) <= tol &&
                abs(ab - bb) <= tol
    }

    /**
     * 若點到邊緣半透明像素，往附近找一個 alpha 夠高的代表色
     */
    private fun pickOpaqueNeighbor(idBmp: Bitmap, ix: Int, iy: Int): Int? {
        val w = idBmp.width
        val h = idBmp.height
        val base = idBmp[ix, iy]
        val baseRGB = filterAlphaRgb(base)
        val r = 2 // 搜尋半徑
        for (dy in -r..r) for (dx in -r..r) {
            val x = ix + dx; val y = iy + dy
            if (x !in 0 until w || y !in 0 until h) continue
            val c = idBmp[x, y]
            if (Color.alpha(c) >= MIN_ALPHA && sameRGB(filterAlphaRgb(c), baseRGB)) {
                return c or 0xFF000000.toInt() // 轉成不透明代表色
            }
        }
        return null
    }

    /**
     * 用  flood fill 演算法製作遮罩
     * @param outline 外框圖 Bitmap
     * @param seedX flood fill 起點 X 軸座標
     * @param seedY flood fill 起點 Y 軸座標
     * @return 遮罩圖與其邊界盒
     */
    private fun floodMaskFromOutline(
        outline: Bitmap, seedX: Int, seedY: Int
    ): Pair<Bitmap, Rect>? {
        val outlineWd = outline.width
        val outlineHt = outline.height

        /**
         * 拜訪過並且不為外框的像素
         */
        val visitedArray = BooleanArray(outlineWd * outlineHt)

        /**
         * 待處理邊界的填色佇列
         */
        val floodFillDeque = ArrayDeque<Int>()

        if (seedX !in 0 until outlineWd || seedY !in 0 until outlineHt) return null

        val pixelsArray = IntArray(outlineWd * outlineHt)
        outline.getPixels(pixelsArray, 0, outlineWd, 0, 0, outlineWd, outlineHt)

        /**
         * 近似亮度
         */
        fun approxLuma(color: Int): Int {
            val r = (color ushr 16) and 0xFF // ushr 無號右移，用以拆解顏色通道
            val g = (color ushr  8) and 0xFF
            val b =  color          and 0xFF
            return (299 * r + 587 * g + 114 * b) / 1000
        }

        /**
         * 是否為黑框線
         */
        fun isOutlineWall(ix: Int, iy: Int): Boolean {
            val pixelColor = pixelsArray[iy * outlineWd + ix]
            val colorLuma = (pixelColor ushr 24) and 0xFF
            if (colorLuma < LINE_ALPHA_MIN) return false
            return approxLuma(pixelColor) <= LINE_LUMA_MAX  // 黑線或很深的線
        }

        /**
         * 將 [[x], [y]] 位置推入 deque 中
         */
        fun dequePush(x: Int, y: Int) {
            val i = y * outlineWd + x
            // 尚未拜訪過，且並非是牆
            if (!visitedArray[i] && !isOutlineWall(x, y)) {
                visitedArray[i] = true
                floodFillDeque.add(i)
            }
        }

        if (isOutlineWall(seedX, seedY)) return null // 點在牆上不填

        dequePush(seedX, seedY)

        var minX = seedX; var maxX = seedX
        var minY = seedY; var maxY = seedY

        // BFS
        while (floodFillDeque.isNotEmpty()) {
            val i = floodFillDeque.removeFirst() // 將目前要擴張的像素推出
            val x = i % outlineWd
            val y = i / outlineWd

            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y

            if (x > 0)             dequePush(x - 1, y)
            if (x + 1 < outlineWd) dequePush(x + 1, y)
            if (y > 0)             dequePush(x, y - 1)
            if (y + 1 < outlineHt) dequePush(x, y + 1)
        }

        if (minX > maxX || minY > maxY) return null

        val bw = maxX - minX + 1
        val bh = maxY - minY + 1
        val mask = createBitmap(bw, bh)

        // 將 visited 區域寫入遮罩（alpha=255）
        val row = IntArray(bw)

        for (yy in 0 until bh) {
            val y = minY + yy
            for (xx in 0 until bw) {
                val x = minX + xx
                val alpha = if (visitedArray[y * outlineWd + x]) 0xFF else 0x00
                row[xx] = (alpha shl 24) // 將位元左移 24 位，將之移到 ALPHA 通道
            }
            mask.setPixels(row, 0, bw, 0, yy, bw, 1)
        }
        return mask to Rect(minX, minY, maxX + 1, maxY + 1)
    }

    /**
     * 處理點擊事件
     * @param x 觸控點的 x 座標
     * @param y 觸控點的 y 座標
     */
    private fun handleTapEvent(x: Float, y: Float) {
        val pos = tapPointToBitmapXY(x, y) ?: return
        val idBmp = regionIdMap ?: return
        val (ix, iy) = pos

        // 取得代表色：盡量選擇 alpha 高的像素；忽略 alpha 只看 RGB
        var seed = idBmp[ix, iy]
        if (Color.alpha(seed) < MIN_ALPHA) {
            val alt = pickOpaqueNeighbor(idBmp, ix, iy) ?: return
            seed = alt
        }
        // 背景/線條（全透明或近黑線）直接略過
        if (Color.alpha(seed) == 0) return
        // 用外框圖來產生真正的形狀遮罩
        val (mask, rect) = floodMaskFromOutline(outlineBitmap!!, ix, iy) ?: return
        val bw = rect.width(); val bh = rect.height()

        val colorBitmap = createColorBitmap(mask, selectedColor, bw, bh)

        // 疊回 colorLayer
        colorLayerCanvas?.drawBitmap(colorBitmap, rect.left.toFloat(), rect.top.toFloat(), null)
        invalidate()
    }

    private fun createColorBitmap(mask: Bitmap, color: Int, width: Int, height: Int): Bitmap {
        // 先畫「遮罩」，再用 SRC_IN 上色
        val patch = createBitmap(width, height)
        val c = Canvas(patch)
        val patchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = false
            isDither = false
        }
        // 先把「遮罩圖」畫上去
        patchPaint.xfermode = null
        c.drawBitmap(mask, 0f, 0f, patchPaint)
        // 再用 SRC_IN 畫一整塊顏色，只會留在遮罩覆蓋區域
        patchPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        patchPaint.color = color
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), patchPaint)

        return patch
    }

    companion object {
        private const val MIN_ALPHA = 200      // 視為有效區域的最小 alpha
        private const val TOL = 6
        private const val LINE_ALPHA_MIN = 8   // 視為黑線的最低不透明度
        private const val LINE_LUMA_MAX  = 24  // 視為黑線的最大亮度// RGB 容差 (0..255)
    }
}