package com.colorcottage.app

import com.colorcottage.app.R
import android.app.*
import android.os.*
import android.provider.MediaStore
import android.content.*
import android.graphics.*
import android.view.*
import android.widget.*
import java.io.File
import java.io.OutputStream
import java.util.ArrayDeque
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat

class ColoringCanvas(
    private val appContext: Context,
    private val page: ColoringPage
) : View(appContext) {

    private var lineArtBitmap: Bitmap? = null
    private val imageRect = RectF()

    enum class Tool {
        BRUSH,
        BUCKET
    }

    var paintColor: Int = Color.RED
    var brushSize: Float = 24f
    var tool: Tool = Tool.BRUSH

    private var colorBitmap: Bitmap? = null
    private var colorCanvas: Canvas? = null

    private val undoStack = mutableListOf<Bitmap>()

    private var currentPath: Path? = null
    private var currentPaint: Paint? = null

    private val linePaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
    }

    init {
        setBackgroundColor(Color.WHITE)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w <= 0 || h <= 0) return
        zoom = 1f
        panX = 0f
        panY = 0f

        val rawLineArt = loadPageArtwork()
        lineArtBitmap = makeWhiteTransparent(rawLineArt)
        colorBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        colorBitmap?.eraseColor(Color.TRANSPARENT)
        colorCanvas = Canvas(colorBitmap!!)

        loadProgress()
    }

    // Zoom changes only the display; coloring and saved pixels stay in view coordinates.
    private var zoom = 1f
    private var panX = 0f
    private var panY = 0f
    private var gestureActive = false
    private var bucketTapPending = false
    private var bucketDownX = 0f
    private var bucketDownY = 0f
    private val bucketTapSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var previousSpan = 0f
    private var previousFocusX = 0f
    private var previousFocusY = 0f
    private var lastTapTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.translate(panX, panY)
        canvas.scale(zoom, zoom)
        colorBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        currentPath?.let { path -> currentPaint?.let { canvas.drawPath(path, it) } }
        drawPage(canvas)
        canvas.restore()
    }

