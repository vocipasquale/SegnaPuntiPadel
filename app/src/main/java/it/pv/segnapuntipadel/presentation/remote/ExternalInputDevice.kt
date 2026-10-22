package it.pv.segnapuntipadel.presentation.remote

import android.os.Build
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent

/**
 * Distingue i dispositivi di input esterni (telecomando Bluetooth) dai tasti del telefono.
 * InputDevice.isExternal è pubblico da Android 10; sulle versioni precedenti esiste come
 * metodo nascosto e viene letto via reflection.
 */
object ExternalInputDevice {
    private const val TAG = "SegnaPuntiRemote"
    private var fallbackLogged = false

    private val hiddenIsExternal by lazy {
        try {
            InputDevice::class.java.getMethod("isExternal")
        } catch (e: NoSuchMethodException) {
            Log.w(TAG, "InputDevice.isExternal non disponibile", e)
            null
        }
    }

    fun isExternal(event: KeyEvent): Boolean {
        val device = event.device ?: return false
        if (device.isVirtual) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return device.isExternal

        val method = hiddenIsExternal
        if (method != null) {
            try {
                return method.invoke(device) as Boolean
            } catch (e: ReflectiveOperationException) {
                logFallback(e)
            } catch (e: SecurityException) {
                logFallback(e)
            }
        }
        // Senza informazione affidabile si privilegia il funzionamento del telecomando.
        return true
    }

    private fun logFallback(error: Exception) {
        if (fallbackLogged) return
        fallbackLogged = true
        Log.w(TAG, "Impossibile leggere InputDevice.isExternal: ogni dispositivo fisico è considerato esterno", error)
    }
}
