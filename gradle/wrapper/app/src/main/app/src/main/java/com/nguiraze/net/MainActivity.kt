package com.nguiraze.net

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = TextView(this).apply {
            text = "NGUIRAZE NET\nAplicação iniciada com sucesso!"
            textSize = 22f
            setPadding(32, 64, 32, 32)
        }

        setContentView(text)
    }
}
