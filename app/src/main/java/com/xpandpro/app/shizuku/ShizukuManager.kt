package com.xpandpro.app.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.SystemClock
import com.xpandpro.app.BuildConfig
import com.xpandpro.app.display.ShellResult
import com.xpandpro.app.display.ShellRunner
import com.xpandpro.app.shell.IShellService
import com.xpandpro.app.shell.ShellService
import rikka.shizuku.Shizuku

class ShizukuManager(
    context: Context,
    private val onChanged: () -> Unit,
) : ShellRunner {

    enum class Status { UNAVAILABLE, NEEDS_PERMISSION, PERMISSION_BLOCKED, CONNECTING, READY }

    @Volatile private var service: IShellService? = null
    @Volatile private var binding = false

    private val args = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, ShellService::class.java.name)
    )
        .processNameSuffix("shell")
        .daemon(false)
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
            binding = false
            service = if (binder != null && binder.pingBinder()) IShellService.Stub.asInterface(binder) else null
            onChanged()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            binding = false
            onChanged()
        }
    }

    private val binderReceived = Shizuku.OnBinderReceivedListener { onChanged() }
    private val binderDead = Shizuku.OnBinderDeadListener {
        service = null
        binding = false
        onChanged()
    }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { _, _ -> onChanged() }

    fun register() {
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
    }

    fun unregister() {
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        Shizuku.removeRequestPermissionResultListener(permissionResult)
        if (service != null || binding) {
            try {
                Shizuku.unbindUserService(args, connection, true)
            } catch (_: Throwable) {
            }
        }
        service = null
        binding = false
    }

    fun status(): Status {
        return try {
            if (!Shizuku.pingBinder() || Shizuku.isPreV11()) return Status.UNAVAILABLE
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                return if (Shizuku.shouldShowRequestPermissionRationale()) Status.PERMISSION_BLOCKED
                else Status.NEEDS_PERMISSION
            }
            if (service != null) Status.READY else Status.CONNECTING
        } catch (_: Throwable) {
            Status.UNAVAILABLE
        }
    }

    fun requestPermission() {
        try {
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (_: Throwable) {
        }
    }

    /** Pide a Shizuku arrancar nuestro servicio si ya hay permiso. */
    fun ensureBound() {
        if (service != null || binding) return
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                binding = true
                Shizuku.bindUserService(args, connection)
            }
        } catch (_: Throwable) {
            binding = false
        }
    }

    /** Bloqueante: llamar siempre fuera del hilo principal. */
    override fun run(command: String): ShellResult {
        val s = awaitService(5000)
            ?: return ShellResult(-1, "Servicio de Shizuku no disponible")
        return try {
            val raw = s.exec(command) ?: return ShellResult(-1, "Sin respuesta")
            val code = raw.substringBefore('\n').trim().toIntOrNull() ?: -1
            ShellResult(code, raw.substringAfter('\n', ""))
        } catch (t: Throwable) {
            ShellResult(-1, t.message ?: "Error")
        }
    }

    private fun awaitService(timeoutMs: Long): IShellService? {
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (service == null && SystemClock.elapsedRealtime() < end) {
            ensureBound()
            Thread.sleep(100)
        }
        return service
    }

    private companion object {
        const val REQUEST_CODE = 1001
    }
}
