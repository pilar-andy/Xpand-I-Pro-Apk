package com.xpandpro.app.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.xpandpro.app.AppServices
import com.xpandpro.app.Config
import com.xpandpro.app.PrefsStateStore
import com.xpandpro.app.R
import com.xpandpro.app.display.DisplayController
import com.xpandpro.app.display.DisplayState
import com.xpandpro.app.display.Outcome
import com.xpandpro.app.shizuku.ShizukuManager
import com.xpandpro.app.shizuku.ShizukuManager.Status
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {


    private lateinit var store: PrefsStateStore
    private lateinit var shizuku: ShizukuManager
    private lateinit var controller: DisplayController

    private lateinit var statusText: TextView
    private lateinit var mainButton: Button
    private lateinit var infoText: TextView
    private lateinit var multiplierText: TextView
    private lateinit var seek: SeekBar
    private lateinit var diagText: TextView
    private lateinit var gameButton: Button

    private val io = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())

    private var busy = false
    private var deviceState: DisplayState? = null
    private var countdown: CountDownTimer? = null
    private var confirmDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = PrefsStateStore(this)
        shizuku = ShizukuManager(this) { ui.post { refresh() } }
        controller = DisplayController(shizuku, store)

        statusText = findViewById(R.id.status_text)
        mainButton = findViewById(R.id.main_button)
        infoText = findViewById(R.id.info_text)
        multiplierText = findViewById(R.id.multiplier_text)
        seek = findViewById(R.id.multiplier_seek)
        diagText = findViewById(R.id.diag_text)
        gameButton = findViewById(R.id.game_button)

        // Barra: 0..29 -> 1.01x..1.30x
        seek.max = Config.MAX_PERCENT - Config.MIN_PERCENT
        seek.progress = store.multiplierPercent - Config.MIN_PERCENT
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) store.multiplierPercent = progress + Config.MIN_PERCENT
                render()
            }
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })
        diagText.text = getString(R.string.diag, Build.MODEL, Build.VERSION.RELEASE, Build.VERSION.SDK_INT)

        mainButton.setOnClickListener { onMainButton() }
        gameButton.setOnClickListener { openGame() }

        AppServices.shizuku = shizuku
        shizuku.register()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onStop() {
        super.onStop()
        // Si el usuario sale de la app mientras corre la cuenta atrás, es que ve y
        // maneja el teléfono con normalidad: se da por confirmado y se conserva el estirado.
        cancelConfirm()
    }

    override fun onDestroy() {
        cancelConfirm()
        if (AppServices.shizuku === shizuku) AppServices.shizuku = null
        shizuku.unregister()
        io.shutdown()
        super.onDestroy()
    }

    // ---------- estado y pantalla ----------

    private fun refresh() {
        val status = shizuku.status()
        shizuku.ensureBound()
        if (status == Status.READY && !busy) {
            io.execute {
                val s = controller.readState()
                ui.post { deviceState = s; render() }
            }
        }
        render()
    }

    private fun isStretched() =
        deviceState?.isOverridden == true || store.scalingChanged || store.densityChanged ||
            store.projectionChanged

    private fun render() {
        val status = shizuku.status()
        val stretched = status == Status.READY && isStretched()

        statusText.text = getString(
            when (status) {
                Status.UNAVAILABLE -> R.string.status_unavailable
                Status.NEEDS_PERMISSION -> R.string.status_needs_permission
                Status.PERMISSION_BLOCKED -> R.string.status_blocked
                Status.CONNECTING -> R.string.status_connecting
                Status.READY -> R.string.status_ready
            }
        )

        mainButton.setText(
            when (status) {
                Status.UNAVAILABLE -> if (shizukuInstalled()) R.string.btn_open_shizuku else R.string.btn_install_shizuku
                Status.NEEDS_PERMISSION -> R.string.btn_grant
                Status.PERMISSION_BLOCKED -> R.string.btn_open_shizuku
                Status.CONNECTING -> R.string.btn_connecting
                Status.READY -> if (stretched) R.string.btn_restore else R.string.btn_activate
            }
        )
        mainButton.setBackgroundResource(if (stretched) R.drawable.bg_main_on else R.drawable.bg_main_off)
        mainButton.isEnabled = !busy && status != Status.CONNECTING

        val percent = store.multiplierPercent
        multiplierText.text = String.format(Locale.US, "%.2fx", percent / 100.0)

        val s = deviceState
        infoText.text = if (status == Status.READY && s != null) {
            if (stretched) getString(R.string.info_stretched, percent / 100.0)
            else getString(R.string.info_preview, percent / 100.0)
        } else ""

        seek.isEnabled = !busy && !stretched
        gameButton.visibility = if (gameInstalled()) View.VISIBLE else View.GONE
    }

    private fun setBusy(b: Boolean) {
        busy = b
        render()
    }

    // ---------- acciones ----------

    private fun onMainButton() {
        if (busy) return
        when (shizuku.status()) {
            Status.UNAVAILABLE ->
                if (shizukuInstalled()) openShizuku() else openShizukuStore()
            Status.PERMISSION_BLOCKED -> openShizuku()
            Status.NEEDS_PERMISSION -> shizuku.requestPermission()
            Status.CONNECTING -> Unit
            Status.READY -> if (isStretched()) runRestore() else runApply()
        }
    }

    private fun runApply() {
        val multiplier = store.multiplierPercent / 100.0
        setBusy(true)
        io.execute {
            val out = controller.applyProjection(multiplier)
            ui.post {
                busy = false
                when (out) {
                    is Outcome.Success -> {
                        deviceState = out.state
                        render()
                        startConfirm()
                    }
                    is Outcome.Failure -> {
                        toast(out.message)
                        refresh()
                    }
                }
            }
        }
    }

    private fun runRestore(onDone: (() -> Unit)? = null) {
        cancelConfirm()
        setBusy(true)
        io.execute {
            val out = controller.restore()
            ui.post {
                busy = false
                when (out) {
                    is Outcome.Success -> {
                        deviceState = out.state
                        refresh()
                        onDone?.invoke()
                    }
                    is Outcome.Failure -> {
                        toast(out.message)
                        refresh()
                    }
                }
            }
        }
    }

    /** Red de seguridad: si no se confirma a tiempo, se restaura sola. */
    private fun startConfirm() {
        cancelConfirm()
        val dialog = AlertDialog.Builder(this, R.style.Theme_XpandPro_Dialog)
            .setTitle(R.string.confirm_title)
            .setMessage(getString(R.string.confirm_message, Config.CONFIRM_SECONDS))
            .setCancelable(false)
            .setPositiveButton(R.string.confirm_keep) { _, _ -> cancelConfirm() }
            .setNegativeButton(R.string.confirm_restore) { _, _ -> runRestore() }
            .create()
        confirmDialog = dialog
        dialog.show()

        countdown = object : CountDownTimer(Config.CONFIRM_SECONDS * 1000L, 1000L) {
            override fun onTick(msLeft: Long) {
                dialog.setMessage(getString(R.string.confirm_message, (msLeft / 1000L + 1).toInt()))
            }

            override fun onFinish() {
                runRestore()
            }
        }.start()
    }

    private fun cancelConfirm() {
        countdown?.cancel()
        countdown = null
        confirmDialog?.dismiss()
        confirmDialog = null
    }

    // ---------- utilidades ----------

    private fun packageInstalled(pkg: String) = try {
        packageManager.getPackageInfo(pkg, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun shizukuInstalled() = packageInstalled(Config.SHIZUKU_PACKAGE)
    private fun gameInstalled() = packageInstalled(Config.BLOOD_STRIKE_PACKAGE)

    private fun openShizuku() {
        val intent = packageManager.getLaunchIntentForPackage(Config.SHIZUKU_PACKAGE)
        if (intent != null) startActivity(intent) else openShizukuStore()
    }

    private fun openShizukuStore() {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${Config.SHIZUKU_PACKAGE}"))
        try {
            startActivity(market)
        } catch (_: Exception) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${Config.SHIZUKU_PACKAGE}")
                )
            )
        }
    }

    private fun openGame() {
        val intent = packageManager.getLaunchIntentForPackage(Config.BLOOD_STRIKE_PACKAGE)
        if (intent != null) startActivity(intent) else toast(getString(R.string.game_not_installed))
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
