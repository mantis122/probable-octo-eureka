package com.colorcottage.app

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(255, 248, 235))
        }

        val title = TextView(this).apply {
            text = "Color Cottage"
            textSize = 30f
            setTextColor(Color.rgb(80, 48, 24))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 24, 0, 16)
        }

        val canvas = ColoringCanvas(this)
        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 20)
        }

        val colors = listOf(
            Color.RED, Color.rgb(255, 140, 0), Color.YELLOW, Color.GREEN,
            Color.CYAN, Color.BLUE, Color.rgb(160, 64, 255), Color.MAGENTA,
            Color.rgb(120, 70, 30), Color.BLACK
        )

        val palette = LinearLayout(this).apply { gravity = android.view.Gravity.CENTER }
        colors.forEach { c ->
            val b = Button(this).apply {
                text = " "
                setBackgroundColor(c)
                setOnClickListener { canvas.paintColor = c }
            }
            palette.addView(b, LinearLayout.LayoutParams(86, 86).apply { setMargins(5, 5, 5, 5) })
        }

        val buttons = LinearLayout(this).apply { gravity = android.view.Gravity.CENTER }
        val undo = Button(this).apply { text = "Undo"; setOnClickListener { canvas.undo() } }
        val clear = Button(this).apply { text = "Clear"; setOnClickListener { canvas.clearDrawing() } }
        buttons.addView(undo)
        buttons.addView(clear)

        tools.addView(palette)
        tools.addView(buttons)  
        root.addView(title)  
        root.addView(
    canvas,
    LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        0,
        1f
    )
)
        root.addView(tools)
        setContentView(root)
    }
}

class ColoringCanvas(context: android.content.Context) : View(context) {
    var paintColor: Int = Color.RED
    private val strokes = mutableListOf<Pair<Path, Paint>>()
    private var currentPath: Path? = null
    private var currentPaint: Paint? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawColoringPage(canvas)
        strokes.forEach { canvas.drawPath(it.first, it.second) }
        currentPath?.let { path -> currentPaint?.let { paint -> canvas.drawPath(path, paint) } }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path().apply { moveTo(event.x, event.y) }
                currentPaint = Paint().apply {
                    color = paintColor
                    style = Paint.Style.STROKE
                    strokeWidth = 34f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    isAntiAlias = true
                }
            }
            MotionEvent.ACTION_MOVE -> currentPath?.lineTo(event.x, event.y)
            MotionEvent.ACTION_UP -> {
                currentPath?.let { p -> currentPaint?.let { strokes.add(Pair(p, it)) } }
                currentPath = null
                currentPaint = null
            }
        }
        invalidate()
        return true
    }

    fun undo() {
        if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
        invalidate()
    }

    fun clearDrawing() {
        strokes.clear()
        invalidate()
    }

    private fun drawColoringPage(canvas: Canvas) {
        val line = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 10f
            isAntiAlias = true
        }
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val houseTop = h * 0.24f
        val houseBottom = h * 0.78f
        val houseLeft = w * 0.18f
        val houseRight = w * 0.82f

        // sun
        canvas.drawCircle(w * 0.82f, h * 0.13f, 54f, line)
        for (i in 0 until 8) {
            val a = Math.toRadians((i * 45).toDouble())
            val x1 = w * 0.82f + Math.cos(a).toFloat() * 75f
            val y1 = h * 0.13f + Math.sin(a).toFloat() * 75f
            val x2 = w * 0.82f + Math.cos(a).toFloat() * 105f
            val y2 = h * 0.13f + Math.sin(a).toFloat() * 105f
            canvas.drawLine(x1, y1, x2, y2, line)
        }

        // house
        val roof = Path().apply {
            moveTo(houseLeft - 35f, houseTop + 150f)
            lineTo(cx, houseTop)
            lineTo(houseRight + 35f, houseTop + 150f)
        }
        canvas.drawPath(roof, line)
        canvas.drawRect(houseLeft, houseTop + 150f, houseRight, houseBottom, line)
        canvas.drawRect(cx - 55f, houseBottom - 155f, cx + 55f, houseBottom, line)
        canvas.drawCircle(cx + 35f, houseBottom - 80f, 8f, line)
        canvas.drawRect(houseLeft + 55f, houseTop + 220f, houseLeft + 155f, houseTop + 320f, line)
        canvas.drawRect(houseRight - 155f, houseTop + 220f, houseRight - 55f, houseTop + 320f, line)
        canvas.drawLine(houseLeft + 105f, houseTop + 220f, houseLeft + 105f, houseTop + 320f, line)
        canvas.drawLine(houseLeft + 55f, houseTop + 270f, houseLeft + 155f, houseTop + 270f, line)
        canvas.drawLine(houseRight - 105f, houseTop + 220f, houseRight - 105f, houseTop + 320f, line)
        canvas.drawLine(houseRight - 155f, houseTop + 270f, houseRight - 55f, houseTop + 270f, line)

        // flowers
        for (i in 0..4) {
            val fx = w * (0.18f + i * 0.16f)
            val fy = h * 0.88f
            canvas.drawLine(fx, fy, fx, fy - 55f, line)
            canvas.drawCircle(fx, fy - 70f, 18f, line)
            canvas.drawCircle(fx - 18f, fy - 55f, 18f, line)
            canvas.drawCircle(fx + 18f, fy - 55f, 18f, line)
            canvas.drawCircle(fx, fy - 40f, 18f, line)
        }
    }
}
