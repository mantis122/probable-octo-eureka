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

class MainActivity : Activity() {
    private lateinit var canvas: ColoringCanvas
    private var currentPage = ColoringPage.PUPPY
    private var selectedColor: Int = Color.RED
    private val paletteButtons = mutableListOf<Button>()


enum class ColoringPage(val title: String, val imageRes: Int) {
    PUPPY("Puppy", R.drawable.animal_puppy),
    KITTEN("Kitten", R.drawable.animal_kitten),
    BUNNY("Bunny", R.drawable.animal_bunny),
    DUCKLING("Duckling", R.drawable.animal_duckling),
    PONY("Pony", R.drawable.animal_pony),
    DINOSAUR("Baby Dinosaur", R.drawable.animal_dinosaur),

    LAMB("Lamb", R.drawable.animal_lamb),
    PIGLET("Piglet", R.drawable.animal_piglet),
    CHICK("Chick", R.drawable.animal_chick),
    CALF("Calf", R.drawable.animal_calf),

    KOALA("Koala Joey", R.drawable.animal_koala),
    OTTER("Otter Pup", R.drawable.animal_otter),
    HEDGEHOG("Hedgehog", R.drawable.animal_hedgehog),
    RACCOON("Raccoon Kit", R.drawable.animal_raccoon),
    FOX("Fox Cub", R.drawable.animal_fox),
    BEAR("Bear Cub", R.drawable.animal_bear),
    TURTLE("Turtle", R.drawable.animal_turtle),
    PANDA("Panda",R.drawable.animal_panda)
}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showGallery()
    }

private fun isCompleted(page: ColoringPage): Boolean {
    return getSharedPreferences("color_cottage_progress", MODE_PRIVATE)
        .getBoolean("completed_${page.name}", false)
}

private fun setCompleted(page: ColoringPage, completed: Boolean) {
    getSharedPreferences("color_cottage_progress", MODE_PRIVATE)
        .edit()
        .putBoolean("completed_${page.name}", completed)
        .apply()
}

private fun showStickerReward(page: ColoringPage) {
    AlertDialog.Builder(this)
        .setTitle("Sticker earned!")
        .setMessage("You earned a ${page.title} sticker!")
        .setPositiveButton("Yay!", null)
        .show()
}

private fun showStickerGallery() {
    val scroll = ScrollView(this)

    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(24, 36, 24, 120)
        setBackgroundColor(Color.rgb(255, 248, 235))
    }

    root.addView(TextView(this).apply {
        text = "🏆 My Stickers"
        textSize = 30f
        gravity = Gravity.CENTER
        setTextColor(Color.rgb(70, 50, 40))
        setPadding(0, 0, 0, 24)
    })

    val earnedPages = ColoringPage.values().filter { isCompleted(it) }

    if (earnedPages.isEmpty()) {
        root.addView(TextView(this).apply {
            text = "Complete a page to earn stickers!"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(120, 100, 90))
            setPadding(0, 24, 0, 24)
        })
    } else {
        earnedPages.forEach { page ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(24, 24, 24, 24)
                setBackgroundColor(Color.WHITE)
                elevation = 8f
            }

            val stickerImage = ImageView(this).apply {
                setImageBitmap(makeGalleryThumbnail(page))
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                setBackgroundColor(Color.rgb(245, 245, 245))
                setPadding(8, 8, 8, 8)
            }

            card.addView(stickerImage, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                360
            ))

            card.addView(TextView(this).apply {
                text = "🏆 ${page.title} Sticker"
                textSize = 24f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(70, 120, 80))
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, 16, 0, 0)
            })

            root.addView(card, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 28)
            })
        }
    }

    root.addView(Button(this).apply {
        text = "Back to Pages"
        textSize = 20f
        setOnClickListener { showGallery() }
    })

    scroll.addView(root)
    setContentView(scroll)
}


private fun updatePaletteSelection() {
    paletteButtons.forEach { button ->
        val color = button.tag as Int

        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)

            if (color == selectedColor) {
    setStroke(
        8,
        if (color == Color.BLACK) Color.WHITE else Color.BLACK
    )
} else {
                setStroke(2, Color.LTGRAY)
            }
        }

        button.background = drawable
    }
}

private fun makeColorButton(color: Int): Button {
    return Button(this).apply {
        text = ""
        tag = color

        setOnClickListener {
            selectedColor = color
            canvas.paintColor = color
            updatePaletteSelection()
        }

        paletteButtons.add(this)
    }
}