private fun makeLineArtForExport(source: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)

    for (y in 0 until source.height) {
        for (x in 0 until source.width) {
            val pixel = source.getPixel(x, y)
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            if (r < 180 && g < 180 && b < 180) {
                output.setPixel(x, y, Color.BLACK)
            } else {
                output.setPixel(x, y, Color.TRANSPARENT)
            }
        }
    }

    return output
}

    private fun loadPageArtwork(): Bitmap =
        rasterizeAnimalArtwork(appContext, page.imageRes, page.vectorRasterWidth, page.vectorRasterHeight)

    private fun makeWhiteTransparent(source: Bitmap): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)

        for (y in 0 until output.height) {
            for (x in 0 until output.width) {
                val pixel = output.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                if (r > 120 && g > 120 && b > 120) {
                    output.setPixel(x, y, Color.TRANSPARENT)
               } else {
                output.setPixel(x, y, Color.BLACK)
                }
            }
        }

        return output
    }

    private fun drawPage(canvas: Canvas) {
    val bitmap = lineArtBitmap ?: return

    val viewW = width.toFloat()
    val viewH = height.toFloat()
    val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val viewRatio = viewW / viewH

    val drawW: Float
    val drawH: Float

    if (bitmapRatio > viewRatio) {
        drawW = viewW
        drawH = viewW / bitmapRatio
    } else {
        drawH = viewH
        drawW = viewH * bitmapRatio
    }

    val left = (viewW - drawW) / 2f
    val top = (viewH - drawH) / 2f

    imageRect.set(left, top, left + drawW, top + drawH)

    canvas.drawBitmap(bitmap, null, imageRect, null)
}

    private fun clampPan() {
        // Never allow the entire page to disappear off-screen.
        panX = panX.coerceIn(width * (1f - zoom), 0f)
        panY = panY.coerceIn(height * (1f - zoom), 0f)
    }

    private fun resetZoom() {
        zoom = 1f
        panX = 0f
        panY = 0f
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_DOWN) {
            parent?.requestDisallowInterceptTouchEvent(true)
            gestureActive = false
            bucketTapPending = false
            val now = event.eventTime
            val doubleTap = now - lastTapTime < 320L &&
                kotlin.math.abs(event.x - lastTapX) < 65f &&
                kotlin.math.abs(event.y - lastTapY) < 65f
            lastTapTime = now
            lastTapX = event.x
            lastTapY = event.y
            if (doubleTap) {
                currentPath = null
                currentPaint = null
                resetZoom()
                lastTapTime = 0L
                return true
            }
        }

        if (action == MotionEvent.ACTION_POINTER_DOWN && event.pointerCount >= 2) {
            // Discard an unfinished stroke rather than painting during a pinch.
            currentPath = null
            currentPaint = null
            bucketTapPending = false
            gestureActive = true
            val dx = event.getX(1) - event.getX(0)
            val dy = event.getY(1) - event.getY(0)
            previousSpan = kotlin.math.hypot(dx, dy).coerceAtLeast(1f)
            previousFocusX = (event.getX(0) + event.getX(1)) / 2f
            previousFocusY = (event.getY(0) + event.getY(1)) / 2f
            invalidate()
            return true
        }

        if (action == MotionEvent.ACTION_MOVE && event.pointerCount >= 2) {
            val dx = event.getX(1) - event.getX(0)
            val dy = event.getY(1) - event.getY(0)
            val span = kotlin.math.hypot(dx, dy).coerceAtLeast(1f)
            val fx = (event.getX(0) + event.getX(1)) / 2f
            val fy = (event.getY(0) + event.getY(1)) / 2f
            val newZoom = (zoom * span / previousSpan).coerceIn(1f, 3f)
            val localX = (previousFocusX - panX) / zoom
            val localY = (previousFocusY - panY) / zoom
            zoom = newZoom
            panX = fx - localX * zoom
            panY = fy - localY * zoom
            clampPan()
            previousSpan = span
            previousFocusX = fx
            previousFocusY = fy
            invalidate()
            return true
        }

        if (action == MotionEvent.ACTION_POINTER_UP) {
            bucketTapPending = false
            gestureActive = true
            return true
        }
        if (gestureActive) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                gestureActive = false
            }
            return true
        }

        val x = (event.x - panX) / zoom
        val y = (event.y - panY) / zoom
        when (tool) {
            Tool.BRUSH -> handleBrush(event, x, y)
            Tool.BUCKET -> when (action) {
                MotionEvent.ACTION_DOWN -> {
                    // Wait for finger release: a second finger may turn this into a pinch.
                    bucketDownX = event.x
                    bucketDownY = event.y
                    bucketTapPending = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (kotlin.math.hypot(event.x - bucketDownX, event.y - bucketDownY) > bucketTapSlop) {
                        bucketTapPending = false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (bucketTapPending &&
                        kotlin.math.hypot(event.x - bucketDownX, event.y - bucketDownY) <= bucketTapSlop &&
                        x >= 0f && x < width && y >= 0f && y < height) {
                        saveUndoState()
                        bucketFill(x.toInt(), y.toInt(), paintColor)
                        saveProgress()
                        invalidate()
                    }
                    bucketTapPending = false
                }
                MotionEvent.ACTION_CANCEL -> bucketTapPending = false
            }
        }
        return true
    }

    private fun handleBrush(event: MotionEvent, x: Float, y: Float) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                saveUndoState()

                currentPath = Path().apply { moveTo(x, y) }
                currentPaint = Paint().apply {
                    color = paintColor
                    strokeWidth = brushSize
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    isAntiAlias = true
                }
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                currentPath?.lineTo(x, y)
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                val path = currentPath
                val paint = currentPaint

                if (path != null && paint != null) {
                    colorCanvas?.drawPath(path, paint)
                }

                currentPath = null
                currentPaint = null
                saveProgress()
                invalidate()
            }
        }
    }

    private fun bucketFill(startX: Int, startY: Int, newColor: Int) {
    val bitmap = colorBitmap ?: return

    if (startX !in 0 until width || startY !in 0 until height) return

    val boundary = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val boundaryCanvas = Canvas(boundary)
    boundaryCanvas.drawColor(Color.WHITE)
    drawPage(boundaryCanvas)

    if (isBlackLine(boundary.getPixel(startX, startY))) return

    val targetColor = bitmap.getPixel(startX, startY)
    if (targetColor == newColor) return

    val queue: ArrayDeque<Point> = ArrayDeque()
    queue.add(Point(startX, startY))

    while (queue.isNotEmpty()) {
        val p = queue.removeFirst()

        if (p.x !in 0 until width || p.y !in 0 until height) continue
        if (isBlackLine(boundary.getPixel(p.x, p.y))) continue
        if (bitmap.getPixel(p.x, p.y) != targetColor) continue

        bitmap.setPixel(p.x, p.y, newColor)

        queue.add(Point(p.x + 1, p.y))
        queue.add(Point(p.x - 1, p.y))
        queue.add(Point(p.x, p.y + 1))
        queue.add(Point(p.x, p.y - 1))
    }
}

    private fun isBlackLine(pixel: Int): Boolean {
    if (Color.alpha(pixel) == 0) return false

    val r = Color.red(pixel)
    val g = Color.green(pixel)
    val b = Color.blue(pixel)

    return r < 100 && g < 100 && b < 100
}

    private fun saveUndoState() {
        val bitmap = colorBitmap ?: return
        undoStack.add(bitmap.copy(Bitmap.Config.ARGB_8888, true))

        if (undoStack.size > 20) {
            undoStack.removeAt(0)
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)
            colorBitmap = previous.copy(Bitmap.Config.ARGB_8888, true)
            colorCanvas = Canvas(colorBitmap!!)
            saveProgress()
            invalidate()
        }
    }

    fun clearDrawing() {
    saveUndoState()
    colorBitmap?.eraseColor(Color.TRANSPARENT)
    saveProgress()
    invalidate()
}

fun exportBitmap(): Bitmap {
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val exportCanvas = Canvas(output)

    exportCanvas.drawColor(Color.WHITE)

    colorBitmap?.let { colors ->
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = colors.getPixel(x, y)

                if (Color.alpha(pixel) == 255) {
                    output.setPixel(
                        x,
                        y,
                        Color.rgb(
                            Color.red(pixel),
                            Color.green(pixel),
                            Color.blue(pixel)
                        )
                    )
                }
            }
        }
    }

val rawLineArt = loadPageArtwork()
val exportLineArt = makeLineArtForExport(rawLineArt)

exportCanvas.drawBitmap(
    exportLineArt,
    null,
    imageRect,
    null
)

    return output
}

    fun saveProgress() {
        val bitmap = colorBitmap ?: return
        val file = progressFile()

        file.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun loadProgress() {
        val file = progressFile()

        if (file.exists()) {
            val saved = BitmapFactory.decodeFile(file.absolutePath)
            if (saved != null) {
                val scaled = Bitmap.createScaledBitmap(saved, width, height, true)
                colorCanvas?.drawBitmap(scaled, 0f, 0f, null)
            }
        }
    }

    private fun progressFile(): File {
        return File(appContext.filesDir, "progress_${page.name}.png")
    }
}
