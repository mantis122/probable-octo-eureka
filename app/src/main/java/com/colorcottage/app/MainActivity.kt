package com.colorcottage.app

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(255, 248, 235))
        }

        val title = TextView(this).apply {
            text = "Color Cottage"
            textSize = 28f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 32, 0, 16)
            setTextColor(Color.rgb(70, 50, 40))
        }

        val canvas = ColoringCanvas(this)

        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(8, 8, 8, 24)
        }

        val palette = LinearLayout(this).apply {
            gravity = android.view.Gravity.CENTER
        }

        val colors = intArrayOf(
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
            Color.MAGENTA, Color.CYAN, Color.BLACK, Color.rgb(255, 140, 0)
        )

        colors.forEach { c ->
            val b = Button(this).apply {
                text = ""
                setBackgroundColor(c)
                setOnClickListener { canvas.paintColor = c }
            }
            palette.addView(
                b,
                LinearLayout.LayoutParams(86, 86).apply {
                    setMargins(5, 5, 5, 5)
                }
            )
        }

        val buttons = LinearLayout(this).apply {
            gravity = android.view.Gravity.CENTER
        }

        val undo = Button(this).apply {
            text = "Undo"
            setOnClickListener { canvas.undo() }
        }

        val clear = Button(this).apply {
            text = "Clear"
            setOnClickListener { canvas.clearDrawing() }
        }

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

    private val linePaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    init {
        setBackgroundColor(Color.WHITE)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(24f, 24f, w - 24f, h - 24f, linePaint)
        canvas.drawCircle(w / 2f, h / 2f, minOf(w, h) / 4f, linePaint)

        for ((path, paint) in strokes) {
            canvas.drawPath(path, paint)
        }

        currentPath?.let { path ->
            currentPaint?.let { paint ->
                canvas.drawPath(path, paint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path().apply {
                    moveTo(x, y)
                }

                currentPaint = Paint().apply {
                    color = paintColor
                    strokeWidth = 24f
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
}
