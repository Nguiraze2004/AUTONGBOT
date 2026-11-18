package com.nguiraze.net

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.widget.TextView
import android.view.Gravity

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val tela = TextView(this).apply {
            text = "NGUIRAZE NET\n\nAPLICAÇÃO A FUNCIONAR!"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(9, 9, 11))
        }

        setContentView(tela)
    }
}
