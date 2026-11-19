
package com.nguiraze.net

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*

data class Transferencia(
    val numeroCliente: String,
    val quantidadeMB: Int,
    val executor: String,
    val dataHora: String,
    val estado: String
)

data class Pedido(
    val numeroCliente: String,
    val quantidadeMB: Int,
    val executor: String,
    val dataHora: String,
    val estado: String
)


data class Executor(
    val nome: String,
    var estado: String,
    var vendasConcluidas: Int = 0,
    var totalMBEnviados: Long = 0,
    var enviosEmCurso: Int = 0,
    var falhas: Int = 0
)

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

    private val transferencias = mutableListOf<Transferencia>()
    private val pedidosFila = mutableListOf<Pedido>()
    private val executores = mutableListOf(
    Executor("EXECUTOR 1", "OFFLINE"),
    Executor("EXECUTOR 2", "OFFLINE")
)

    private fun registarTransferencia(
        numero: String,
        mb: Int,
        executor: String,
        dataHora: String,
        estado: String
    ) {
        transferencias.add(
            Transferencia(
                numeroCliente = numero,
                quantidadeMB = mb,
                executor = executor,
                dataHora = dataHora,
                estado = estado
            )
        )
    }

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

            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(card)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.rgb(32, 40, 52))
            }

            setOnClickListener {
                if (item.first.contains("Resumo")) {
                    showPage("Resumo")
                }
            }
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

    if (name == "Fila") {

        space(20)
        content.addView(label("NGUIRAZE NET", 21f, red, true))
        space(18)

        // PAINEL DO EXECUTOR
        val executorBox = panel(18)

        executorBox.addView(
            label("PAINEL DO EXECUTOR", 17f, white, true)
        )
        space(12)

        executorBox.addView(
            label("Executor 1", 15f, white, true)
        )
        executorBox.addView(
            label("Executor 2", 15f, white, true)
        )

        addPanel(executorBox)
        space(14)

        // PESQUISA
        val searchBox = panel(14)

        val search = android.widget.EditText(this).apply {
            hint = "Pesquisar número ou executor..."
            setHintTextColor(gray)
            setTextColor(white)
            textSize = 14f
            maxLines = 1
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(20, 27, 38))
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.rgb(45, 55, 70))
            }
        }

        searchBox.addView(
            search,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        addPanel(searchBox)
        
        space(10)

        // INDICADORES EM UMA ÚNICA FILA

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun statusCard(title: String, color: Int): LinearLayout {
            val card = panel(8)
            card.gravity = android.view.Gravity.CENTER

            card.addView(label("0", 20f, color, true))
            card.addView(label(title, 10f, gray))

            return card
        }

        val statusParams = LinearLayout.LayoutParams(
            0, dp(86), 1f
        ).apply {
            setMargins(dp(3), 0, dp(3), 0)
        }

        statusRow.addView(statusCard("Online", green), statusParams)
        statusRow.addView(statusCard("Em curso", blue), statusParams)
        statusRow.addView(statusCard("Em espera", yellow), statusParams)
        statusRow.addView(statusCard("Bloqueados", red), statusParams)

        content.addView(statusRow)
        
        space(18)

        // TITULO ENVIO MANUAL
        content.addView(
            label("➤  ENVIO MANUAL", 14f, white, true)
        )
        space(10)

        // CARTAO ENVIO MANUAL
        val manualBox = panel(14)
        fun innerSpace(height: Int) {
    manualBox.addView(
        View(this),
        LinearLayout.LayoutParams(1, dp(height))
    )
        }

        manualBox.addView(
            label("➤  Envio manual", 16f, white, true)
        )

        manualBox.addView(
            label(
                "Despacha diretamente para um Agente livre",
                12f, gray
            )
        )

        innerSpace(18)

        manualBox.addView(
            label("PACOTES RÁPIDOS", 12f, gray, true)
        )

        innerSpace(8)

        // CAMPOS DE ENTRADA
        val numberInput = android.widget.EditText(this).apply {
            hint = "Número do cliente"
            setHintTextColor(gray)
            setTextColor(white)
            textSize = 14f
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(5, 5, 5))
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.rgb(35, 42, 54))
            }
        }

        val mbInput = android.widget.EditText(this).apply {
            hint = "Megabytes"
            setHintTextColor(gray)
            setTextColor(white)
            textSize = 14f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(5, 5, 5))
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.rgb(35, 42, 54))
            }
        }

        // PACOTE RAPIDO
        val quickRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val quickPackage = android.widget.Button(this).apply {
            text = "450MB  ·  10 MT"
            textSize = 12f
            isAllCaps = false
            setTextColor(white)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.rgb(45, 55, 70))
            }
            setOnClickListener {
                mbInput.setText("450")
            }
        }

        quickRow.addView(quickPackage)
        manualBox.addView(quickRow)

        innerSpace(12)

        // NUMERO DO CLIENTE
        manualBox.addView(
            numberInput,
            LinearLayout.LayoutParams(-1, dp(50))
        )

        innerSpace(10)

        // QUANTIDADE DE MB
        manualBox.addView(
            mbInput,
            LinearLayout.LayoutParams(-1, dp(50))
        )

        innerSpace(8)

        manualBox.addView(
            label("Aceite entre 100MB e 10240MB (10GB).", 11f, gray)
        )

        innerSpace(14)

        // BOTAO DESPACHAR
        
