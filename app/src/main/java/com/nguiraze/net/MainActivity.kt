package com.nguiraze.net

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================
// 1. MODELOS
// ============================================================

data class PedidoFila(
    val id: String,
    val numeroCliente: String,
    val megas: Int,
    val hora: String
)

data class TransacaoConcluida(
    val numeroCliente: String,
    val megas: Int,
    val valorMT: Int,
    val hora: String,
    val simInfo: String,
    val status: String // Entregue, Falhou ou Incerto
)

data class AgenteExecutor(
    val id: Int,
    val nome: String,
    val sigla: String,
    var online: Boolean,
    var vendas: Int,
    val simTag: String
)

// ============================================================
// 2. ACTIVITY PRINCIPAL
// ============================================================

class MainActivity : Activity() {

    // -------------------- CORES --------------------

    private val bgTop = Color.rgb(5, 9, 16)
    private val bgBot = Color.rgb(5, 9, 16)
    private val cardNavy = Color.rgb(14, 20, 30)
    private val cardDark = Color.rgb(12, 18, 27)
    private val borderBlue = Color.rgb(29, 40, 55)

    private val cyan = Color.rgb(65, 133, 245)
    private val green = Color.rgb(48, 198, 128)
    private val greenBg = Color.rgb(13, 49, 39)
    private val yellow = Color.rgb(244, 185, 55)
    private val red = Color.rgb(235, 70, 75)

    private val white = Color.WHITE
    private val lightBlue = Color.rgb(151, 161, 177)

    // -------------------- ELEMENTOS PRINCIPAIS --------------------

    private lateinit var content: LinearLayout
    private lateinit var navBar: LinearLayout

    private var abaAtual = "Início"

    // -------------------- DADOS DA APLICAÇÃO --------------------

    private val concluidos = mutableListOf<TransacaoConcluida>()
    private val filaEspera = mutableListOf<PedidoFila>()
    private val executores = mutableListOf<AgenteExecutor>()

    private var pedidoEmCurso: PedidoFila? = null

    // -------------------- CONTROLO DOS SIMs --------------------

    private var simSelecionado = 1
    private var sim1Usados = 0
    private var sim2Usados = 0
    private val limiteSim = 10

    private var filtroConclusao = "Todos"
    private var filaPausada = false
    private var metaGb = 10

    private val preferencias by lazy {
        getSharedPreferences("nguiraze_net_dados", Context.MODE_PRIVATE)
    }

    // ============================================================
    // 3. INICIALIZAÇÃO
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        carregarContadores()
        verificarNovoDia()

