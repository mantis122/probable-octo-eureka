package com.colorcottage.app

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

class MainActivity : Activity() {
    private lateinit var canvas: ColoringCanvas
    private var currentPage = ColoringPage.CIRCLE

    enum class ColoringPage(val title: String) {
        CIRCLE("Circle"),
        HOUSE("House"),
        FLOWER("Flower"),
        ROCKET("Rocket"),
        EGG("Dino Egg")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showGallery()
    }

    private fun showGallery() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 36, 24, 24)
            setBackgroundColor(Color.rgb(255, 248, 235))
        }

        root.addView(TextView(this).apply {
            text = "Color Cottage"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(70, 50, 40))
        })

        ColoringPage.values().forEach { page ->
            root.addView(Button(this).apply {
                text = page.title
                textSize = 22f
                setOnClickListener {
                    currentPage = page
                    showColoringPage()
                }
            })
        }

        setContentView(root)
    }

    private fun showColoringPage() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(255, 248, 235))
        }

        val title = TextView(this).apply {
            text = currentPage.title
            textSize = 26f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 8)
            setTextColor(Color.rgb(70, 50, 40))
        }

        canvas = ColoringCanvas(this, currentPage)

        val colors = intArrayOf(
            Color.RED,
            Color.BLUE,
            Color.GREEN,
            Color.YELLOW,
            Color.MAGENTA,
            Color.CYAN,
            Color.BLACK,
            Color.rgb(255, 140, 0),
            Color.rgb(150, 75, 0),
            Color.rgb(255, 170, 200),
            Color.rgb(160, 220, 120),
            Color.rgb(170, 130, 255)
        )

        val palette = LinearLayout(this).apply { gravity = Gravity.CENTER }
        colors.forEach { c ->
            palette.addView(Button(this).apply {
                text = ""
                setBackgroundColor(c)
                setOnClickListener { canvas.paintColor = c }
            }, LinearLayout.LayoutParams(70, 70).apply {
                setMargins(4, 4, 4, 4)
            })
        }

        val toolRow = LinearLayout(this).apply { gravity = Gravity.CENTER }

        val brush = Button(this).apply {
            text = "Brush"
            setOnClickListener { canvas.tool = ColoringCanvas.Tool.BRUSH }
        }

        val bucket = Button(this).apply {
            text = "Bucket"
            setOnClickListener { canvas.tool = ColoringCanvas.Tool.BUCKET }
        }

        val small = Button(this).apply {
            text = "12"
            setOnClickListener { canvas.brushSize = 12f }
        }

        val medium = Button(this).apply {
            text = "24"
            setOnClickListener { canvas.brushSize = 24f }
        }

        val large = Button(this).apply {
            text = "40"
            setOnClickListener { canvas.brushSize = 40f }
        }

        toolRow.addView(brush)
        toolRow.addView(bucket)
        toolRow.addView(small)
        toolRow.addView(medium)
        toolRow.addView(large)

        val actionRow = LinearLayout(this).apply { gravity = Gravity.CENTER }

        val back = Button(this).apply {
            text = "Pages"
            setOnClickListener {
                canvas.saveProgress()
                showGallery()
            }
        }

        val undo = Button(this).apply {
            text = "Undo"
            setOnClickListener { canvas.undo() }
        }

        val clear = Button(this).apply {
            text = "Clear"
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Clear page?")
                    .setMessage("Erase this coloring page?")
                    .setPositiveButton("Clear") { _, _ -> canvas.clearDrawing() }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        val save = Button(this).apply {
            text = "Save"
            setOnClickListener { saveCanvasToGallery() }
        }

        actionRow.addView(back)
        actionRow.addView(undo)
        actionRow.addView(clear)
        actionRow.addView(save)

        root.addView(title)
        root.addView(canvas, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))
        root.addView(palette)
        root.addView(toolRow)
        root.addView(actionRow)

        setContentView(root)
    }

    private fun saveCanvasToGallery() {
        canvas.saveProgress()

        val bitmap = canvas.exportBitmap()
        val filename = "ColorCottage_${currentPage.name}_${System.currentTimeMillis()}.png"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Color Cottage")
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (uri != null) {
            val stream: OutputStream? = contentResolver.openOutputStream(uri)
            stream?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            Toast.makeText(this, "Saved to gallery!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
        }
    }
}

