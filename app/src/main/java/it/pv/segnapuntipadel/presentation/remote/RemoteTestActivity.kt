package it.pv.segnapuntipadel.presentation.remote

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import it.pv.segnapuntipadel.R

/**
 * Schermata diagnostica: mostra tutti gli eventi dei tasti ricevuti (telecomando Bluetooth,
 * tasti fisici del dispositivo) senza modificare punteggi o volume.
 * Il registro è selezionabile e può essere copiato o condiviso per intero.
 */
class RemoteTestActivity : ComponentActivity() {
    private val entries = ArrayDeque<String>()
    private var eventCount = 0
    private var lastEventTime = NO_EVENT
    private lateinit var log: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_remote_test)

        log = findViewById(R.id.remote_test_log)
        findViewById<Button>(R.id.remote_test_copy).setOnClickListener { copyLog() }
        findViewById<Button>(R.id.remote_test_share).setOnClickListener { shareLog() }
        findViewById<Button>(R.id.remote_test_clear).setOnClickListener { clear() }
        findViewById<Button>(R.id.remote_test_back).setOnClickListener { finish() }

        savedInstanceState?.let { state ->
            state.getStringArrayList(STATE_ENTRIES)?.let { entries.addAll(it) }
            eventCount = state.getInt(STATE_COUNT)
        }
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(STATE_ENTRIES, ArrayList(entries))
        outState.putInt(STATE_COUNT, eventCount)
    }

    // Gli eventi vanno letti qui, prima delle view: il registro selezionabile può ricevere il
    // focus e consumare alcuni tasti (es. Invio, frecce), che altrimenti non verrebbero mostrati.
    // RestrictedApi è un falso positivo su ComponentActivity.dispatchKeyEvent (metodo pubblico).
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        record(event)
        // I tasti volume vengono consumati per non cambiare il volume durante il test;
        // gli altri proseguono con la gestione standard (es. Indietro).
        return isVolumeKey(event.keyCode) || super.dispatchKeyEvent(event)
    }

    private fun isVolumeKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_MUTE

    private fun record(event: KeyEvent) {
        eventCount++
        val interval = if (lastEventTime == NO_EVENT) {
            getString(R.string.remote_test_no_interval)
        } else {
            getString(R.string.remote_test_interval, event.eventTime - lastEventTime)
        }
        lastEventTime = event.eventTime

        val entry = getString(
            R.string.remote_test_entry,
            eventCount,
            actionLabel(event.action),
            KeyEvent.keyCodeToString(event.keyCode),
            event.keyCode,
            event.repeatCount,
            event.scanCode,
            interval,
            event.device?.name ?: getString(R.string.remote_test_unknown_device),
            event.deviceId,
            if (event.isLongPress) getString(R.string.remote_test_long_press) else "",
            getString(
                if (ExternalInputDevice.isExternal(event)) {
                    R.string.remote_test_external_yes
                } else {
                    R.string.remote_test_external_no
                }
            )
        )
        entries.addFirst(entry)
        while (entries.size > MAX_ENTRIES) entries.removeLast()
        render()
    }

    private fun actionLabel(action: Int): String = getString(
        when (action) {
            KeyEvent.ACTION_DOWN -> R.string.remote_test_action_down
            KeyEvent.ACTION_UP -> R.string.remote_test_action_up
            else -> R.string.remote_test_action_other
        }
    )

    private fun clear() {
        entries.clear()
        eventCount = 0
        lastEventTime = NO_EVENT
        render()
    }

    private fun render() {
        log.text = if (entries.isEmpty()) {
            getString(R.string.remote_test_waiting)
        } else {
            entries.joinToString("\n")
        }
    }

    /** Testo completo da inviare: intestazione con il dispositivo, poi eventi in ordine cronologico. */
    private fun exportText(): String? {
        if (entries.isEmpty()) {
            Toast.makeText(this, R.string.remote_test_nothing_to_export, Toast.LENGTH_SHORT).show()
            return null
        }
        val header = getString(
            R.string.remote_test_export_header,
            Build.MANUFACTURER,
            Build.MODEL,
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT,
            eventCount,
            entries.size
        )
        return header + "\n\n" + entries.reversed().joinToString("\n")
    }

    private fun copyLog() {
        val text = exportText() ?: return
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.remote_test_title), text))
        // Da Android 13 il sistema mostra già una conferma della copia.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, R.string.remote_test_copied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareLog() {
        val text = exportText() ?: return
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.remote_test_share_subject))
            .putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(send, getString(R.string.remote_test_share)))
    }

    private companion object {
        const val MAX_ENTRIES = 200
        const val NO_EVENT = -1L
        const val STATE_ENTRIES = "remote_test_entries"
        const val STATE_COUNT = "remote_test_count"
    }
}