        window.statusBarColor = bgTop
        window.navigationBarColor = bgBot

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgTop)
        }

        val scroll = ScrollView(this).apply {
            clipToPadding = false
            isFillViewport = true
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(24))
        }

        scroll.addView(
            content,
            ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        navBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(6, 16, 40))
            setPadding(dp(4), dp(6), dp(4), dp(6))
        }

        root.addView(
            navBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(64)
            )
        )

        setContentView(root)
        navegarPara("Início")
    }

    // ============================================================
    // 4. NAVEGAÇÃO
    // ============================================================

    private fun navegarPara(aba: String) {
        abaAtual = aba
        content.removeAllViews()

        when (aba) {
            "Início" -> telaInicio()
            "Fila" -> telaFilaExecutores()
            "Concluídos" -> telaConcluidos()
            "SIMs" -> telaSims()
            "Config" -> telaConfig()
            else -> telaInicio()
        }

        renderizarBarraInferior()
    }

    // ============================================================
    // 5. TELA INICIAL
    // ============================================================

    private fun telaInicio() {
        val entreguesHoje = concluidos.filter { it.status == "Entregue" }
        val totalConcluidas = entreguesHoje.size
        val totalMegas = entreguesHoje.sumBy { it.megas }
        val gbCalculados = totalMegas / 1024.0

        val progresso = if (metaGb > 0) {
            (gbCalculados / metaGb.toDouble()).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

        val gbStr = String.format(Locale("pt", "MZ"), "%.2f", gbCalculados)

        // CABEÇALHO
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val avatar = txt("🤖", 19f, white).apply {
            gravity = Gravity.CENTER
            background = oval(Color.rgb(12, 42, 95), cyan)
        }

        header.addView(avatar, LinearLayout.LayoutParams(dp(42), dp(42)))

        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)

            addView(txt("NGUIRAZE NET", 16f, cyan, true))
            addView(txt("Versão instalada: 1.86 (86)", 11f, lightBlue))
        }

        header.addView(titleCol, LinearLayout.LayoutParams(0, -2, 1f))

        header.addView(
            pillBadge(
                "● USSD não ligado",
                yellow,
                Color.rgb(55, 43, 12),
                yellow
            )
        )

        content.addView(header)

        // CARTÃO DE RESUMO
        val summaryCard = cardBox(cardNavy, borderBlue).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(
                RingProgressView(
                    this@MainActivity,
                    progresso,
                    gbStr,
                    "GB VENDIDOS"
                ),
                LinearLayout.LayoutParams(dp(140), dp(140)).apply {
                    rightMargin = dp(6)
                }
            )

            val pedidosCircle = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = oval(Color.rgb(8, 48, 39), green)

                addView(
                    txt("$totalConcluidas", 25f, white, true).apply {
                        gravity = Gravity.CENTER
                    }
                )
                addView(txt("TOTAL", 10f, green, true).apply {
                    gravity = Gravity.CENTER
                })
                addView(txt("ATENDIDOS", 10f, green, true).apply {
                    gravity = Gravity.CENTER
                })
            }

            addView(
                pedidosCircle,
                LinearLayout.LayoutParams(dp(140), dp(140)).apply {
                    leftMargin = dp(12)
                }
            )
        }

        content.addView(summaryCard)

        // ATALHOS
        val iconsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(6)
                bottomMargin = dp(16)
            }
        }

        val atalhos = listOf(
            Triple("➤", "Enviar megas", { navegarPara("Fila") }),
            Triple("☰", "Fila", { navegarPara("Fila") }),
            Triple("▤", "Histórico", { navegarPara("Concluídos") }),
            Triple("◉", "Meta", {
                Toast.makeText(
                    this,
                    "Meta diária: $metaGb GB",
                    Toast.LENGTH_SHORT
                ).show()
            })
        )

        atalhos.forEach { item ->
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER

                val circle = txt(item.first, 17f, white, true).apply {
                    gravity = Gravity.CENTER
                    background = oval(
                        Color.rgb(14, 65, 165),
                        Color.rgb(45, 135, 255)
                    )
                }

                addView(circle, LinearLayout.LayoutParams(dp(52), dp(52)))
                addView(
                    txt(item.second, 11f, white, true).apply {
                        gravity = Gravity.CENTER
                        setPadding(0, dp(5), 0, 0)
                    }
                )

                setOnClickListener { item.third() }
            }

            iconsRow.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        }

        content.addView(iconsRow)

        // CABEÇALHO DOS SIMs
        val simHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(8)
            }

            addView(
                txt("CARTÕES SIM", 13f, lightBlue, true),
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            addView(
                pillBadge("+ Gerir SIMs", cyan, cardDark, cyan).apply {
                    setOnClickListener { navegarPara("SIMs") }
                }
            )
        }

        content.addView(simHeader)

        // CARTÕES SIM
        val simsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(6)
            }
        }

        fun miniSimBox(numero: Int, usados: Int): LinearLayout {
            val ativo = simSelecionado == numero

            return cardBox(
                if (ativo) Color.rgb(10, 38, 36) else cardDark,
                if (ativo) green else borderBlue
            ).apply {
                val top = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL

                    addView(
                        txt(
                            "SIM $numero",
                            12f,
                            if (ativo) green else lightBlue,
                            true
                        ),
                        LinearLayout.LayoutParams(0, -2, 1f)
                    )

                    if (ativo) addView(txt("●", 12f, green))
                }

                addView(top)
                addView(txt("$usados/$limiteSim", 22f, white, true))
                addView(
                    txt("transferências", 11f, lightBlue).apply {
                        setPadding(0, 0, 0, dp(8))
                    }
                )

                val btns = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                if (!ativo) {
                    btns.addView(
                        smallActionBtn("Usar", green) {
                            simSelecionado = numero
                            guardarContadores()
                            navegarPara("Início")
                        },
                        LinearLayout.LayoutParams(0, dp(30), 1f).apply {
                            rightMargin = dp(4)
                        }
                    )
                }

                btns.addView(
                    smallActionBtn("Reset", yellow) {
                        if (numero == 1) sim1Usados = 0 else sim2Usados = 0
                        guardarContadores()
                        navegarPara("Início")
                    },
                    LinearLayout.LayoutParams(0, dp(30), 1f)
                )

                addView(btns)
            }
        }

        simsRow.addView(
            miniSimBox(1, sim1Usados),
            LinearLayout.LayoutParams(0, -2, 1f).apply {
                rightMargin = dp(6)
            }
        )

        simsRow.addView(
            miniSimBox(2, sim2Usados),
            LinearLayout.LayoutParams(0, -2, 1f).apply {
                leftMargin = dp(6)
            }
        )

        content.addView(simsRow)

        content.addView(
            txt("Contadores diários dos cartões SIM", 11f, lightBlue).apply {
                setPadding(0, 0, 0, dp(10))
            }
        )

        // FILA
        val filaBar = cardBox(cardDark, borderBlue).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(
                txt("FILA DE ESPERA (${filaEspera.size})", 12f, lightBlue, true),
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            addView(
                smallActionBtn(
                    if (filaPausada) "▶ Retomar" else "❚❚ Pausar",
                    yellow
                ) {
                    filaPausada = !filaPausada
                    navegarPara("Início")
                },
                LinearLayout.LayoutParams(dp(95), dp(32))
            )
        }

        content.addView(filaBar)

        // ÚLTIMOS PEDIDOS
        content.addView(
            txt("Últimos pedidos", 15f, white, true).apply {
                setPadding(0, dp(4), 0, dp(8))
            }
        )

        val ultimos = concluidos
            .filter { it.status == "Entregue" }
            .take(3)

        if (ultimos.isEmpty()) {
            content.addView(
                txt(
                    "Ainda não existem transferências confirmadas.",
                    12f,
                    lightBlue
                ).apply {
                    setPadding(0, dp(4), 0, dp(8))
                }
            )
        } else {
            ultimos.forEach { content.addView(buildTransacaoRow(it)) }
        }
    }

    // ============================================================
    // 6. TELA FILA E EXECUTORES
    // ============================================================

    private fun telaFilaExecutores() {
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(10)
            }

            addView(
                txt("‹", 24f, white, true).apply {
                    setPadding(0, 0, dp(12), 0)
                    setOnClickListener { navegarPara("Início") }
                }
            )

            val titleCol = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL

                addView(txt("Fila & Executores", 18f, white, true))

                val pendentes = filaEspera.size +
                    (if (pedidoEmCurso != null) 1 else 0)
                val ativos = executores.count { it.online }

                addView(
                    txt(
                        "$pendentes pedidos pendentes · $ativos executores ativos",
                        11f,
                        lightBlue
                    )
                )
            }

            addView(titleCol, LinearLayout.LayoutParams(0, -2, 1f))

            addView(
                pillBadge(
                    if (filaPausada) "● Pausada" else "● Ativa",
                    if (filaPausada) yellow else green,
                    greenBg,
                    borderBlue
                )
            )
        }

        content.addView(topBar)

        val controlRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(12)
            }

            addView(
                smallActionBtn(
                    if (filaPausada) "▶ Retomar Fila" else "❚❚ Pausar Fila",
                    yellow
                ) {
                    filaPausada = !filaPausada
                    navegarPara("Fila")
                },
                LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    rightMargin = dp(6)
                }
            )

            addView(
                smallActionBtn("🗑 Limpar", red) {
                    filaEspera.clear()
                    navegarPara("Fila")
                },
                LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    leftMargin = dp(6)
                }
            )
        }

        content.addView(controlRow)

        content.addView(
            txt("EXECUTORES (${executores.size})", 12f, lightBlue, true).apply {
                setPadding(0, dp(4), 0, dp(8))
            }
        )

        if (executores.isEmpty()) {
            content.addView(
                txt("Nenhum executor configurado.", 12f, lightBlue).apply {
                    setPadding(0, 0, 0, dp(12))
                }
            )
        }

        val execRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(12)
            }
        }

        executores.forEachIndexed { index, agente ->
            val cardAg = cardBox(
                if (agente.online) Color.rgb(10, 36, 38) else cardDark,
                if (agente.online) green else borderBlue
            ).apply {
                addView(txt(agente.sigla, 13f, cyan, true))
                addView(txt(agente.nome, 13f, white, true))
                addView(txt("#${agente.id}", 11f, lightBlue))
                addView(
                    txt(
                        if (agente.online) "● Online" else "● Standby",
                        11f,
                        if (agente.online) green else yellow
                    ).apply {
                        setPadding(0, dp(6), 0, dp(6))
                    }
                )
                addView(txt("${agente.vendas} vendas", 12f, white, true))
                addView(txt(agente.simTag, 10f, lightBlue))
            }

            execRow.addView(
                cardAg,
                LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (index == 0) rightMargin = dp(5) else leftMargin = dp(5)
                }
            )
        }

        content.addView(execRow)

        content.addView(
            txt("⌛ EM CURSO AGORA", 12f, lightBlue, true).apply {
                setPadding(0, 0, 0, dp(6))
            }
        )

        val runningCard = cardBox(Color.rgb(8, 32, 72), cyan)
        val atual = pedidoEmCurso

        if (atual == null) {
            runningCard.addView(
                txt("Nenhuma ordem em curso no momento.", 12f, lightBlue)
            )
        } else {
            runningCard.addView(
                txt("${atual.numeroCliente} · ${atual.megas} MB", 15f, white, true)
            )
            runningCard.addView(
                txt(
                    "Pedido registado · Aguardando confirmação da operadora",
                    11f,
                    yellow
                )
            )
        }

        content.addView(runningCard)

        content.addView(
            txt("FILA DE ESPERA (${filaEspera.size})", 12f, lightBlue, true).apply {
                setPadding(0, dp(6), 0, dp(8))
            }
        )

        if (filaEspera.isEmpty()) {
            content.addView(
                txt("Não existem pedidos na fila de espera.", 12f, lightBlue).apply {
                    setPadding(dp(4), dp(8), dp(4), dp(12))
                }
            )
        }

        filaEspera.forEachIndexed { index, pedido ->
            val linha = cardBox(cardDark, borderBlue).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val info = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(
                        txt(
                            "#${index + 1} · ${pedido.numeroCliente}",
                            14f,
                            white,
                            true
                        )
                    )
                    addView(
                        txt("${pedido.megas} MB · ${pedido.hora}", 11f, lightBlue)
                    )
                }

                addView(info, LinearLayout.LayoutParams(0, -2, 1f))

                addView(
                    smallActionBtn("▲ Subir", cyan) {
                        if (index > 0) {
                            val item = filaEspera.removeAt(index)
                            filaEspera.add(index - 1, item)
                            navegarPara("Fila")
                        }
                    },
                    LinearLayout.LayoutParams(dp(72), dp(30)).apply {
                        rightMargin = dp(6)
                    }
                )

                addView(
                    smallActionBtn("✕ Remover", red) {
                        filaEspera.removeAt(index)
                        navegarPara("Fila")
                    },
                    LinearLayout.LayoutParams(dp(84), dp(30))
                )
            }

            content.addView(linha)
        }
    }

    // ============================================================
    // 7. TELA DE HISTÓRICO
    // ============================================================

    private fun telaConcluidos() {
        content.addView(txt("Conclusão", 19f, white, true))

        content.addView(
            txt("Histórico de transferências", 12f, lightBlue).apply {
                setPadding(0, 0, 0, dp(14))
            }
        )

        val filtros = listOf("Todos", "Entregue", "Falhou", "Incerto", "Em fila")
        val filtrosRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        filtros.forEach { filtro ->
            val selecionado = filtroConclusao == filtro

            val btn = TextView(this).apply {
                text = filtro
                textSize = 10f
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(if (selecionado) cyan else lightBlue)
                background = roundRect(
                    if (selecionado) Color.rgb(8, 38, 68) else cardDark,
                    if (selecionado) cyan else borderBlue,
                    12
                )
                setOnClickListener {
                    filtroConclusao = filtro
                    navegarPara("Concluídos")
                }
            }

            filtrosRow.addView(
                btn,
                LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                    if (filtro != "Todos") leftMargin = dp(3)
                    if (filtro != "Em fila") rightMargin = dp(3)
                }
            )
        }

        content.addView(
            filtrosRow,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(16)
            }
        )

        val lista = when (filtroConclusao) {
            "Todos" -> concluidos.toList()
            "Entregue" -> concluidos.filter { it.status == "Entregue" }
            "Falhou" -> concluidos.filter { it.status == "Falhou" }
            "Incerto" -> concluidos.filter { it.status == "Incerto" }
            else -> emptyList()
        }

        val mostrarFila = filtroConclusao == "Todos" ||
            filtroConclusao == "Em fila"

        val total = lista.size + if (mostrarFila) filaEspera.size else 0

        content.addView(
            txt("$total REGISTOS", 12f, cyan, true).apply {
                setPadding(0, 0, 0, dp(10))
            }
        )

        lista.forEach { content.addView(buildTransacaoRow(it)) }

        if (mostrarFila) {
            filaEspera.forEach { pedido ->
                val linha = cardBox(cardDark, borderBlue).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL

                    val info = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(txt(pedido.numeroCliente, 15f, white, true))
                        addView(
                            txt(
                                "${pedido.megas} MB · ${pedido.hora}",
                                11f,
                                lightBlue
                            )
                        )
                    }

                    addView(info, LinearLayout.LayoutParams(0, -2, 1f))
                    addView(
                        pillBadge(
                            "Em fila",
                            yellow,
                            Color.rgb(55, 43, 12),
                            yellow
                        )
                    )
                }
                content.addView(linha)
            }
        }

        if (total == 0) {
            content.addView(
                cardBox(cardDark, borderBlue).apply {
                    addView(
                        txt(
                            "Não existem registos nesta categoria.",
                            13f,
                            lightBlue
                        )
                    )
                }
            )
        }
    }

    // ============================================================
    // 8. TELA DE GESTÃO DOS SIMs
    // ============================================================

    private fun telaSims() {
        content.addView(txt("Gestão de SIMs", 19f, white, true))

        content.addView(
            txt("Reset diário às 00:00", 12f, lightBlue).apply {
                setPadding(0, 0, 0, dp(12))
            }
        )

        val sims = listOf(
            Triple(1, "VODACOM · Slot 1", sim1Usados),
            Triple(2, "SIM 2 · Slot 2", sim2Usados)
        )

        sims.forEach { sim ->
            val numero = sim.first
            val descricao = sim.second
            val usados = sim.third
            val ativo = simSelecionado == numero

            val box = cardBox(
                if (ativo) Color.rgb(10, 38, 36) else cardDark,
                if (ativo) green else borderBlue
            ).apply {
                addView(
                    txt(
                        "SIM $numero ${if (ativo) "· ACTIVO" else ""}",
                        16f,
                        if (ativo) green else white,
                        true
                    )
                )
                addView(txt(descricao, 12f, lightBlue))
                addView(
                    txt(
                        "Transferências: $usados / $limiteSim",
                        14f,
                        white,
                        true
                    ).apply {
                        setPadding(0, dp(6), 0, dp(8))
                    }
                )

                if (!ativo) {
                    addView(
                        smallActionBtn("Usar este SIM", green) {
                            simSelecionado = numero
                            guardarContadores()
                            navegarPara("SIMs")
                        },
                        LinearLayout.LayoutParams(-1, dp(36))
                    )
                }

                addView(
                    smallActionBtn("Reiniciar contador", yellow) {
                        if (numero == 1) sim1Usados = 0 else sim2Usados = 0
                        guardarContadores()
                        navegarPara("SIMs")
                    },
                    LinearLayout.LayoutParams(-1, dp(36)).apply {
                        topMargin = dp(6)
                    }
                )
            }

            content.addView(box)
        }

        val usadosAtivo = if (simSelecionado == 1) sim1Usados else sim2Usados
        val aviso = if (usadosAtivo >= limiteSim) {
            "Limite atingido. Seleccione o outro SIM para continuar."
        } else {
            "SIM seleccionado: $simSelecionado · Restam ${limiteSim - usadosAtivo} transferências."
        }

        content.addView(
            txt(aviso, 12f, if (usadosAtivo >= limiteSim) yellow else lightBlue).apply {
                setPadding(0, dp(8), 0, 0)
            }
        )
    }

    // ============================================================
    // 9. CONFIGURAÇÕES
    // ============================================================

    private fun telaConfig() {
        content.addView(txt("Configurações", 19f, white, true))

        content.addView(
            txt(
                "Ajustes do motor USSD e Acessibilidade",
                12f,
                lightBlue
            ).apply {
                setPadding(0, 0, 0, dp(12))
            }
        )

        content.addView(
            cardBox(cardDark, borderBlue).apply {
                addView(txt("Serviço de Acessibilidade USSD", 15f, white, true))
                addView(
                    txt(
                        "A leitura das respostas da operadora ainda precisa de ser ligada e testada.",
                        12f,
                        lightBlue
                    )
                )
            }
        )

        content.addView(
            cardBox(cardDark, borderBlue).apply {
                addView(txt("Confirmação de transferências", 15f, white, true))
                addView(
                    txt(
                        "O aplicativo não deve marcar megas como entregues sem confirmação real da Vodacom.",
                        12f,
                        lightBlue
                    )
                )
            }
        )

        content.addView(
            cardBox(cardDark, borderBlue).apply {
                addView(txt("Meta diária", 15f, white, true))
                addView(txt("Meta actual: $metaGb GB", 13f, cyan, true))
            }
        )
    }

    // ============================================================
    // 10. BARRA DE NAVEGAÇÃO
    // ============================================================

    private fun renderizarBarraInferior() {
        navBar.removeAllViews()

        val abas = listOf(
            "Início" to "⌂",
            "Fila" to "☰",
            "Concluídos" to "✓",
            "SIMs" to "▤",
            "Config" to "⚙"
        )

        abas.forEach { (nome, icone) ->
            val selecionada = nome == abaAtual

            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(2), dp(4), dp(2), dp(4))

                if (selecionada) {
                    background = roundRect(
                        Color.rgb(12, 46, 98),
                        cyan,
                        12
                    )
                }

                addView(
                    txt(
                        icone,
                        16f,
                        if (selecionada) cyan else lightBlue,
                        true
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                )

                addView(
                    txt(
                        nome,
                        10f,
                        if (selecionada) cyan else lightBlue,
                        selecionada
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                )

                setOnClickListener { navegarPara(nome) }
            }

            navBar.addView(
                item,
                LinearLayout.LayoutParams(0, -1, 1f).apply {
                    marginStart = dp(2)
                    marginEnd = dp(2)
                }
            )
        }
    }

    // ============================================================
    // 11. LINHA DO HISTÓRICO
    // ============================================================

    private fun buildTransacaoRow(item: TransacaoConcluida): LinearLayout {
        val entregue = item.status == "Entregue"
        val incerto = item.status == "Incerto"

        val statusText = when {
            entregue -> "✓ Entregue"
            incerto -> "● Incerto"
            else -> "✕ Falhou"
        }

        val corEstado = when {
            entregue -> green
            incerto -> yellow
            else -> red
        }

        val fundoEstado = when {
            entregue -> greenBg
            incerto -> Color.rgb(55, 43, 12)
            else -> Color.rgb(65, 18, 26)
        }

        return cardBox(cardDark, borderBlue).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val icon = txt(
                when {
                    entregue -> "✓"
                    incerto -> "●"
                    else -> "✕"
                },
                14f,
                corEstado,
                true
            ).apply {
                gravity = Gravity.CENTER
                background = oval(fundoEstado, corEstado)
            }

            addView(icon, LinearLayout.LayoutParams(dp(36), dp(36)))

            val meio = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), 0, 0, 0)

                addView(txt(item.numeroCliente, 15f, white, true))
                addView(
                    txt(
                        "Megas · ${item.hora} · ${item.simInfo}",
                        11f,
                        lightBlue
                    )
                )
            }

            addView(meio, LinearLayout.LayoutParams(0, -2, 1f))

            val direita = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END

                addView(txt("${item.megas} MB", 13f, cyan, true))
                addView(
                    pillBadge(
                        statusText,
                        corEstado,
                        fundoEstado,
                        corEstado
                    )
                )
            }

            addView(direita)
        }
    }

    // ============================================================
    // 12. CONTADORES DIÁRIOS DOS SIMs
    // ============================================================

    private fun carregarContadores() {
        sim1Usados = preferencias.getInt("sim1_usados", 0)
        sim2Usados = preferencias.getInt("sim2_usados", 0)
        simSelecionado = preferencias.getInt("sim_selecionado", 1)
            .coerceIn(1, 2)
    }

    private fun guardarContadores() {
        preferencias.edit()
            .putInt("sim1_usados", sim1Usados)
            .putInt("sim2_usados", sim2Usados)
            .putInt("sim_selecionado", simSelecionado)
            .putString("data_contadores", dataHoje())
            .apply()
    }

    private fun verificarNovoDia() {
        val dataGuardada = preferencias.getString("data_contadores", "")
        val hoje = dataHoje()

        if (dataGuardada != hoje) {
            sim1Usados = 0
            sim2Usados = 0
            guardarContadores()
        }
    }

    private fun dataHoje(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // ============================================================
    // 13. COMPONENTE DO CÍRCULO DE PROGRESSO
    // ============================================================

    private inner class RingProgressView(
        context: Context,
        private val percentagem: Float,
        private val valorPrincipal: String,
        private val textoInferior: String
    ) : View(context) {

        private val paintFundo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(9).toFloat()
            color = Color.rgb(14, 52, 118)
        }

        private val paintProgresso = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(9).toFloat()
            strokeCap = Paint.Cap.ROUND
            color = cyan
        }

        private val paintValor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white
            textSize = dp(19).toFloat()
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        private val paintTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lightBlue
            textSize = dp(11).toFloat()
            textAlign = Paint.Align.CENTER
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val tamanho = width.coerceAtMost(height).toFloat()
            val padding = dp(12).toFloat()
            val rect = RectF(
                padding,
                padding,
                tamanho - padding,
                tamanho - padding
            )

            canvas.drawArc(rect, 0f, 360f, false, paintFundo)
            canvas.drawArc(
                rect,
                -90f,
                360f * percentagem,
                false,
                paintProgresso
            )

            canvas.drawText(
                valorPrincipal,
                tamanho / 2f,
                tamanho / 2f + dp(2),
                paintValor
            )

            canvas.drawText(
                textoInferior,
                tamanho / 2f,
                tamanho / 2f + dp(18),
                paintTexto
            )
        }
    }

    // ============================================================
    // 14. HELPERS VISUAIS
    // ============================================================

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

    private fun cardBox(corFundo: Int, corBorda: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(10)
            }
            background = roundRect(corFundo, corBorda, 14)
        }
    }

    private fun smallActionBtn(
        texto: String,
        cor: Int,
        acao: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = texto
            textSize = 11f
            setTextColor(cor)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = roundRect(Color.rgb(8, 22, 48), cor, 10)
            setOnClickListener { acao() }
        }
    }

    private fun pillBadge(
        texto: String,
        corTexto: Int,
        corFundo: Int,
        corBorda: Int
    ): TextView {
        return TextView(this).apply {
            this.text = texto
            textSize = 10f
            setTextColor(corTexto)
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = roundRect(corFundo, corBorda, 20)
        }
    }

    private fun roundRect(
        corFundo: Int,
        corBorda: Int,
        raioDp: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(corFundo)
            cornerRadius = dp(raioDp).toFloat()
            setStroke(dp(1), corBorda)
        }
    }

    private fun oval(corFundo: Int, corBorda: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(corFundo)
            setStroke(dp(1), corBorda)
        }
    }

    private fun txt(
        texto: String,
        tamanho: Float,
        cor: Int,
        negrito: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = texto
            textSize = tamanho
            setTextColor(cor)
            if (negrito) setTypeface(null, Typeface.BOLD)
        }
    }
}
