package com.nguiraze.net

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private val bg = Color.rgb(6, 9, 14)
    private val card = Color.rgb(14, 19, 27)
    private val red = Color.rgb(225, 10, 25)
    private val green = Color.rgb(0, 210, 115)
    private val white = Color.WHITE
    private val gray = Color.rgb(145, 155, 170)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = red
        window.navigationBarColor = bg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        // 1. Cabeçalho Vermelho
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(red)
            setPadding(dp(18), dp(16), dp(18), dp(16))
            addView(txt("NGUIRAZE NET", 19f, white, true))
            addView(txt("● Conectado · Automação USSD", 12f, Color.rgb(255, 220, 220)))
        })

        // 2. Corpo do Painel
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        body.addView(box(green, "BOT ONLINE ● ATIVO", "Velocidade USSD: Ultra"))
        body.addView(box(red, "NA FILA: 0", "CONCLUÍDAS: 4"))
        body.addView(box(Color.rgb(32, 42, 54), "META DIÁRIA: 225 MT / 500 MT", "Faltam 275 MT"))
        body.addView(box(green, "SIM 1 (VODACOM) · ACTIVO", "0 / 10 transferências · Saldo: 1179.55 TB"))
        body.addView(box(Color.rgb(32, 42, 54), "SIM 2 (MOVITEL)", "0 / 10 transferências"))

        root.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(-1, 0, 1f))

        // 3. Barra Inferior (5 Abas)
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(11, 15, 22))
            setPadding(dp(6), dp(8), dp(6), dp(8))
        }
        listOf("Início", "Fila", "Conclusão", "SIMs", "Config").forEachIndexed { i, nome ->
            nav.addView(txt(nome, 11f, if (i == 0) white else gray, i == 0).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(8))
                if (i == 0) background = GradientDrawable().apply {
                    setColor(red); cornerRadius = dp(10).toFloat()
                }
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(nav)

        setContentView(root)
    }

    private fun box(stroke: Int, titulo: String, sub: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
        background = GradientDrawable().apply {
            setColor(card); cornerRadius = dp(14).toFloat(); setStroke(dp(1), stroke)
        }
        addView(txt(titulo, 15f, white, true))
        addView(txt(sub, 12f, gray))
    }

    private fun txt(t: String, s: Float, c: Int, b: Boolean = false) = TextView(this).apply {
        text = t; textSize = s; setTextColor(c)
        if (b) setTypeface(null, Typeface.BOLD)
    }
}
