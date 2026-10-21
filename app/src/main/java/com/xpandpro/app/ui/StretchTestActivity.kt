package com.xpandpro.app.ui

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.xpandpro.app.R

/**
 * Prueba de compatibilidad, igual para cualquier teléfono. Se muestra con la
 * resolución ya cambiada: dibuja un círculo perfecto (en la resolución lógica)
 * y un borde en los límites de la imagen. Según cómo lo vea el usuario:
 *  - redondo y con barras negras a los lados → el teléfono NO estira;
 *  - ovalado y el borde toca los bordes reales de la pantalla → SÍ estira.
 */
class StretchTestActivity : Activity() {

    companion object {
        const val RESULT_ROUND_BARS = 1
        const val RESULT_OVAL_FILL = 2
        const val RESULT_UNSURE = 3
        private const val SECONDS = 25
    }

    private var countdown: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        if (Build.VERSION.SDK_INT >= 28) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }

        val dp = resources.displayMetrics.density
        val root = FrameLayout(this)
        root.addView(
            TestView(this),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(0xCC000000.toInt())
            setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), (8 * dp).toInt())
        }
        val question = TextView(this).apply {
            text = getString(R.string.test_question, SECONDS)
            setTextColor(getColor(R.color.text))
            textSize = 14f
            gravity = Gravity.CENTER
        }
        panel.addView(question)

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun option(textRes: Int, code: Int) = Button(this).apply {
            setText(textRes)
            textSize = 12f
            isAllCaps = true
            setTextColor(getColor(R.color.text))
            setBackgroundResource(R.drawable.bg_secondary)
            stateListAnimator = null
            setOnClickListener { finishWith(code) }
        }
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            .apply { setMargins((4 * dp).toInt(), 0, (4 * dp).toInt(), 0) }
        row.addView(option(R.string.test_round_bars, RESULT_ROUND_BARS), lp)
        row.addView(option(R.string.test_oval_fill, RESULT_OVAL_FILL), lp)
        row.addView(option(R.string.test_unsure, RESULT_UNSURE), lp)
        panel.addView(row)

        root.addView(
            panel,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )
        setContentView(root)

        // Red de seguridad: si no se responde, se cierra y la pantalla se restaura.
        countdown = object : CountDownTimer(SECONDS * 1000L, 1000L) {
            override fun onTick(msLeft: Long) {
                question.text = getString(R.string.test_question, (msLeft / 1000L + 1).toInt())
            }

            override fun onFinish() {
                setResult(RESULT_CANCELED)
                finish()
            }
        }.start()
    }

    private fun finishWith(code: Int) {
        countdown?.cancel()
        setResult(code)
        finish()
    }

    override fun onBackPressed() {
        countdown?.cancel()
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onDestroy() {
        countdown?.cancel()
        super.onDestroy()
    }

    private class TestView(context: Context) : View(context) {
        private val border = Paint().apply {
            style = Paint.Style.STROKE
            color = 0xFF00E676.toInt()
            strokeWidth = 8f * resources.displayMetrics.density
        }
        private val fill = Paint().apply {
            style = Paint.Style.FILL
            color = 0xFFE10600.toInt()
            isAntiAlias = true
        }
        private val line = Paint().apply {
            style = Paint.Style.STROKE
            color = 0xFFFFFFFF.toInt()
            strokeWidth = 2f * resources.displayMetrics.density
            isAntiAlias = true
        }
        private val label = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        override fun onDraw(c: Canvas) {
            c.drawColor(0xFF000000.toInt())
            val w = width.toFloat()
            val h = height.toFloat()
            val half = border.strokeWidth / 2f
            c.drawRect(half, half, w - half, h - half, border)

            // Círculo perfecto en la resolución lógica (arriba, para no tapar los botones).
            val r = h * 0.22f
            val cx = w / 2f
            val cy = h * 0.30f
            c.drawCircle(cx, cy, r, fill)
            c.drawLine(cx - r, cy, cx + r, cy, line)
            c.drawLine(cx, cy - r, cx, cy + r, line)
            label.textSize = h * 0.05f
            c.drawText("${width} × ${height}", cx, h * 0.62f, label)
        }
    }
}
