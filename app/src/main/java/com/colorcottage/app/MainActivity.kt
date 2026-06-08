package com.colorcottage.app

import android.app.*
import android.os.*
import android.provider.MediaStore
import android.content.ContentValues
import android.graphics.*
import android.view.*
import android.widget.*
import java.io.OutputStream

class MainActivity : Activity() {
    private lateinit var canvas: ColoringCanvas
    private var currentPage = ColoringPage.CIRCLE

    enum class ColoringPage(val title: String) {
        CIRCLE("Circle"), HOUSE("House"), FLOWER("Flower"), ROCKET("Rocket"), EGG("Dino Egg")
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

        val title = TextView(this).apply {
            text = "Color Cottage"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(70, 50, 40))
        }

        root.addView(title)

        ColoringPage.values().forEach { page ->
            val button = Button(this).apply {
                text = page.title
                textSize = 22f
                setOnClickListener {
                    currentPage = page
                    showColoringPage()
                }
            }
            root.addView(button)
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
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
            Color.MAGENTA, Color.CYAN, Color.BLACK, Color.rgb(255, 140, 0),
            Color.rgb(150, 75, 0), Color.rgb(255, 170, 200)
        )

        val palette = LinearLayout(this).apply { gravity = Gravity.CENTER }
        colors.forEach { c ->
            val b = Button(this).apply {
                text = ""
                setBackgroundColor(c)
                setOnClickListener { canvas.paintColor = c }
            }
            palette.addView(b, LinearLayout.LayoutParams(76, 76).apply {
                setMargins(4, 4, 4, 4)
            })
        }

        val brushRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        listOf(12f, 24f, 40f).forEach { size ->
            val b = Button(this).apply {
                text = size.toInt().toString()
                setOnClickListener { canvas.brushSize = size }
            }
            brushRow.addView(b)
        }

        val actionRow = LinearLayout(this).apply { gravity = Gravity.CENTER }

        val back = Button(this).apply {
            text = "Pages"
            setOnClickListener { showGallery() }
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
            setOnClickListener {
                saveCanvasToGallery()
            }
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
        root.addView(brushRow)
        root.addView(actionRow)

        setContentView(root)
    }

    private fun saveCanvasToGallery() {
        val bitmap = canvas.exportBitmap()
        val filename = "ColorCottage_${System.currentTimeMillis()}.png"

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
    context: android.content.Context,
    private val page: MainActivity.ColoringPage
) : View(context) {
    var paintColor: Int = Color.RED
    var brushSize: Float = 24f

    private val strokes = mutableListOf<Pair<Path, Paint>>()
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

    init {
        setBackgroundColor(Color.WHITE)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawPage(canvas)

        for ((path, paint) in strokes) {
            canvas.drawPath(path, paint)
        }

        currentPath?.let { path ->
            currentPaint?.let { paint ->
                canvas.drawPath(path, paint)
            }
        }
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

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
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
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                currentPath?.lineTo(x, y)
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                val path = currentPath
                val paint = currentPaint
                if (path != null && paint != null) {
                    strokes.add(Pair(Path(path), Paint(paint)))
                }
                currentPath = null
                currentPaint = null
                invalidate()
                return true
            }
        }

        return true
    }

    fun undo() {
        if (strokes.isNotEmpty()) {
            strokes.removeAt(strokes.lastIndex)
            invalidate()
        }
    }

    fun clearDrawing() {
        strokes.clear()
        invalidate()
    }

    fun exportBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val exportCanvas = Canvas(bitmap)
        draw(exportCanvas)
        return bitmap
    }
}
