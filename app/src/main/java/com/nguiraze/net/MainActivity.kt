
package com.nguiraze.net

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private val bg = Color.rgb(5, 8, 14)
    private val card = Color.rgb(14, 19, 28)
    private val white = Color.WHITE
    private val gray = Color.rgb(155, 165, 180)
    private val red = Color.rgb(230, 10, 35)
    private val green = Color.rgb(40, 200, 125)
    private val blue = Color.rgb(45, 135, 255)
    private val yellow = Color.rgb(240, 180, 50)

    private lateinit var content: LinearLayout
    private lateinit var navigation: LinearLayout
    private var paused = true

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility =
    View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this).apply {
            clipToPadding = false
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(24), dp(14), dp(20))
        }

        scroll.addView(content)

        root.addView(scroll, LinearLayout.LayoutParams(
            -1, 0, 1f
        ))

        navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(10, 14, 22))
            setPadding(dp(5), dp(7), dp(5), dp(7))
        }

        root.addView(navigation, LinearLayout.LayoutParams(
    -1, dp(65)
))

root.setOnApplyWindowInsetsListener { _, insets ->

    val bottomInset = insets.systemWindowInsetBottom

    navigation.setPadding(
        dp(5),
        dp(7),
        dp(5),
        dp(7) + bottomInset
    )

    val params = navigation.layoutParams
    params.height = dp(65) + bottomInset
    navigation.layoutParams = params

    insets
}

