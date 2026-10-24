package com.xpandpro.app.shell

import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import com.xpandpro.app.display.ProjectionPlan
import com.xpandpro.app.display.ProjectionPlanner
import java.lang.reflect.InvocationTargetException

/**
 * Corre dentro del proceso privilegiado de Shizuku. Usa APIs ocultas de Android
 * (SurfaceControl) por reflexión, igual que hace scrcpy, para cambiar cómo el compositor
 * proyecta la imagen sobre el panel. No toca `wm size`.
 *
 * El cambio no es permanente: reiniciar el teléfono, o cambiar `wm size`, lo deshace.
 */
object ProjectionTool {

    /** Devuelve "<codigo>\n<salida>" como el resto de comandos. */
    fun handle(cmd: String): String = try {
        val p = cmd.trim().split(" ")
        when (p.getOrNull(1)) {
            "info" -> "0\n" + info()
            "stretch" -> "0\n" + stretch(p[2].toInt(), p[3].toInt(), p[4].toInt())
            else -> "-2\nComando no permitido"
        }
    } catch (t: Throwable) {
        val e = unwrap(t)
        "1\nERR ${e.javaClass.simpleName}: ${e.message}"
    }

    private fun stretch(physW0: Int, physH0: Int, permille: Int): String {
        val di = displayInfo()
        val rotation = di.javaClass.getField("rotation").getInt(di)
        val lw = di.javaClass.getField("logicalWidth").getInt(di)
        val lh = di.javaClass.getField("logicalHeight").getInt(di)

        val plan = ProjectionPlanner.stretch(physW0, physH0, rotation, lw, lh, permille)
        if (plan is ProjectionPlan.Invalid) throw IllegalStateException(plan.reason)
        plan as ProjectionPlan.Ok

        val stack = Rect(plan.layerStack.left, plan.layerStack.top, plan.layerStack.right, plan.layerStack.bottom)
        val disp = Rect(plan.display.left, plan.display.top, plan.display.right, plan.display.bottom)
        applyProjection(displayToken(), rotation, stack, disp)
        return "ok rot=$rotation logical=${lw}x$lh display=${plan.display}"
    }

    private fun info(): String {
        val sb = StringBuilder()
        sb.appendLine("sdk=${Build.VERSION.SDK_INT} release=${Build.VERSION.RELEASE}")
        try {
            val di = displayInfo()
            val c = di.javaClass
            sb.appendLine(
                "rotation=${c.getField("rotation").getInt(di)} " +
                    "logical=${c.getField("logicalWidth").getInt(di)}x${c.getField("logicalHeight").getInt(di)}"
            )
        } catch (t: Throwable) {
            sb.appendLine("displayInfo: ERR ${unwrap(t).message}")
        }
        try {
            sb.appendLine("token=" + (displayToken() != null))
        } catch (t: Throwable) {
            sb.appendLine("token: ERR ${unwrap(t).message}")
        }
        val tx = runCatching { Class.forName("android.view.SurfaceControl\$Transaction") }.getOrNull()
        sb.appendLine(
            "Transaction.setDisplayProjection=" + (tx?.let {
                runCatching {
                    it.getMethod(
                        "setDisplayProjection", IBinder::class.java, Int::class.javaPrimitiveType,
                        Rect::class.java, Rect::class.java
                    )
                }.isSuccess
            } ?: false)
        )
        return sb.toString().trim()
    }

    private fun displayInfo(): Any {
        val cls = Class.forName("android.hardware.display.DisplayManagerGlobal")
        val inst = cls.getMethod("getInstance").invoke(null)
        return cls.getMethod("getDisplayInfo", Int::class.javaPrimitiveType).invoke(inst, 0)
            ?: throw IllegalStateException("Sin información de pantalla")
    }

