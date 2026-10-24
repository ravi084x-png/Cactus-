package com.tera.keyboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class TeraIME : InputMethodService() {

    private val apiUrl = "https://tera-ai-new.vercel.app/api/fix"
    private var shift = false
    private var symbols = false
    private lateinit var root: LinearLayout
    private val ui = Handler(Looper.getMainLooper())

    override fun onCreateInputView(): View {
        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(0xFF1E1E1E.toInt())
        root.setPadding(dp(4), dp(6), dp(4), dp(6))
        build()
        return root
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun key(label: String, weight: Float = 1f, color: Int = 0xFF3A3A3A.toInt(), onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = label
        b.isAllCaps = false
        b.setTextColor(0xFFFFFFFF.toInt())
        b.textSize = 18f
        b.setBackgroundColor(color)
        b.setPadding(0, 0, 0, 0)
        val lp = LinearLayout.LayoutParams(0, dp(48), weight)
        lp.setMargins(dp(2), dp(3), dp(2), dp(3))
        b.layoutParams = lp
        b.setOnClickListener { onClick() }
        return b
    }

    private fun build() {
        root.removeAllViews()
        val rows = if (symbols)
            listOf("1234567890", "@#\$%&-+()/", "*\"':;!?,.~")
        else
            listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")

        for ((i, r) in rows.withIndex()) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            if (i == 2 && !symbols) {
                row.addView(key(if (shift) "⬆" else "⇧", 1.5f) {
                    shift = !shift
                    build()
                })
            }
            for (ch in r) {
                val s = ch.toString()
                val label = if (shift && !symbols) s.uppercase() else s
                row.addView(key(label) {
                    currentInputConnection?.commitText(label, 1)
                    if (shift) {
                        shift = false
                        build()
                    }
                })
            }
            if (i == 2) {
                row.addView(key("⌫", 1.5f) {
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                })
            }
            root.addView(row)
        }

        val bottom = LinearLayout(this)
        bottom.orientation = LinearLayout.HORIZONTAL
        bottom.addView(key(if (symbols) "ABC" else "?123", 1.3f) {
            symbols = !symbols
            build()
        })
        bottom.addView(key("✨", 1.5f, 0xFF7C3AED.toInt()) { fixText() })
        bottom.addView(key("space", 4f) {
            currentInputConnection?.commitText(" ", 1)
        })
        bottom.addView(key(".", 1f) {
            currentInputConnection?.commitText(".", 1)
        })
        bottom.addView(key("↵", 1.3f) { pressEnter() })
        root.addView(bottom)
    }

    private fun pressEnter() {
        val ei = currentInputEditorInfo
        val action = (ei?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        val noAction = ((ei?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            currentInputConnection?.performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun fixText() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(5000, 0)?.toString() ?: ""
        val after = ic.getTextAfterCursor(5000, 0)?.toString() ?: ""
        val full = before + after
        if (full.isBlank()) {
            toast("Pehle kuch likho")
            return
        }
        toast("✨ Sudhar raha hoon...")

        Thread {
            try {
                val conn = URL(apiUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.outputStream.write(JSONObject().put("text", full).toString().toByteArray())

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream.bufferedReader().readText()
                val j = JSONObject(body)
                val result = j.optString("result", "")

                ui.post {
                    if (result.isNotEmpty()) {
                        val ic2 = currentInputConnection
                        ic2?.deleteSurroundingText(before.length, after.length)
                        ic2?.commitText(result, 1)
                    } else {
                        toast(j.optString("error", "Kuch gadbad hui"))
                    }
                }
            } catch (e: Exception) {
                ui.post { toast("Internet check karo") }
            }
        }.start()
    }
}