setContentView(root)
root.requestApplyInsets()

        showHome()
        updateNavigation("Início")
    }

    private fun label(
        value: String,
        size: Float = 14f,
        color: Int = white,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }
    }

    private fun space(height: Int) {
        content.addView(View(this), LinearLayout.LayoutParams(
            1, dp(height)
        ))
    }

    private fun section(value: String) {
        val t = label(value, 12f, gray, true)
        t.setPadding(dp(3), dp(15), dp(3), dp(8))
        content.addView(t)
    }

    private fun panel(
        padding: Int = 14,
        color: Int = card
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
            background = android.graphics.drawable.GradientDrawable().apply {
    setColor(color)
    cornerRadius = dp(18).toFloat()
    setStroke(dp(1), Color.rgb(32, 40, 52))
            }
        }
    }

    private fun addPanel(view: View, bottom: Int = 9) {
        content.addView(view, LinearLayout.LayoutParams(
            -1, -2
        ).apply {
            bottomMargin = dp(bottom)
        })
    }

    private fun horizontal(vararg views: View): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            views.forEach {
                addView(it, LinearLayout.LayoutParams(
                    0, -2, 1f
                ).apply {
                    marginEnd = dp(5)
                    marginStart = dp(5)
                })
            }
        }
    }

    private fun showHome() {
        content.removeAllViews()

        // CABEÇALHO
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(red)
        }

        header.addView(label("⚙", 25f, white, true))

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label("NGUIRAZE NET", 20f, white, true))
            addView(label("● Sistema em preparação", 11f, white))
        }

        header.addView(brand, LinearLayout.LayoutParams(
            0, -2, 1f
        ).apply { marginStart = dp(12) })

        header.addView(label("24H", 16f, white, true))
        addPanel(header, 12)

        // CONTROLO DA FILA
        val pauseCard = panel(12, Color.rgb(45, 10, 19))
        pauseCard.addView(label(
            if (paused) "FILA PAUSADA" else "FILA PREPARADA",
            13f, red, true
        ))
        pauseCard.addView(label(
            if (paused) "Pausada manualmente." else "Aguardando configuração.",
            11f, white
        ))

        val pauseButton = Button(this).apply {
            text = if (paused) "RETOMAR FILA" else "PAUSAR FILA"
            setTextColor(white)
            setBackgroundColor(red)
            setOnClickListener {
                paused = !paused
                showHome()
                updateNavigation("Início")
            }
        }

        pauseCard.addView(pauseButton, LinearLayout.LayoutParams(
            -1, dp(46)
        ).apply { topMargin = dp(8) })

        addPanel(pauseCard, 9)

        // BOT
        val bot = panel()
        bot.addView(label("🤖  BOT", 17f, green, true))
        bot.addView(label("Aguardando configuração", 12f, gray))
        addPanel(bot)

        // AUTOMAÇÃO
        val automation = panel()
        automation.addView(label(
            "⚙  Serviço de automação", 15f, white, true
        ))
        automation.addView(label(
            "Acessibilidade ainda não configurada",
            12f, gray
        ))
        addPanel(automation)

        // ATALHOS
        section("ATALHOS")

        shortcutRow(
            listOf(
                "▥\nResumo" to green,
                "♙\nSIMs" to red,
                "◉\nCredencial" to blue,
                "ⓢ\nCrédito" to yellow
            )
        )

        shortcutRow(
            listOf(
                "⚙\nAvançado" to yellow,
                "➤\nSMS" to green,
                "⌛\nAgendar" to yellow
            )
        )

        // USSD
        val ussd = panel()
        ussd.addView(label("▶  Velocidade do USSD", 14f, white, true))
        ussd.addView(label("Modo: Automático • Por configurar", 11f, gray))
        addPanel(ussd)

        // ESTATÍSTICAS
        section("RESUMO DAS TRANSFERÊNCIAS")

        addPanel(horizontal(
            statistic("☰", "Na fila", "0", red),
            statistic("✓", "Concluídas", "0", green)
        ))

        // META
        section("META DIÁRIA")

        val goal = panel()
        goal.addView(label("🎯  Meta de hoje", 14f, white, true))
        goal.addView(label("0 MT • 0% concluído", 12f, gray))
        goal.addView(label("Meta ainda não configurada", 11f, gray))
        addPanel(goal)

        // ÚLTIMA VENDA
        section("ÚLTIMA VENDA")

        val sale = panel()
        sale.addView(label("Nenhuma venda registada", 13f, gray))
        addPanel(sale)

        // VENDAS POR HORA
        section("VENDAS POR HORA (HOJE)")

        val chart = panel()
        chart.addView(label(
            "Ainda não existem vendas registadas.",
            12f, gray
        ))
        chart.addView(label(
            "0 vendas • Faturação: 0 MT",
            12f, green
        ))
        addPanel(chart)

        // SIM
        section("CARTÕES SIM")

        addPanel(horizontal(
            simCard("SIM 1"),
            simCard("SIM 2")
        ))

        // FILA DE ESPERA
        section("FILA DE ESPERA")

        val queue = panel()
        queue.addView(label("◉  Sem pedidos na fila", 13f, gray))
        addPanel(queue)

        // EQUIPA
        section("EQUIPA")

        val team = panel()
        team.addView(label("Área de equipa", 13f, gray))
        addPanel(team)
    }

    private fun shortcutRow(items: List<Pair<String, Int>>) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        items.forEach { item ->
            val tile = TextView(this).apply {
                text = item.first
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(item.second)
                setTypeface(null, Typeface.BOLD)
                setPadding(dp(2), dp(10), dp(2), dp(10))
                setBackgroundColor(card)
            }

            row.addView(tile, LinearLayout.LayoutParams(
                0, dp(65), 1f
            ).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
                bottomMargin = dp(7)
            })
        }

        content.addView(row)
    }

    private fun statistic(
        icon: String,
        name: String,
        number: String,
        color: Int
    ): LinearLayout {
        val box = panel()
        box.addView(label("$icon  $name", 12f, color, true))
        box.addView(label(number, 25f, white, true))
        return box
    }

    private fun simCard(name: String): LinearLayout {
        val box = panel(11)
        box.addView(label(name, 13f, green, true))
        box.addView(label("0 / 10", 23f, white, true))
        box.addView(label("Transferências", 10f, gray))
        box.addView(label("Limite diário: 10", 10f, gray))
        return box
    }

    private fun showPage(name: String) {
        content.removeAllViews()
        space(25)
        content.addView(label("NGUIRAZE NET", 21f, red, true))
        space(20)
        content.addView(label(name, 25f, white, true))
        space(12)

        val box = panel(20)
        box.addView(label("Área de $name", 16f, white, true))
        box.addView(label(
            "Esta secção será desenvolvida e configurada.",
            13f, gray
        ))
        addPanel(box)
    }

    private fun updateNavigation(selected: String) {
        navigation.removeAllViews()

        val names = listOf(
            "Início", "Fila", "Conclusão",
            "Pagamentos", "Config"
        )

        val icons = listOf("⌂", "☷", "✓", "ⓢ", "⚙")

        names.forEachIndexed { index, name ->
            val active = name == selected

            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(2), dp(5), dp(2), dp(5))
                setBackgroundColor(
                    if (active) red else Color.TRANSPARENT
                )

                addView(label(
                    icons[index], 20f,
                    if (active) white else gray, true
                ).apply { gravity = Gravity.CENTER })

                addView(label(
                    name, 9f,
                    if (active) white else gray, active
                ).apply { gravity = Gravity.CENTER })

                setOnClickListener {
                    if (name == "Início") showHome()
                    else showPage(name)
                    updateNavigation(name)
                }
            }

            navigation.addView(item, LinearLayout.LayoutParams(
                0, -1, 1f
            ).apply {
                marginStart = dp(2)
                marginEnd = dp(2)
            })
        }
    }
}