    /**
     * Token de la pantalla principal, según la versión de Android:
     * 14+ -> DisplayControl (clase de services.jar, como hace scrcpy);
     * 10-13 -> SurfaceControl.getPhysicalDisplayIds/Token; 9 o menos -> getBuiltInDisplay(0).
     */
    private fun displayToken(): IBinder {
        val errors = StringBuilder()
        val sc = Class.forName("android.view.SurfaceControl")
        val attempts: List<Pair<String, () -> Any?>> = listOf(
            "getInternalDisplayToken" to { sc.getMethod("getInternalDisplayToken").invoke(null) },
            "SurfaceControl.physical" to {
                val ids = sc.getMethod("getPhysicalDisplayIds").invoke(null) as LongArray
                sc.getMethod("getPhysicalDisplayToken", Long::class.javaPrimitiveType).invoke(null, ids[0])
            },
            "DisplayControl" to {
                val dc = displayControlClass()
                val ids = dc.getMethod("getPhysicalDisplayIds").invoke(null) as LongArray
                dc.getMethod("getPhysicalDisplayToken", Long::class.javaPrimitiveType).invoke(null, ids[0])
            },
            "getBuiltInDisplay" to { sc.getMethod("getBuiltInDisplay", Int::class.javaPrimitiveType).invoke(null, 0) },
        )
        for ((name, f) in attempts) {
            try {
                (f() as? IBinder)?.let { return it }
            } catch (t: Throwable) {
                errors.append(name).append(": ").append(unwrap(t).javaClass.simpleName).append("; ")
            }
        }
        throw IllegalStateException("Sin token de pantalla ($errors)")
    }

    /** Android 14+: DisplayControl vive en services.jar y necesita la librería android_servers. */
    private fun displayControlClass(): Class<*> {
        runCatching { return Class.forName("android.view.DisplayControl") }
        val factory = Class.forName("com.android.internal.os.ClassLoaderFactory")
        val create = factory.getDeclaredMethod(
            "createClassLoader", String::class.java, String::class.java, String::class.java,
            ClassLoader::class.java, Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, String::class.java
        )
        val loader = create.invoke(
            null, "/system/framework/services.jar", null, null,
            ClassLoader.getSystemClassLoader(), 0, true, null
        ) as ClassLoader
        val dc = loader.loadClass("com.android.server.display.DisplayControl")
        val load = Runtime::class.java.getDeclaredMethod("loadLibrary0", Class::class.java, String::class.java)
        load.isAccessible = true
        load.invoke(Runtime.getRuntime(), dc, "android_servers")
        return dc
    }

    private fun applyProjection(token: IBinder, orientation: Int, stack: Rect, display: Rect) {
        val sc = Class.forName("android.view.SurfaceControl")
        val txClass = runCatching { Class.forName("android.view.SurfaceControl\$Transaction") }.getOrNull()
        val instanceMethod = txClass?.let {
            runCatching {
                it.getMethod(
                    "setDisplayProjection", IBinder::class.java, Int::class.javaPrimitiveType,
                    Rect::class.java, Rect::class.java
                )
            }.getOrNull()
        }
        if (txClass != null && instanceMethod != null) {
            // Android 10+ (API de transacciones)
            val tx = txClass.getDeclaredConstructor().newInstance()
            try {
                instanceMethod.invoke(tx, token, orientation, stack, display)
                txClass.getMethod("apply").invoke(tx)
            } finally {
                (tx as? AutoCloseable)?.close()
            }
        } else {
            // Android 9 y anteriores (API estática)
            sc.getMethod("openTransaction").invoke(null)
            try {
                sc.getMethod(
                    "setDisplayProjection", IBinder::class.java, Int::class.javaPrimitiveType,
                    Rect::class.java, Rect::class.java
                ).invoke(null, token, orientation, stack, display)
            } finally {
                sc.getMethod("closeTransaction").invoke(null)
            }
        }
    }

    private fun unwrap(t: Throwable): Throwable =
        if (t is InvocationTargetException) (t.targetException ?: t) else t
}
