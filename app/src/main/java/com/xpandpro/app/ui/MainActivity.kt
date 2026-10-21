package com.xpandpro.app.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import com.xpandpro.app.Config
import com.xpandpro.app.PrefsStateStore
import com.xpandpro.app.R
import com.xpandpro.app.display.DisplayController
import com.xpandpro.app.display.DisplayState
import com.xpandpro.app.display.Outcome
import com.xpandpro.app.display.StretchPreset
import com.xpandpro.app.shizuku.ShizukuManager
import com.xpandpro.app.shizuku.ShizukuManager.Status
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private lateinit var store: PrefsStateStore
    private lateinit var shizuku: ShizukuManager
    private lateinit var controller: DisplayController

    private lateinit var statusText: TextView
    private lateinit var mainButton: Button
    private lateinit var infoText: TextView
    private lateinit var presetGroup: RadioGroup
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
        presetGroup = findViewById(R.id.preset_group)
        gameButton = findViewById(R.id.game_button)

        presetGroup.check(radioIdFor(store.preset))
        presetGroup.setOnCheckedChangeListener { _, id -> store.preset = presetForId(id) }

        mainButton.setOnClickListener { onMainButton() }
        gameButton.setOnClickListener { openGame() }

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

    private fun isStretched() = deviceState?.isOverridden == true

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

        val s = deviceState
        infoText.text = if (status == Status.READY && s != null) {
            if (stretched) getString(R.string.info_stretched, s.physical.toString(), s.current.toString())
            else getString(R.string.info_normal, s.physical.toString())
        } else ""

        val canChoose = !busy && !stretched
        for (i in 0 until presetGroup.childCount) presetGroup.getChildAt(i).isEnabled = canChoose
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
        val preset = store.preset
        setBusy(true)
        io.execute {
            val out = controller.apply(preset)
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

    private fun runRestore() {
        cancelConfirm()
        setBusy(true)
        io.execute {
            val out = controller.restore()
            ui.post {
                busy = false
                when (out) {
                    is Outcome.Success -> deviceState = out.state
                    is Outcome.Failure -> toast(out.message)
                }
                refresh()
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

    private fun radioIdFor(p: StretchPreset) = when (p) {
        StretchPreset.R16_9 -> R.id.rb_16_9
        StretchPreset.R16_10 -> R.id.rb_16_10
        StretchPreset.R4_3 -> R.id.rb_4_3
        StretchPreset.R5_4 -> R.id.rb_5_4
    }

    private fun presetForId(id: Int) = when (id) {
        R.id.rb_16_9 -> StretchPreset.R16_9
        R.id.rb_16_10 -> StretchPreset.R16_10
        R.id.rb_5_4 -> StretchPreset.R5_4
        else -> StretchPreset.R4_3
    }

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
