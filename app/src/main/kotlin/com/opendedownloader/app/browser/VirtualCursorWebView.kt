package com.opendedownloader.app.browser

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.webkit.WebView

class VirtualCursorWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private var cursorX = 300f
    private var cursorY = 300f
    private val cursorRadius = 12f
    private var isCursorEnabled = true

    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF") // High-contrast cyan
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun setCursorEnabled(enabled: Boolean) {
        isCursorEnabled = enabled
        invalidate()
    }

    fun isCursorEnabled() = isCursorEnabled

    fun moveCursor(dx: Float, dy: Float) {
        cursorX = (cursorX + dx).coerceIn(0f, width.toFloat())
        cursorY = (cursorY + dy).coerceIn(0f, height.toFloat())
        invalidate()
    }

    fun clickCursor() {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()

        val downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, cursorX, cursorY, 0)
        val upEvent = MotionEvent.obtain(downTime, eventTime + 50, MotionEvent.ACTION_UP, cursorX, cursorY, 0)

        dispatchTouchEvent(downEvent)
        dispatchTouchEvent(upEvent)

        downEvent.recycle()
        upEvent.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isCursorEnabled) {
            canvas.drawCircle(cursorX, cursorY, cursorRadius, cursorPaint)
            canvas.drawCircle(cursorX, cursorY, cursorRadius, borderPaint)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!isCursorEnabled) {
            return super.onKeyDown(keyCode, event)
        }

        // Custom step sizing for speed and remote navigability
        val step = 30f
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                moveCursor(0f, -step)
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                moveCursor(0f, step)
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                moveCursor(-step, 0f)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                moveCursor(step, 0f)
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                clickCursor()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