val dispatchButton = android.widget.Button(this).apply {
    text = "➤  Despachar para um Agente"
    textSize = 13f
    isAllCaps = false
    setTextColor(white)
    background = android.graphics.drawable.GradientDrawable().apply {
        setColor(Color.rgb(15, 22, 32))
        cornerRadius = dp(18).toFloat()
        setStroke(dp(1), Color.rgb(40, 50, 65))
    }

    setOnClickListener {
        val numero = numberInput.text.toString().trim()
        val megas = mbInput.text.toString().trim()
        val quantidade = megas.toIntOrNull()

        if (numero.isEmpty() || megas.isEmpty()) {
            android.widget.Toast.makeText(
                this@MainActivity,
                "Preencha o número e os megabytes.",
                android.widget.Toast.LENGTH_SHORT
            ).show()

        } else if (quantidade == null || quantidade !in 100..10240) {
            android.widget.Toast.makeText(
                this@MainActivity,
                "A quantidade deve estar entre 100 MB e 10 GB.",
                android.widget.Toast.LENGTH_SHORT
            ).show()

        } else {
            val dataHora = android.text.format.DateFormat.format(
                "dd/MM/yyyy HH:mm",
                java.util.Date()
            ).toString()

            pedidosFila.add(
                Pedido(
                    numeroCliente = numero,
                    quantidadeMB = quantidade,
                    executor = "Por atribuir",
                    dataHora = dataHora,
                    estado = "Em fila"
                )
            )

            android.widget.Toast.makeText(
                this@MainActivity,
                "Pedido adicionado à fila de espera!",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            showPage("Fila")
        }
    }
}

manualBox.addView(
    dispatchButton,
    LinearLayout.LayoutParams(-1, dp(50))
)


        addPanel(manualBox)
                space(20)

        
content.addView(
    label("➤  EXECUTORES", 14f, white, true)
)

space(10)

val addExecutorButton = Button(this).apply {
    text = "＋  ADICIONAR EXECUTOR"
    textSize = 13f
    isAllCaps = false
    setTextColor(white)

    background = android.graphics.drawable.GradientDrawable().apply {
        setColor(Color.rgb(15, 22, 32))
        cornerRadius = dp(14).toFloat()
        setStroke(dp(1), red)
    }

    setOnClickListener {

        val proximoNumero = executores.size + 1

        val nomeInput = EditText(this@MainActivity).apply {
            setText("EXECUTOR $proximoNumero")
            setSingleLine(true)
            setTextColor(white)
            hint = "Nome do executor"
        }

        android.app.AlertDialog.Builder(this@MainActivity)
            .setTitle("NOVO EXECUTOR")
            .setMessage("Digite o nome do executor:")
            .setView(nomeInput)
            .setNegativeButton("CANCELAR", null)
            .setPositiveButton("ADICIONAR") { _, _ ->

                val nome = nomeInput.text.toString().trim()

                if (nome.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "Digite o nome do executor.",
                        Toast.LENGTH_SHORT
                    ).show()

                } else if (executores.any {
                    it.nome.equals(nome, ignoreCase = true)
                }) {
                    Toast.makeText(
                        this@MainActivity,
                        "Este executor já existe.",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {
                    executores.add(
                        Executor(nome, "OFFLINE")
                    )

                    showPage("Fila")
                }
            }
            .show()
    }
}

content.addView(
    addExecutorButton,
    LinearLayout.LayoutParams(-1, dp(50))
)

space(12)


val executorsContainer = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
}

executores.forEach { executor ->

    val executorBox = panel(18)

    val estadoCor = when (executor.estado) {
        "LIVRE" -> green
        "OCUPADO" -> yellow
        else -> gray
    }

    val header = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    header.addView(
        label(executor.nome, 16f, white, true),
        LinearLayout.LayoutParams(0, -2, 1f)
    )

    header.addView(
        label("● ${executor.estado}", 11f, estadoCor, true)
    )

    executorBox.addView(header)

    val statsRow1 = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    setPadding(0, dp(18), 0, 0)
    }

    fun addStat(
        linha: LinearLayout,
        valor: String,
        descricao: String
    ) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        item.addView(label(valor, 22f, white, true))
        item.addView(label(descricao, 10f, gray))

        linha.addView(
            item,
            LinearLayout.LayoutParams(0, -2, 1f)
        )
    }



    addStat(
        statsRow1,
        executor.vendasConcluidas.toString(),
        "Vendas concluídas"
    )

    addStat(
        statsRow1,
        "${executor.totalMBEnviados} MB",
        "Total de MB enviados"
    )

    executorBox.addView(statsRow1)



    val statsRow2 = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    setPadding(0, dp(18), 0, 0)
    }

    addStat(
        statsRow2,
        executor.enviosEmCurso.toString(),
        "Envios em curso"
    )

    addStat(
        statsRow2,
        executor.falhas.toString(),
        "Falhas"
    )

    executorBox.addView(statsRow2)

    executorsContainer.addView(
        executorBox,
        LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(12)
        }
    )
}