private fun makeGalleryThumbnail(page: ColoringPage): Bitmap {
    return BitmapFactory.decodeResource(resources, page.imageRes)
}
private fun makeWhiteTransparent(source: Bitmap): Bitmap {
    val output = source.copy(Bitmap.Config.ARGB_8888, true)

    for (y in 0 until output.height) {
        for (x in 0 until output.width) {
            val pixel = output.getPixel(x, y)
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            if (r > 220 && g > 220 && b > 220) {
                output.setPixel(x, y, Color.TRANSPARENT)
            } else {
                output.setPixel(x, y, Color.BLACK)
            }
        }
    }

    return output
}    

    private fun showGallery() {
    val scroll = ScrollView(this)



    val root = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(24, 36, 24, 120)
    setBackgroundColor(Color.rgb(255, 248, 235))
    }

    root.addView(TextView(this).apply {
        text = "Color Cottage"
        textSize = 28f
        gravity = Gravity.CENTER
        setTextColor(Color.rgb(70, 50, 40))
        setPadding(0, 0, 0, 24)
    })

val startedCount = ColoringPage.values().count { page ->
    File(filesDir, "progress_${page.name}.png").exists()
}

val completedCount = ColoringPage.values().count { page ->
    isCompleted(page)
}

val totalCount = ColoringPage.values().size

root.addView(TextView(this).apply {
    text = "🎨 $startedCount started   🏆 $completedCount of $totalCount completed"
    textSize = 18f
    gravity = Gravity.CENTER
    setTextColor(Color.rgb(120, 100, 90))
    setPadding(0, 0, 0, 28)
})

val progressPercent =
    if (totalCount == 0) 0
    else (completedCount * 100 / totalCount)

root.addView(ProgressBar(
    this,
    null,
    android.R.attr.progressBarStyleHorizontal
).apply {
    max = 100
    progress = progressPercent
}, LinearLayout.LayoutParams(
    LinearLayout.LayoutParams.MATCH_PARENT,
    LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
    setMargins(24, 0, 24, 12)
})

root.addView(TextView(this).apply {
    text = "$completedCount / $totalCount Animals Collected"
    textSize = 18f
    gravity = Gravity.CENTER
    setTextColor(Color.rgb(70, 120, 80))
    setPadding(0, 0, 0, 24)
})

root.addView(Button(this).apply {
    text = "🏆 Stickers"
    textSize = 20f
    setOnClickListener {
        showStickerGallery()
    }
}, LinearLayout.LayoutParams(
    LinearLayout.LayoutParams.MATCH_PARENT,
    LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
    setMargins(0, 0, 0, 28)
})

    ColoringPage.values().forEach { page ->
        val completed = isCompleted(page)

val card = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    gravity = Gravity.CENTER
    setPadding(24, 24, 24, 24)

    setBackgroundColor(
        if (completed)
            Color.rgb(225, 250, 225)
        else
            Color.WHITE
    )

    elevation = 8f

    setOnClickListener {
        currentPage = page
        showColoringPage()
    }
}

        val thumbnail = ImageView(this).apply {
    setImageBitmap(makeGalleryThumbnail(page))
    scaleType = ImageView.ScaleType.FIT_CENTER
    adjustViewBounds = true
    setPadding(8, 8, 8, 8)
    setBackgroundColor(Color.rgb(245,245,245))}

        card.addView(thumbnail, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            520
        ))

        card.addView(TextView(this).apply {
    text = page.title
    textSize = 28f
    gravity = Gravity.CENTER
    setTextColor(Color.rgb(70, 50, 40))
    setTypeface(typeface, Typeface.BOLD)
    setPadding(0, 16, 0, 4)
})

val progressFile = File(filesDir, "progress_${page.name}.png")

card.addView(TextView(this).apply {

    val completed = isCompleted(page)

    text = when {
        completed -> "🏆 Completed"
        progressFile.exists() -> "⭐ Started"
        else -> "Ready to color!"
    }

    textSize = 20f
    gravity = Gravity.CENTER

    setTextColor(
        when {
            completed -> Color.rgb(70, 150, 80)
            progressFile.exists() -> Color.rgb(210, 140, 40)
            else -> Color.rgb(120, 100, 90)
        }
    )

    setPadding(0, 4, 0, 8)
})

        root.addView(card, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 36)
        })
    }

    scroll.addView(root)
    setContentView(scroll)
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

        val palette = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    gravity = Gravity.CENTER
    setPadding(0, 8, 0, 8)
}

val paletteRow1 = LinearLayout(this).apply {
    gravity = Gravity.CENTER
}

val paletteRow2 = LinearLayout(this).apply {
    gravity = Gravity.CENTER
}

