package com.example.healthieru.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import java.util.Locale

/** A smooth, animated line chart drawn with Canvas. No external library needed. */
class ProgressChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var labels: List<String> = emptyList()
    private var values: List<Double> = emptyList()
    private var unit = ""
    private var accent = Color.parseColor("#1B7F6B")
    private var reveal = 1f
    private var animator: ValueAnimator? = null
    private val d = resources.displayMetrics.density

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E1E8E5"); strokeWidth = 1f * d
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5F6F7A"); textSize = 11f * d
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3f * d
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#22334A"); textSize = 13f * d
        typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT
    }
    private val messagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5F6F7A"); textSize = 14f * d; textAlign = Paint.Align.CENTER
    }

    fun setData(labels: List<String>, values: List<Double>, unit: String, color: Int) {
        this.labels = labels
        this.values = values
        this.unit = unit
        this.accent = color
        animator?.cancel()
        reveal = 0f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            addUpdateListener { reveal = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    private fun fmt(v: Double) =
        if (unit == "bpm") String.format(Locale.US, "%.0f", v) else String.format(Locale.US, "%.1f", v)

    private fun smoothPath(xs: FloatArray, ys: FloatArray): Path {
        val p = Path()
        p.moveTo(xs[0], ys[0])
        for (i in 1 until xs.size) {
            val cx = (xs[i - 1] + xs[i]) / 2f
            p.cubicTo(cx, ys[i - 1], cx, ys[i], xs[i], ys[i])
        }
        return p
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        if (values.size < 2) {
            canvas.drawText("Record your data on at least two days", w / 2f, h / 2f - 8 * d, messagePaint)
            canvas.drawText("to see your progress here.", w / 2f, h / 2f + 14 * d, messagePaint)
            return
        }

        val left = 52f * d
        val right = w - 24f * d
        val top = 28f * d
        val bottom = h - 32f * d

        var min = values.minOrNull() ?: 0.0
        var max = values.maxOrNull() ?: 1.0
        if (max - min < 0.5) { min -= 1.0; max += 1.0 }
        else { val pad = (max - min) * 0.2; min -= pad; max += pad }

        val n = values.size
        val xs = FloatArray(n) { left + (right - left) * it / (n - 1) }
        val ys = FloatArray(n) { i ->
            bottom - ((bottom - top) * ((values[i] - min) / (max - min))).toFloat()
        }

        // Grid lines and y-axis labels
        axisPaint.textAlign = Paint.Align.RIGHT
        for (i in 0..3) {
            val gy = top + (bottom - top) * i / 3f
            canvas.drawLine(left, gy, right, gy, gridPaint)
            val v = max - (max - min) * i / 3.0
            canvas.drawText(fmt(v), left - 8 * d, gy + 4 * d, axisPaint)
        }

        // X-axis labels (at most five)
        axisPaint.textAlign = Paint.Align.CENTER
        val count = minOf(n, 5)
        (0 until count)
            .map { Math.round(it * (n - 1).toFloat() / (count - 1)) }
            .distinct()
            .forEach { idx ->
                if (idx < labels.size) canvas.drawText(labels[idx], xs[idx], bottom + 22 * d, axisPaint)
            }

        // Reveal animation: clip the chart from left to right
        canvas.save()
        canvas.clipRect(0f, 0f, left + (w - left) * reveal, h)

        val line = smoothPath(xs, ys)
        val fill = Path(line).apply {
            lineTo(xs[n - 1], bottom)
            lineTo(xs[0], bottom)
            close()
        }
        fillPaint.shader = LinearGradient(
            0f, top, 0f, bottom,
            Color.argb(90, Color.red(accent), Color.green(accent), Color.blue(accent)),
            Color.argb(0, Color.red(accent), Color.green(accent), Color.blue(accent)),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(fill, fillPaint)

        linePaint.color = accent
        canvas.drawPath(line, linePaint)

        dotPaint.color = accent
        for (i in 0 until n) {
            val last = i == n - 1
            canvas.drawCircle(xs[i], ys[i], (if (last) 8f else 6f) * d, ringPaint)
            canvas.drawCircle(xs[i], ys[i], (if (last) 6f else 4.5f) * d, dotPaint)
        }

        // Value label above the latest point
        canvas.drawText("${fmt(values.last())} $unit", xs[n - 1] + 8 * d, ys[n - 1] - 16 * d, valuePaint)

        canvas.restore()
    }
}