class ColoringCanvas(
    private val appContext: Context,
    private val page: MainActivity.ColoringPage
) : View(appContext) {

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

        colorBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        colorCanvas = Canvas(colorBitmap!!)

        loadProgress()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        colorBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }

        currentPath?.let { path ->
            currentPaint?.let { paint ->
                canvas.drawPath(path, paint)
            }
        }

        drawPage(canvas)
    }

    private fun drawPage(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f

        when (page) {
            MainActivity.ColoringPage.CIRCLE -> {
                canvas.drawCircle(cx, cy, minOf(w, h) / 3f, linePaint)
                canvas.drawRect(32f, 32f, w - 32f, h - 32f, linePaint)
            }

            MainActivity.ColoringPage.HOUSE -> {
                val house = Path().apply {
                    moveTo(cx - 180, cy)
                    lineTo(cx, cy - 180)
                    lineTo(cx + 180, cy)
                    close()
                }
                canvas.drawPath(house, linePaint)
                canvas.drawRect(cx - 140, cy, cx + 140, cy + 220, linePaint)
                canvas.drawRect(cx - 45, cy + 90, cx + 45, cy + 220, linePaint)
                canvas.drawRect(cx - 110, cy + 40, cx - 50, cy + 100, linePaint)
                canvas.drawRect(cx + 50, cy + 40, cx + 110, cy + 100, linePaint)
            }

            MainActivity.ColoringPage.FLOWER -> {
                for (i in 0 until 8) {
                    val angle = Math.toRadians((i * 45).toDouble())
                    val px = cx + Math.cos(angle).toFloat() * 95f
                    val py = cy - 80 + Math.sin(angle).toFloat() * 95f
                    canvas.drawCircle(px, py, 60f, linePaint)
                }
                canvas.drawCircle(cx, cy - 80, 60f, linePaint)
                canvas.drawLine(cx, cy - 20, cx, cy + 250, linePaint)
                canvas.drawCircle(cx - 60, cy + 120, 45f, linePaint)
                canvas.drawCircle(cx + 60, cy + 160, 45f, linePaint)
            }

            MainActivity.ColoringPage.ROCKET -> {
                val rocket = Path().apply {
                    moveTo(cx, cy - 260)
                    lineTo(cx - 100, cy - 60)
                    lineTo(cx - 80, cy + 160)
                    lineTo(cx + 80, cy + 160)
                    lineTo(cx + 100, cy - 60)
                    close()
                }
                canvas.drawPath(rocket, linePaint)
                canvas.drawCircle(cx, cy - 80, 45f, linePaint)
                canvas.drawLine(cx - 80, cy + 160, cx - 150, cy + 260, linePaint)
                canvas.drawLine(cx + 80, cy + 160, cx + 150, cy + 260, linePaint)
            }

            MainActivity.ColoringPage.EGG -> {
                val egg = RectF(cx - 160, cy - 230, cx + 160, cy + 230)
                canvas.drawOval(egg, linePaint)
                canvas.drawCircle(cx - 70, cy - 40, 35f, linePaint)
                canvas.drawCircle(cx + 70, cy - 20, 35f, linePaint)
                canvas.drawCircle(cx, cy + 80, 45f, linePaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (tool) {
            Tool.BRUSH -> handleBrush(event, x, y)
            Tool.BUCKET -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    saveUndoState()
                    bucketFill(x.toInt(), y.toInt(), paintColor)
                    saveProgress()
                    invalidate()
                }
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

        val targetColor = bitmap.getPixel(startX, startY)

        if (targetColor == newColor) return
        if (isBlackLine(boundary.getPixel(startX, startY))) return

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
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return r < 80 && g < 80 && b < 80
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
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val exportCanvas = Canvas(bitmap)
        exportCanvas.drawColor(Color.WHITE)

        colorBitmap?.let {
            exportCanvas.drawBitmap(it, 0f, 0f, null)
        }

        drawPage(exportCanvas)
        return bitmap
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