paletteButtons.clear()
selectedColor = canvas.paintColor

colors.forEachIndexed { index, c ->
    val button = makeColorButton(c)

    val params = LinearLayout.LayoutParams(88, 88).apply {
        setMargins(6, 6, 6, 6)
    }

    if (index < 6) {
        paletteRow1.addView(button, params)
    } else {
        paletteRow2.addView(button, params)
    }
}

palette.addView(paletteRow1)
palette.addView(paletteRow2)

updatePaletteSelection()

        val toolRow = LinearLayout(this).apply { gravity = Gravity.CENTER }

        val brush = Button(this).apply {
            text = "Brush"
            setOnClickListener { canvas.tool = ColoringCanvas.Tool.BRUSH }
        }

        val bucket = Button(this).apply {
            text = "Bucket"
            setOnClickListener { canvas.tool = ColoringCanvas.Tool.BUCKET }
        }

var brushSizeIndex = 1
val brushSizes = floatArrayOf(12f, 24f, 40f)

val sizeButton = Button(this).apply {
    text = "Size: 24"
    setOnClickListener {
        brushSizeIndex = (brushSizeIndex + 1) % brushSizes.size
        canvas.brushSize = brushSizes[brushSizeIndex]
        text = "Size: ${brushSizes[brushSizeIndex].toInt()}"
    }
}

val done = Button(this).apply {
    text = "Done ✓"
    setOnClickListener {
        canvas.saveProgress()

        val alreadyCompleted = isCompleted(currentPage)

        setCompleted(currentPage, true)

        if (alreadyCompleted) {
            Toast.makeText(this@MainActivity, "Already completed!", Toast.LENGTH_SHORT).show()
        } else {
            showStickerReward(currentPage)
        }
    }
}

toolRow.addView(brush)
toolRow.addView(bucket)
toolRow.addView(sizeButton)
toolRow.addView(done)

        val actionRow = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    gravity = Gravity.CENTER
    setPadding(0, 0, 0, 140)
}

val actionRowTop = LinearLayout(this).apply {
    gravity = Gravity.CENTER
}

val actionRowBottom = LinearLayout(this).apply {
    gravity = Gravity.CENTER
}
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

       val share = Button(this).apply {
    text = "Share"
    setOnClickListener { shareCanvasArtwork() }

}

val testExport = Button(this).apply {
    text = "Test Export"
    setOnClickListener {
        saveCanvasToGallery()
    }
}



val previous = Button(this).apply {
    text = "Prev"
    setOnClickListener {
        canvas.saveProgress()
        val pages = ColoringPage.values()
        val index = pages.indexOf(currentPage)
        currentPage = pages[(index - 1 + pages.size) % pages.size]
        showColoringPage()
    }
}

val next = Button(this).apply {
    text = "Next"
    setOnClickListener {
        canvas.saveProgress()
        val pages = ColoringPage.values()
        val index = pages.indexOf(currentPage)
        currentPage = pages[(index + 1) % pages.size]
        showColoringPage()
    }
}

actionRowTop.addView(previous)
actionRowTop.addView(back)
actionRowTop.addView(undo)

actionRowBottom.addView(clear)
actionRowBottom.addView(testExport)
actionRowBottom.addView(share)
actionRowBottom.addView(next)

actionRow.addView(actionRowTop)
actionRow.addView(actionRowBottom)

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
private fun shareCanvasArtwork() {
    canvas.saveProgress()

    val bitmap = canvas.exportBitmap()
    val file = File(cacheDir, "color_cottage_share_${System.currentTimeMillis()}.jpg")

    file.outputStream().use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
    }

    val uri = FileProvider.getUriForFile(
        this,
        "${packageName}.fileprovider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    startActivity(Intent.createChooser(shareIntent, "Share artwork"))
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

        val rawLineArt = BitmapFactory.decodeResource(resources, page.imageRes)
        lineArtBitmap = makeWhiteTransparent(rawLineArt)
        colorBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        colorBitmap?.eraseColor(Color.TRANSPARENT)
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

                if (Color.alpha(pixel) ==255) {
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

    drawPage(exportCanvas)

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
       // val file = progressFile()

        //if (file.exists()) {
         //   val saved = BitmapFactory.decodeFile(file.absolutePath)
          //  if (saved != null) {
           //     val scaled = Bitmap.createScaledBitmap(saved, width, height, true)
            //    colorCanvas?.drawBitmap(scaled, 0f, 0f, null)
          //  }
     //   }
    }

    private fun progressFile(): File {
        return File(appContext.filesDir, "progress_${page.name}.png")
    }
}