content.addView(executorsContainer)


                // EM CURSO AGORA
        space(20)

        content.addView(
            label("➤  EM CURSO AGORA", 14f, white, true)
        )

        space(10)

                val inProgressBox = panel(14)

        inProgressBox.addView(
            label("●  NENHUM ENVIO EM CURSO", 13f, gray, true)
        )

        inProgressBox.addView(
            label(
                "Os envios ativos aparecerão aqui.",
                12f, gray
            )
        )

        addPanel(inProgressBox)
                // FILA DE ESPERA
        space(20)

        content.addView(
            label("⏳  FILA DE ESPERA", 14f, white, true)
        )

        space(10)

                
val waitingBox = panel(14)

if (pedidosFila.isEmpty()) {

    waitingBox.addView(
        label("⏳  NENHUM PEDIDO NA FILA", 13f, gray, true)
    )

    waitingBox.addView(
        label(
            "Os pedidos aguardando envio aparecerão aqui.",
            12f, gray
        )
    )

} else {

    for (pedido in pedidosFila) {

        waitingBox.addView(
            label("📱 ${pedido.numeroCliente}", 14f, white, true)
        )

        waitingBox.addView(
            label("📦 ${pedido.quantidadeMB} MB", 12f, gray)
        )

        waitingBox.addView(
            label("Executor: ${pedido.executor}", 12f, gray)
        )

        waitingBox.addView(
            label("Estado: ${pedido.estado}", 12f, yellow, true)
        )

        waitingBox.addView(
            label("Data: ${pedido.dataHora}", 11f, gray)
        )

        waitingBox.addView(
            View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    1, dp(8)
                )
            }
        )
    }
}

  
        addPanel(waitingBox)
        
       // HISTÓRICO RECENTE
        space(20)

        content.addView(
            label("➤  HISTÓRICO RECENTE", 14f, white, true)
        )

        space(10)

                val historyBox = panel(14)

        historyBox.addView(
            label("SEM REGISTOS RECENTES", 13f, gray, true)
        )

        historyBox.addView(
            label(
                "Os envios concluídos aparecerão aqui.",
                12f, gray
            )
        )

        
        val historyButton = android.widget.Button(this).apply {
            text = "Ver Todos"
            textSize = 12f
            isAllCaps = false
            setTextColor(white)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(20, 27, 38))
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener {
                showPage("Conclusão")
                updateNavigation("Conclusão")
            }
        }

        historyBox.addView(
            historyButton,
            LinearLayout.LayoutParams(-1, dp(42))
        )

        addPanel(historyBox)

    
    
    
} else if (name == "Conclusão") {

    space(25)
    content.addView(label("NGUIRAZE NET", 21f, red, true))
    space(18)

    content.addView(label("CONCLUSÃO", 23f, white, true))
    space(15)

    var selectedStatus = "TODOS"

    val statusRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
    }

    val statusNames = listOf(
        "TODOS",
        "CONCLUÍDOS",
        "ERROS",
        "INCERTO",
        "EM FILA"
    )

    val statusColors = listOf(
        white, green, red, yellow, blue
    )

    space(22)
    content.addView(
        label("➤  TRANSFERÊNCIAS", 15f, white, true)
    )
    space(10)

    val transfersBox = panel(16)

    val filterLabel = label(
        "FILTRO: TODOS", 12f, red, true
    )

    val emptyStatus = label(
        "NENHUMA TRANSFERÊNCIA", 13f, gray, true
    )

    val emptyDescription = label(
        "As transferências realizadas aparecerão aqui.",
        12f, gray
    )

    val recordsContainer = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
    }

    transfersBox.addView(
        label("HISTÓRICO DE TRANSFERÊNCIAS", 13f, white, true)
    )

    transfersBox.addView(filterLabel)
    transfersBox.addView(recordsContainer)

    addPanel(transfersBox)

    val countLabels = mutableListOf<TextView>()

    fun corresponde(item: Transferencia, filtro: String): Boolean {

        if (filtro == "TODOS") return true

        return when (filtro) {

            "CONCLUÍDOS" ->
                item.estado.equals("Concluída", true) ||
                item.estado.equals("Concluído", true)

            "ERROS" ->
                item.estado.equals("Erro", true)

            "INCERTO" ->
                item.estado.equals("Incerta", true) ||
                item.estado.equals("Incerto", true)

            "EM FILA" ->
                item.estado.equals("Em fila", true)

            else -> false
        }
    }

    fun atualizarHistorico() {

        // ACTUALIZAR CONTADORES
        statusNames.forEachIndexed { index, nome ->

            val total = transferencias.count {
                corresponde(it, nome)
            }

            countLabels[index].text = total.toString()
        }

        // ACTUALIZAR HISTÓRICO
        recordsContainer.removeAllViews()

        val filtradas = transferencias.filter {
            corresponde(it, selectedStatus)
        }

        if (filtradas.isEmpty()) {

            emptyStatus.text =
                if (selectedStatus == "TODOS")
                    "NENHUMA TRANSFERÊNCIA"
                else
                    "NENHUMA TRANSFERÊNCIA EM $selectedStatus"

            recordsContainer.addView(emptyStatus)
            recordsContainer.addView(emptyDescription)

        } else {

            filtradas.reversed().forEach { item ->

                val recordBox = panel(12)

                recordBox.addView(
                    label(
                        "CLIENTE: ${item.numeroCliente}",
                        13f, white, true
                    )
                )

                recordBox.addView(
                    label(
                        "QUANTIDADE: ${item.quantidadeMB} MB",
                        12f, gray
                    )
                )

                recordBox.addView(
                    label(
                        "EXECUTOR: ${item.executor}",
                        12f, gray
                    )
                )

                recordBox.addView(
                    label(
                        "DATA/HORA: ${item.dataHora}",
                        11f, gray
                    )
                )

                recordBox.addView(
                    label(
                        "ESTADO: ${item.estado}",
                        12f, green, true
                    )
                )

                recordsContainer.addView(recordBox)
                space(8)
            }
        }
    }

    // CRIAR INDICADORES
    statusNames.forEachIndexed { index, statusName ->

        val statusBox = panel(8)

        statusBox.addView(
            label(statusName, 9f, statusColors[index], true)
        )

        val countText = label("0", 22f, white, true)

        countLabels.add(countText)
        statusBox.addView(countText)

        statusBox.alpha =
            if (statusName == selectedStatus) 1f else 0.65f

        statusBox.setOnClickListener {

            selectedStatus = statusName

            filterLabel.text = "FILTRO: $selectedStatus"

            for (i in 0 until statusRow.childCount) {
                statusRow.getChildAt(i).alpha = 0.65f
            }

            statusBox.alpha = 1f

            atualizarHistorico()
        }

        statusRow.addView(
            statusBox,
            LinearLayout.LayoutParams(
                0,
                dp(76),
                1f
            ).apply {
                setMargins(dp(2), 0, dp(2), 0)
            }
        )
    }

    content.addView(
        statusRow,
        content.indexOfChild(transfersBox)
    )

    atualizarHistorico()


} else if (name == "SIMs") {

    space(25)
    content.addView(label("NGUIRAZE NET", 21f, red, true))
    space(18)

    content.addView(label("GESTÃO DE SIMs", 23f, white, true))
    space(8)

    content.addView(
        label("Controlo de envios por cartão SIM", 12f, gray)
    )
    space(18)

    fun simCard(numero: String, usados: Int, ativo: Boolean) {
        val box = panel(18)

        box.addView(
            label(numero, 18f, white, true)
        )

        space(8)

        box.addView(
            label(
                "$usados / 10 ENVIO(S)",
                22f,
                if (usados >= 10) red else green,
                true
            )
        )

        val estado = when {
            usados >= 10 -> "LIMITE ATINGIDO"
            ativo -> "SIM SELECIONADO"
            else -> "DISPONÍVEL"
        }

        box.addView(
            label(
                estado,
                12f,
                if (usados >= 10) red else if (ativo) green else gray,
                true
            )
        )

        space(12)

        val usarButton = Button(this).apply {
            text = if (ativo) "SIM SELECIONADO" else "USAR ESTE SIM"
            isAllCaps = false
            setTextColor(white)
            isEnabled = usados < 10 && !ativo

            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(15, 22, 32))
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), if (ativo) green else red)
            }
        }

        box.addView(
            usarButton,
            LinearLayout.LayoutParams(-1, dp(48))
        )

        addPanel(box)
        space(14)
    }

    simCard("SIM 1", 0, true)
    simCard("SIM 2", 0, false)

    val infoBox = panel(16)
    infoBox.addView(
        label("INFORMAÇÃO", 14f, yellow, true)
    )
    infoBox.addView(
        label(
            "O contador será atualizado apenas após confirmação de sucesso da operadora. A gestão automática será ligada posteriormente.",
            12f, gray
        )
    )
    addPanel(infoBox)

} else {

    space(25)
    content.addView(label("NGUIRAZE NET", 21f, red, true))
    space(20)
    content.addView(label(name, 25f, white, true))
    space(12)

    val box = panel(20)

    box.addView(
        label("Área de $name", 16f, white, true)
    )

    box.addView(
        label(
            "Esta secção será desenvolvida e configurada.",
            13f, gray
        )
    )

    addPanel(box)
}
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

            addView(
                label(
                    icons[index], 20f,
                    if (active) white else gray, true
                ).apply {
                    gravity = Gravity.CENTER
                }
            )

            addView(
                label(
                    name, 9f,
                    if (active) white else gray, active
                ).apply {
                    gravity = Gravity.CENTER
                }
            )

            setOnClickListener {
                if (name == "Início") {
                    showHome()
                } else {
                    showPage(name)
                }

                updateNavigation(name)
            }
        }

        navigation.addView(
            item,
            LinearLayout.LayoutParams(0, -1, 1f).apply {
                marginStart = dp(2)
                marginEnd = dp(2)
            }
        )
    }
}
}
