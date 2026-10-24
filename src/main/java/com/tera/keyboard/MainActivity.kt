package com.tera.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER
        box.setPadding(48, 48, 48, 48)

        val title = TextView(this)
        title.text = "Tera Keyboard\n\nKadam 1: keyboard ON karo\nKadam 2: keyboard chuno"
        title.textSize = 20f
        title.gravity = Gravity.CENTER

        val b1 = Button(this)
        b1.text = "1. Keyboard ON karo"
        b1.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        val b2 = Button(this)
        b2.text = "2. Keyboard chuno"
        b2.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        box.addView(title)
        box.addView(b1)
        box.addView(b2)
        setContentView(box)
    }
}
