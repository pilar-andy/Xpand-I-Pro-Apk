package com.xpandpro.app.ui

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.xpandpro.app.AppServices
import com.xpandpro.app.PrefsStateStore
import com.xpandpro.app.R
import com.xpandpro.app.display.DisplayController
import com.xpandpro.app.display.Outcome
import java.util.concurrent.Executors

/**
 * Prueba de compatibilidad, igual para cualquier teléfono. Se muestra con la
 * resolución ya cambiada: dibuja un círculo perfecto (en la resolución lógica)
 * y un borde en los límites de la imagen. Según cómo lo vea el usuario:
 *  - redondo y con barras negras a los lados → el teléfono NO estira;
 *  - ovalado y el borde toca los bordes reales de la pantalla → SÍ estira.
 *
 * Modo EXPERIMENTAL "proyección": no cambia `wm size`; una vez en horizontal pide
 * al servicio privilegiado que estire la franja central de la imagen a todo el
 * panel. También muestra dónde se registra el toque, para medir el desajuste.
 */
class StretchTestActivity : Activity() {

    companion object {
        const val RESULT_ROUND_BARS = 1
        const val RESULT_OVAL_FILL = 2
        const val RESULT_UNSURE = 3
        const val EXTRA_STEP = "step"
        const val EXTRA_TOTAL = "total"
        const val EXTRA_PROJECTION = "projection"
        private const val SECONDS = 25
    }

    private var countdown: CountDownTimer? = null
    private var step = 1
    private var total = 1

    private val io = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())
    private var projectionMode = false
    private var projectionApplied = false
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        step = intent.getIntExtra(EXTRA_STEP, 1)
        total = intent.getIntExtra(EXTRA_TOTAL, 1)
        projectionMode = intent.getBooleanExtra(EXTRA_PROJECTION, false)
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
            text = getString(R.string.test_question, step, total, SECONDS)
            setTextColor(getColor(R.color.text))
            textSize = 14f
            gravity = Gravity.CENTER
        }
        panel.addView(question)

        statusView = TextView(this).apply {
            setTextColor(getColor(R.color.text_dim))
            textSize = 11f
            gravity = Gravity.CENTER
            visibility = if (projectionMode) View.VISIBLE else View.GONE
            text = getString(R.string.projection_waiting)
        }
        panel.addView(statusView)

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
                question.text = getString(R.string.test_question, step, total, (msLeft / 1000L + 1).toInt())
            }

            override fun onFinish() {
                closeWith(RESULT_CANCELED)
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        // El cambio de orientación ya ocurrió: ahora se puede aplicar la proyección.
        if (projectionMode && !projectionApplied) ui.postDelayed({ applyProjection() }, 800)
    }

    // ---------- proyección directa (experimental) ----------

    private fun controller(): DisplayController? =
        AppServices.shizuku?.let { DisplayController(it, PrefsStateStore(this)) }

    private fun applyProjection() {
        val c = controller()
        if (c == null) {
            statusView.text = getString(R.string.projection_no_service)
            return
        }
        val m = PrefsStateStore(this).multiplierPercent / 100.0
        statusView.text = getString(R.string.projection_applying, m)
        projectionApplied = true // se restaura siempre al salir, aunque falle a medias
        io.execute {
            val out = c.applyProjection(m)
            // Datos del sistema para diagnóstico (versión, rotación, API disponible).
            val info = AppServices.shizuku?.run("xpand-projection info")?.output?.trim().orEmpty()
            ui.post {
                val head = when (out) {
                    is Outcome.Success -> getString(R.string.projection_ok)
                    is Outcome.Failure -> getString(R.string.projection_error, out.message)
                }
                statusView.text = if (info.isEmpty()) head else "$head\n$info"
            }
        }
    }

    private fun restoreProjection(then: () -> Unit) {
        val c = controller()
        if (!projectionApplied || c == null) {
            then()
            return
        }
        projectionApplied = false
        io.execute {
            c.restore()
            ui.post { then() }
        }
    }

    // ---------- salida ----------

    private fun finishWith(code: Int) {
        countdown?.cancel()
        closeWith(code)
    }

    private fun closeWith(code: Int) {
        restoreProjection {
            setResult(code)
            finish()
        }
    }

    override fun onBackPressed() {
        countdown?.cancel()
        closeWith(RESULT_CANCELED)
    }

    override fun onDestroy() {
        countdown?.cancel()
        // Último recurso: si la actividad muere sin haber restaurado, se intenta ahora.
        val c = controller()
        if (projectionApplied && c != null) Thread { c.restore() }.start()
        io.shutdown()
        super.onDestroy()
    }

    private class TestView(context: Context) : View(context) {
        private var touchX = -1f
        private var touchY = -1f

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
        private val dot = Paint().apply {
            style = Paint.Style.STROKE
            color = 0xFFFFEB3B.toInt()
            strokeWidth = 3f * resources.displayMetrics.density
            isAntiAlias = true
        }
        private val label = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            if (e.actionMasked == MotionEvent.ACTION_DOWN || e.actionMasked == MotionEvent.ACTION_MOVE) {
                touchX = e.x
                touchY = e.y
                invalidate()
            }
            return true
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

            // Dónde se registra el toque (debería salir justo bajo el dedo).
            if (touchX >= 0f) {
                val tr = 18f * resources.displayMetrics.density
                c.drawCircle(touchX, touchY, tr, dot)
                label.textSize = h * 0.035f
                c.drawText("toque ${touchX.toInt()}, ${touchY.toInt()}", touchX, touchY - tr * 1.6f, label)
            }
        }
    }
}
