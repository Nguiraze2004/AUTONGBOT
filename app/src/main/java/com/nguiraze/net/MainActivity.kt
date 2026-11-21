
package com.nguiraze.net

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PedidoFila(
    val id: String,
    val numeroCliente: String,
    val megas: Int,
    val valorMT: Int,
    val hora: String
)

data class TransacaoConcluida(
    val numeroCliente: String,
    val megas: Int,
    val valorMT: Int,
    val hora: String,
    val simInfo: String,
    val status: String
)

data class AgenteExecutor(
    val id: Int,
    val nome: String,
    val sigla: String,
    var online: Boolean,
    var vendas: Int,
    val simTag: String
)

class MainActivity : Activity() {

    private val bgTop = Color.rgb(5, 14, 34)
    private val bgBot = Color.rgb(4, 20, 52)
    private val cardNavy = Color.rgb(9, 28, 66)
    private val cardDark = Color.rgb(10, 22, 46)
    private val borderBlue = Color.rgb(24, 72, 148)
    private val cyan = Color.rgb(0, 215, 255)
    private val green = Color.rgb(0, 220, 125)
    private val greenBg = Color.rgb(12, 54, 48)
    private val yellow = Color.rgb(255, 193, 7)
    private val red = Color.rgb(240, 75, 85)
    private val white = Color.WHITE
    private val lightBlue = Color.rgb(155, 185, 225)

    private lateinit var content: LinearLayout
    private lateinit var navBar: LinearLayout

    private var abaAtual = "Início"
    private var filtroConcluidos = "Todos"

    private var filaPausada = false
    private var velocidadeUssd = "Ultra"
    private var autoRotacaoSim = true
    private var sobraAutomatica = false
    private var numeroSobra = ""
    private var metaGb = 100

    private var simSelecionado = 1
    private var sim1Usados = 0
    private var sim2Usados = 0
    private val limiteSim = 10

    private var pedidoEmCurso: PedidoFila? = null
    private val filaEspera = mutableListOf<PedidoFila>()
    private val concluidos = mutableListOf<TransacaoConcluida>()

    private val executores = mutableListOf(
        AgenteExecutor(1, "Este aparelho", "ES", true, 0, "SIM 1 · Vodacom"),
        AgenteExecutor(2, "Agente 2", "AG", false, 0, "SIM 2 · Movitel")
    )

    private val totalMegasEntreguesHoje: Long
        get() = concluidos
            .filter { it.status == "Entregue" }
            .sumOf { it.megas.toLong() }

    private val gbVendidosHoje: Double
        get() = totalMegasEntreguesHoje / 1024.0

    private val totalTransferenciasConcluidas: Int
        get() = concluidos.count { it.status == "Entregue" }

    private val saldoFaturadoHojeMT: Int
        get() = concluidos
            .filter { it.status == "Entregue" }
            .sumOf { it.valorMT }

    private val totalNaFila: Int
        get() = filaEspera.size + if (pedidoEmCurso != null) 1 else 0

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bgTop
        window.navigationBarColor = Color.rgb(6, 16, 40)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(bgTop, bgBot)
            )
        }

        val scroll = ScrollView(this).apply {
            clipToPadding = false
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(24))
        }

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        navBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(6, 16, 40))
            setPadding(dp(4), dp(6), dp(4), dp(6))
        }

        root.addView(
            navBar,
            LinearLayout.LayoutParams(-1, dp(64))
        )

        setContentView(root)
        navegarPara("Início")
    }

    // =========================================================
    // NAVEGAÇÃO
    // =========================================================

    private fun navegarPara(aba: String) {
        abaAtual = aba
        content.removeAllViews()
        renderizarBarraInferior()

        when (aba) {
            "Início" -> telaInicio()
            "Fila" -> telaFilaExecutores()
            "Concluídos" -> telaConcluidos()
            "SIMs" -> telaSims()
            "Config" -> telaConfig()
        }
    }

    private fun renderizarBarraInferior() {
        navBar.removeAllViews()

        val abas = listOf(
            "Início" to "⌂",
            "Fila" to "≡",
            "Concluídos" to "✓",
            "SIMs" to "▣",
            "Config" to "⚙"
        )

        abas.forEach { item ->
            val nome = item.first
            val simbolo = item.second
            val ativo = abaAtual == nome

            val coluna = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(2), dp(4), dp(2))
                setOnClickListener {
                    navegarPara(nome)
                }
            }

            coluna.addView(
                txt(
                    simbolo,
                    20f,
                    if (ativo) cyan else lightBlue,
                    true
                )
            )

            coluna.addView(
                txt(
                    nome,
                    9f,
                    if (ativo) cyan else lightBlue,
                    ativo
                )
            )

            navBar.addView(
                coluna,
                LinearLayout.LayoutParams(0, -1, 1f)
            )
        }
    }

    // =========================================================
    // MOTOR DA FILA
    // =========================================================

    private fun despacharPedido(
        numero: String,
        megas: Int,
        valorMT: Int
    ) {
        if (numero.isBlank() || megas <= 0 || valorMT <= 0) {
            Toast.makeText(
                this,
                "Preencha os dados corretamente.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val hora = SimpleDateFormat(
            "HH:mm",
            Locale.getDefault()
        ).format(Date())

        val novo = PedidoFila(
            System.currentTimeMillis().toString(),
            numero,
            megas,
            valorMT,
            hora
        )

        if (pedidoEmCurso == null && !filaPausada) {
            pedidoEmCurso = novo
        } else {
            filaEspera.add(novo)
        }

        Toast.makeText(
            this,
            "Pedido adicionado à fila.",
            Toast.LENGTH_SHORT
        ).show()

        navegarPara("Fila")
    }

    private fun concluirPedidoAtual(status: String) {
        val atual = pedidoEmCurso ?: return

        val dataHora = SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

        val opNome = if (simSelecionado == 1) {
            "SIM 1 (Vodacom)"
        } else {
            "SIM 2 (Movitel)"
        }

        concluidos.add(
            0,
            TransacaoConcluida(
                atual.numeroCliente,
                atual.megas,
                atual.valorMT,
                dataHora,
                opNome,
                status
            )
        )

        if (status == "Entregue") {
            if (simSelecionado == 1) {
                sim1Usados++
            } else {
                sim2Usados++
            }

            executores.firstOrNull { it.online }?.let {
                it.vendas++
            }

            if (
                autoRotacaoSim &&
                simSelecionado == 1 &&
                sim1Usados >= limiteSim
            ) {
                simSelecionado = 2
            } else if (
                autoRotacaoSim &&
                simSelecionado == 2 &&
                sim2Usados >= limiteSim
            ) {
                simSelecionado = 1
            }
        }

        pedidoEmCurso = if (
            !filaPausada && filaEspera.isNotEmpty()
        ) {
            filaEspera.removeAt(0)
        } else {
            null
        }

        navegarPara("Fila")
    }

    private fun iniciarProximoPedido() {
        if (pedidoEmCurso == null && filaEspera.isNotEmpty()) {
            pedidoEmCurso = filaEspera.removeAt(0)
        }
        navegarPara("Fila")
    }

    // =========================================================
    // TELA INÍCIO
    // =========================================================

    private fun telaInicio() {

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val avatar = txt("🤖", 19f, white).apply {
            gravity = Gravity.CENTER
            background = oval(Color.rgb(12, 42, 95), cyan)
        }

        header.addView(
            avatar,
            LinearLayout.LayoutParams(dp(42), dp(42))
        )

        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }

        titleCol.addView(txt("NGUIRAZE NET", 16f, white, true))

        titleCol.addView(
            txt(
                "Automação USSD · ${if (filaPausada) "Pausada" else "Em escuta"}",
                11f,
                lightBlue
            )
        )

        header.addView(
            titleCol,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        header.addView(
            pillBadge(
                if (filaPausada) "● Pausado" else "● Online",
                if (filaPausada) yellow else green,
                if (filaPausada) Color.rgb(60, 48, 12) else greenBg,
                if (filaPausada) yellow else green
            )
        )

        content.addView(header)

        content.addView(
            pillBadge(
                "📡 MEGAS DISPONÍVEIS 24/24H",
                cyan,
                Color.rgb(10, 38, 92),
                borderBlue
            ).apply {
                layoutParams = LinearLayout.LayoutParams(-2, -2).apply {
                    topMargin = dp(12)
                    bottomMargin = dp(12)
                }
            }
        )

        // CARTÃO PRINCIPAL

        val summaryCard = cardBox(cardNavy, borderBlue).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val gbStr = String.format(
            Locale("pt", "MZ"),
            "%.2f",
            gbVendidosHoje
        )

        val progresso = if (metaGb > 0) {
            (gbVendidosHoje / metaGb).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

        summaryCard.addView(
            RingProgressView(
                this@MainActivity,
                progresso,
                gbStr,
                "GB vendidos"
            ),
            LinearLayout.LayoutParams(dp(135), dp(135))
        )

        val rightStats = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }

        rightStats.addView(txt("RESUMO DE HOJE", 11f, lightBlue, true))

        addSpace(rightStats, 8)

        rightStats.addView(
            txt("$totalTransferenciasConcluidas", 23f, white, true)
        )

        rightStats.addView(
            txt("Transferências entregues", 10f, lightBlue)
        )

        addSpace(rightStats, 10)

        rightStats.addView(
            txt("$saldoFaturadoHojeMT MT", 18f, green, true)
        )

        rightStats.addView(
            txt("Total vendido", 10f, lightBlue)
        )

        summaryCard.addView(
            rightStats,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        content.addView(summaryCard)

        // ATALHOS

        sectionTitle("ACESSO RÁPIDO")

        val shortcutRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        shortcutRow1.addView(
            shortcut("📤", "Enviar megas") {
                navegarPara("Fila")
            },
            LinearLayout.LayoutParams(0, dp(82), 1f)
        )

        shortcutRow1.addView(
            shortcut("📋", "Fila") {
                navegarPara("Fila")
            },
            LinearLayout.LayoutParams(0, dp(82), 1f)
        )

        content.addView(shortcutRow1)

        val shortcutRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        shortcutRow2.addView(
            shortcut("📊", "Histórico") {
                navegarPara("Concluídos")
            },
            LinearLayout.LayoutParams(0, dp(82), 1f)
        )

        shortcutRow2.addView(
            shortcut("🎯", "Meta diária") {
                navegarPara("Config")
            },
            LinearLayout.LayoutParams(0, dp(82), 1f)
        )

        content.addView(shortcutRow2)

        // SIMS

        sectionTitle("ESTADO DOS CARTÕES SIM")

        val simRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        simRow.addView(
            simMiniCard(
                "SIM 1 · VODACOM",
                sim1Usados,
                simSelecionado == 1
            ) {
                simSelecionado = 1
                navegarPara("Início")
            },
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        simRow.addView(
            simMiniCard(
                "SIM 2 · MOVITEL",
                sim2Usados,
                simSelecionado == 2
            ) {
                simSelecionado = 2
                navegarPara("Início")
            },
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        content.addView(simRow)

        // FILA

        sectionTitle("ATIVIDADE DA FILA")

        val filaCard = cardBox(cardNavy, borderBlue)

        filaCard.addView(
            txt(
                if (pedidoEmCurso != null) "1 pedido em execução" else "Nenhum pedido em execução",
                13f,
                white,
                true
            )
        )

        filaCard.addView(
            txt(
                "${filaEspera.size} pedidos aguardando",
                11f,
                lightBlue
            )
        )

        filaCard.addView(
            smallActionBtn("ABRIR FILA", cyan) {
                navegarPara("Fila")
            }
        )

        content.addView(filaCard)

        // ÚLTIMAS TRANSAÇÕES

        sectionTitle("ÚLTIMAS TRANSAÇÕES")

        if (concluidos.isEmpty()) {
            content.addView(
                emptyCard("Ainda não existem transações.")
            )
        } else {
            concluidos.take(3).forEach {
                content.addView(buildConclusaoCard(it))
            }
        }
    }

    // =========================================================
    // TELA FILA E EXECUTORES
    // =========================================================

    private fun telaFilaExecutores() {

        pageHeader("Fila de pedidos", "Gestão de envios e executores")

        val controlCard = cardBox(cardNavy, borderBlue)

        controlCard.addView(
            txt(
                if (filaPausada) "FILA PAUSADA" else "FILA ATIVA",
                13f,
                if (filaPausada) yellow else green,
                true
            )
        )

        controlCard.addView(
            txt(
                "Pedidos na fila: $totalNaFila",
                12f,
                lightBlue
            )
        )

        controlCard.addView(
            smallActionBtn(
                if (filaPausada) "RETOMAR FILA" else "PAUSAR FILA",
                if (filaPausada) green else yellow
            ) {
                filaPausada = !filaPausada
                if (!filaPausada) iniciarProximoPedido()
                else navegarPara("Fila")
            }
        )

        controlCard.addView(
            smallActionBtn("LIMPAR FILA DE ESPERA", red) {
                filaEspera.clear()
                navegarPara("Fila")
            }
        )

        content.addView(controlCard)

        // FORMULÁRIO DE ENVIO

        sectionTitle("ADICIONAR PEDIDO")

        val manualCard = cardBox(cardNavy, borderBlue)

        manualCard.addView(txt("Número do cliente", 11f, lightBlue))

        val numeroInput = editField("Ex.: 84xxxxxxx", InputType.TYPE_CLASS_PHONE)
        manualCard.addView(numeroInput)

        manualCard.addView(
            txt("Quantidade de megas (MB)", 11f, lightBlue)
        )

        val mbInput = editField("Ex.: 450", InputType.TYPE_CLASS_NUMBER)
        manualCard.addView(mbInput)

        manualCard.addView(
            txt("Valor em meticais (MT)", 11f, lightBlue)
        )

        val valorInput = editField("Ex.: 10", InputType.TYPE_CLASS_NUMBER)
        manualCard.addView(valorInput)

        val presets = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val pacotes = listOf(
            Triple("450 MB", 450, 10),
            Triple("1 GB", 1024, 20),
            Triple("2 GB", 2048, 40)
        )

        pacotes.forEach { pacote ->
            presets.addView(
                smallActionBtn(pacote.first, cyan) {
                    mbInput.setText(pacote.second.toString())
                    valorInput.setText(pacote.third.toString())
                },
                LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    marginEnd = dp(4)
                }
            )
        }

        manualCard.addView(presets)

        manualCard.addView(
            smallActionBtn("ADICIONAR À FILA", green) {
                despacharPedido(
                    numeroInput.text.toString().trim(),
                    mbInput.text.toString().toIntOrNull() ?: 0,
                    valorInput.text.toString().toIntOrNull() ?: 0
                )
            }
        )

        content.addView(manualCard)

        // EXECUTORES

        sectionTitle("EXECUTORES")

        executores.forEach { executor ->
            val execCard = cardBox(cardNavy, borderBlue)

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            row.addView(
                txt(executor.sigla, 17f, cyan, true).apply {
                    gravity = Gravity.CENTER
                    background = oval(cardDark, borderBlue)
                },
                LinearLayout.LayoutParams(dp(42), dp(42))
            )

            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), 0, 0, 0)
            }

            info.addView(
                txt(executor.nome, 13f, white, true)
            )

            info.addView(
                txt(executor.simTag, 10f, lightBlue)
            )

            info.addView(
                txt("Vendas: ${executor.vendas}", 10f, lightBlue)
            )

            row.addView(
                info,
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            row.addView(
                pillBadge(
                    if (executor.online) "ONLINE" else "OFFLINE",
                    if (executor.online) green else red,
                    cardDark,
                    if (executor.online) green else red
                )
            )

            execCard.addView(row)

            execCard.addView(
                smallActionBtn(
                    if (executor.online) "DESATIVAR" else "ATIVAR",
                    if (executor.online) red else green
                ) {
                    executor.online = !executor.online
                    navegarPara("Fila")
                }
            )

            content.addView(execCard)
        }

        // PEDIDO EM CURSO

        sectionTitle("EM CURSO AGORA")

        val runningCard = cardBox(
            Color.rgb(8, 32, 72),
            cyan
        )

        val atual = pedidoEmCurso

        if (atual != null) {
            runningCard.addView(
                pillBadge("PEDIDO ATUAL", cyan, cardDark, cyan)
            )

            runningCard.addView(
                txt(atual.numeroCliente, 17f, white, true)
            )

            runningCard.addView(
                txt("${atual.megas} MB · ${atual.valorMT} MT", 13f, lightBlue)
            )

            runningCard.addView(
                txt("Adicionado às ${atual.hora}", 10f, lightBlue)
            )

            runningCard.addView(
                txt(
                    "Aguardando execução/validação.",
                    11f,
                    yellow
                )
            )

            runningCard.addView(
                smallActionBtn("TESTE MANUAL: CONFIRMAR ENTREGA", green) {
                    concluirPedidoAtual("Entregue")
                }
            )

            runningCard.addView(
                smallActionBtn("MARCAR COMO FALHOU", red) {
                    concluirPedidoAtual("Falhou")
                }
            )

            runningCard.addView(
                smallActionBtn("MARCAR COMO INCERTO", yellow) {
                    concluirPedidoAtual("Incerto")
                }
            )
        } else {
            runningCard.addView(
                txt(
                    "Nenhum pedido em curso no momento.",
                    12f,
                    lightBlue
                )
            )

            if (filaEspera.isNotEmpty() && !filaPausada) {
                runningCard.addView(
                    smallActionBtn("INICIAR PRÓXIMO PEDIDO", cyan) {
                        iniciarProximoPedido()
                    }
                )
            }
        }

        content.addView(runningCard)

        // FILA DE ESPERA

        sectionTitle("FILA DE ESPERA (${filaEspera.size})")

        if (filaEspera.isEmpty()) {
            content.addView(
                emptyCard("Não existem pedidos aguardando.")
            )
        } else {
            filaEspera.toList().forEachIndexed { index, pedido ->

                val itemCard = cardBox(cardNavy, borderBlue)

                itemCard.addView(
                    txt(
                        "${index + 1}. ${pedido.numeroCliente}",
                        14f,
                        white,
                        true
                    )
                )

                itemCard.addView(
                    txt(
                        "${pedido.megas} MB · ${pedido.valorMT} MT · ${pedido.hora}",
                        11f,
                        lightBlue
                    )
                )

                val actions = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                actions.addView(
                    smallActionBtn("SUBIR", cyan) {
                        if (index > 0) {
                            val item = filaEspera.removeAt(index)
                            filaEspera.add(index - 1, item)
                        }
                        navegarPara("Fila")
                    },
                    LinearLayout.LayoutParams(0, dp(38), 1f)
                )

                actions.addView(
                    smallActionBtn("REMOVER", red) {
                        filaEspera.remove(pedido)
                        navegarPara("Fila")
                    },
                    LinearLayout.LayoutParams(0, dp(38), 1f)
                )

                itemCard.addView(actions)
                content.addView(itemCard)
            }
        }
    }

    // =========================================================
    // TELA CONCLUÍDOS
    // =========================================================

    private fun telaConcluidos() {

        pageHeader("Conclusão", "Histórico de transferências")

        val filtros = listOf(
            "Todos",
            "Entregue",
            "Falhou",
            "Incerto",
            "Em fila"
        )

        val filterRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        filtros.forEach { filtro ->
            filterRow.addView(
                smallActionBtn(
                    filtro,
                    if (filtroConcluidos == filtro) cyan else borderBlue
                ) {
                    filtroConcluidos = filtro
                    navegarPara("Concluídos")
                },
                LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    marginEnd = dp(3)
                }
            )
        }

        content.addView(filterRow)

        val itens = mutableListOf<TransacaoConcluida>()

        if (pedidoEmCurso != null) {
            val p = pedidoEmCurso!!
            itens.add(
                TransacaoConcluida(
                    p.numeroCliente,
                    p.megas,
                    p.valorMT,
                    p.hora,
                    "Em execução",
                    "Em fila"
                )
            )
        }

        filaEspera.forEach { p ->
            itens.add(
                TransacaoConcluida(
                    p.numeroCliente,
                    p.megas,
                    p.valorMT,
                    p.hora,
                    "Aguardando",
                    "Em fila"
                )
            )
        }

        itens.addAll(concluidos)

        val visiveis = if (filtroConcluidos == "Todos") {
            itens
        } else {
            itens.filter { it.status == filtroConcluidos }
        }

        sectionTitle("${visiveis.size} REGISTOS")

        if (visiveis.isEmpty()) {
            content.addView(
                emptyCard("Não existem registos nesta categoria.")
            )
        } else {
            visiveis.forEach {
                content.addView(buildConclusaoCard(it))
            }
        }
    }

    // =========================================================
    // TELA SIMS
    // =========================================================

    private fun telaSims() {

        pageHeader("Cartões SIM", "Controlo de utilização diária")

        simDetalhado(
            "SIM 1",
            "VODACOM",
            sim1Usados,
            simSelecionado == 1,
            green
        ) {
            simSelecionado = 1
            navegarPara("SIMs")
        }

        simDetalhado(
            "SIM 2",
            "MOVITEL",
            sim2Usados,
            simSelecionado == 2,
            cyan
        ) {
            simSelecionado = 2
            navegarPara("SIMs")
        }

        sectionTitle("CONTROLO DE UTILIZAÇÃO")

        val info = cardBox(cardNavy, borderBlue)

        info.addView(
            txt("Limite por SIM: $limiteSim transferências", 13f, white, true)
        )

        info.addView(
            txt("SIM 1: $sim1Usados / $limiteSim", 12f, lightBlue)
        )

        info.addView(
            txt("SIM 2: $sim2Usados / $limiteSim", 12f, lightBlue)
        )

        info.addView(
            smallActionBtn("REINICIAR CONTADORES", yellow) {
                sim1Usados = 0
                sim2Usados = 0
                navegarPara("SIMs")
            }
        )

        content.addView(info)
    }

    // =========================================================
    // TELA CONFIGURAÇÕES
    // =========================================================

    private fun telaConfig() {

        pageHeader("Configurações", "Preferências do sistema")

        sectionTitle("ACESSIBILIDADE")

        val accessCard = cardBox(cardNavy, borderBlue)

        accessCard.addView(
            txt("Serviço de acessibilidade USSD", 13f, white, true)
        )

        accessCard.addView(
            txt(
                "Permite ao aplicativo observar as janelas de interação USSD.",
                11f,
                lightBlue
            )
        )

        accessCard.addView(
            smallActionBtn("ABRIR ACESSIBILIDADE", cyan) {
                startActivity(
                    android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                )
            }
        )

        content.addView(accessCard)

        sectionTitle("VELOCIDADE USSD")

        val speedCard = cardBox(cardNavy, borderBlue)

        listOf("Normal", "Rápida", "Ultra").forEach { velocidade ->
            speedCard.addView(
                smallActionBtn(
                    if (velocidadeUssd == velocidade) "✓ $velocidade" else velocidade,
                    cyan
                ) {
                    velocidadeUssd = velocidade
                    navegarPara("Config")
                }
            )
        }

        content.addView(speedCard)

        sectionTitle("ROTAÇÃO AUTOMÁTICA DE SIM")

        val rotationCard = cardBox(cardNavy, borderBlue)

        rotationCard.addView(
            txt(
                if (autoRotacaoSim) "Ativada" else "Desativada",
                13f,
                if (autoRotacaoSim) green else yellow,
                true
            )
        )

        rotationCard.addView(
            smallActionBtn("ALTERAR OPÇÃO", cyan) {
                autoRotacaoSim = !autoRotacaoSim
                navegarPara("Config")
            }
        )

        content.addView(rotationCard)

        sectionTitle("META DIÁRIA")

        val goalCard = cardBox(cardNavy, borderBlue)

        goalCard.addView(txt("Meta de vendas em GB", 12f, lightBlue))

        val metaInput = editField(
            "Meta em GB",
            android.text.InputType.TYPE_CLASS_NUMBER
        )

        metaInput.setText(metaGb.toString())
        goalCard.addView(metaInput)

        goalCard.addView(
            smallActionBtn("GUARDAR META", green) {
                val novaMeta = metaInput.text.toString().toIntOrNull()

                if (novaMeta != null && novaMeta > 0) {
                    metaGb = novaMeta
                    Toast.makeText(
                        this,
                        "Meta atualizada.",
                        Toast.LENGTH_SHORT
                    ).show()
                    navegarPara("Config")
                } else {
                    Toast.makeText(
                        this,
                        "Introduza uma meta válida.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        content.addView(goalCard)

        sectionTitle("SOBRA AUTOMÁTICA")

        val sobraCard = cardBox(cardNavy, borderBlue)

        sobraCard.addView(
            txt(
                if (sobraAutomatica) "Ativada" else "Desativada",
                12f,
                lightBlue
            )
        )

        val sobraInput = editField(
            "Número para sobra",
            android.text.InputType.TYPE_CLASS_PHONE
        )

        sobraInput.setText(numeroSobra)
        sobraCard.addView(sobraInput)

        sobraCard.addView(
            smallActionBtn("ALTERAR SOBRA AUTOMÁTICA", cyan) {
                numeroSobra = sobraInput.text.toString()
                sobraAutomatica = !sobraAutomatica
                navegarPara("Config")
            }
        )

        content.addView(sobraCard)
    }

    // =========================================================
    // COMPONENTES VISUAIS
    // =========================================================

    private fun pageHeader(titulo: String, subtitulo: String) {
        content.addView(
            txt(titulo, 22f, white, true)
        )

        content.addView(
            txt(subtitulo, 11f, lightBlue).apply {
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(16)
                }
            }
        )
    }

    private fun sectionTitle(titulo: String) {
        content.addView(
            txt(titulo, 11f, cyan, true).apply {
                setPadding(0, dp(12), 0, dp(8))
            }
        )
    }

    private fun addSpace(parent: LinearLayout, height: Int) {
        parent.addView(
            android.view.View(this),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }

    private fun shortcut(
        icon: String,
        name: String,
        action: () -> Unit
    ): LinearLayout {
        return cardBox(cardNavy, borderBlue).apply {
            gravity = Gravity.CENTER
            addView(txt(icon, 22f, cyan))
            addView(txt(name, 11f, white, true))
            setOnClickListener { action() }
        }
    }

    private fun simMiniCard(
        nome: String,
        usados: Int,
        selecionado: Boolean,
        action: () -> Unit
    ): LinearLayout {
        return cardBox(
            if (selecionado) Color.rgb(10, 48, 85) else cardNavy,
            if (selecionado) cyan else borderBlue
        ).apply {
            addView(txt(nome, 10f, white, true))
            addView(
                txt("$usados / $limiteSim", 19f, cyan, true)
            )
            addView(txt("Transferências usadas", 9f, lightBlue))
            addView(
                smallActionBtn(
                    if (selecionado) "SELECIONADO" else "SELECIONAR",
                    cyan
                ) {
                    action()
                }
            )
        }
    }

    private fun simDetalhado(
        nome: String,
        operadora: String,
        usados: Int,
        selecionado: Boolean,
        cor: Int,
        action: () -> Unit
    ) {
        val card = cardBox(
            if (selecionado) Color.rgb(10, 48, 85) else cardNavy,
            if (selecionado) cyan else borderBlue
        )

        card.addView(
            txt("$nome · $operadora", 16f, white, true)
        )

        card.addView(
            txt(
                if (selecionado) "SIM selecionado" else "SIM disponível",
                11f,
                if (selecionado) green else lightBlue
            )
        )

        card.addView(
            txt("$usados / $limiteSim transferências", 14f, cor, true)
        )

        val percentagem = (usados.toFloat() / limiteSim).coerceIn(0f, 1f)

        val progress = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = roundRect(cardDark, borderBlue, 8)
        }

        progress.addView(
            android.view.View(this).apply {
                background = roundRect(cor, cor, 8)
            },
            LinearLayout.LayoutParams(
                0,
                dp(8),
                percentagem.coerceAtLeast(0.02f)
            )
        )

        card.addView(progress)

        card.addView(
            smallActionBtn("SELECIONAR ESTE SIM", cyan) {
                action()
            }
        )

        content.addView(card)
    }

    private fun emptyCard(mensagem: String): LinearLayout {
        return cardBox(cardDark, borderBlue).apply {
            gravity = Gravity.CENTER
            addView(txt("📭", 25f, lightBlue))
            addView(txt(mensagem, 11f, lightBlue))
        }
    }

    private fun editField(
        hintText: String,
        inputType: Int
    ): EditText {
        return EditText(this).apply {
            hint = hintText
            hintTextColor = lightBlue
            setTextColor(white)
            textSize = 13f
            this.inputType = inputType
            setSingleLine(true)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = roundRect(cardDark, borderBlue, 10)

            layoutParams = LinearLayout.LayoutParams(
                -1,
                dp(46)
            ).apply {
                topMargin = dp(5)
                bottomMargin = dp(9)
            }
        }
    }

    private fun buildConclusaoCard(
        item: TransacaoConcluida
    ): LinearLayout {

        val card = cardBox(cardNavy, borderBlue)

        val cor = when (item.status) {
            "Entregue" -> green
            "Falhou" -> red
            "Incerto" -> yellow
            else -> cyan
        }

        card.addView(
            pillBadge(
                item.status.uppercase(Locale.getDefault()),
                cor,
                cardDark,
                cor
            )
        )

        card.addView(
            txt(item.numeroCliente, 15f, white, true)
        )

        card.addView(
            txt(
                "${item.megas} MB · ${item.valorMT} MT",
                12f,
                lightBlue
            )
        )

        card.addView(
            txt(item.hora, 10f, lightBlue)
        )

        card.addView(
            txt(item.simInfo, 10f, cyan)
        )

        return card
    }

    // =========================================================
    // DESENHO DO CÍRCULO DE PROGRESSO
    // =========================================================

    inner class RingProgressView(
        context: android.content.Context,
        private val pct: Float,
        private val mainVal: String,
        private val subVal: String
    ) : android.view.View(context) {

        private val pBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(9).toFloat()
            color = Color.rgb(14, 52, 118)
        }

        private val pFg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(9).toFloat()
            strokeCap = Paint.Cap.ROUND
            color = cyan
        }

        private val pBig = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white
            textSize = dp(19).toFloat()
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        private val pSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lightBlue
            textSize = dp(11).toFloat()
            textAlign = Paint.Align.CENTER
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val s = width.coerceAtMost(height).toFloat()
            val pad = dp(12).toFloat()

            val rect = RectF(
                pad,
                pad,
                s - pad,
                s - pad
            )

            canvas.drawArc(rect, 0f, 360f, false, pBg)

            canvas.drawArc(
                rect,
                -90f,
                360f * pct,
                false,
                pFg
            )

            canvas.drawText(
                mainVal,
                s / 2f,
                s / 2f + dp(2),
                pBig
            )

            canvas.drawText(
                subVal,
                s / 2f,
                s / 2f + dp(20),
                pSub
            )
        }
    }

    // =========================================================
    // ESTILOS E FORMAS
    // =========================================================

    private fun cardBox(
        bg: Int,
        stroke: Int
    ) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(12), dp(12), dp(12))
        layoutParams = LinearLayout.LayoutParams(
            -1,
            -2
        ).apply {
            bottomMargin = dp(10)
        }
        background = roundRect(bg, stroke, 14)
    }

    private fun smallActionBtn(
        label: String,
        stroke: Int,
        onClick: () -> Unit
    ) = TextView(this).apply {
        text = label
        textSize = 10f
        setTextColor(stroke)
        setTypeface(null, Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(dp(8), dp(9), dp(8), dp(9))
        background = roundRect(
            Color.rgb(8, 22, 48),
            stroke,
            10
        )

        layoutParams = LinearLayout.LayoutParams(
            -1,
            -2
        ).apply {
            topMargin = dp(7)
        }

        setOnClickListener {
            onClick()
        }
    }

    private fun pillBadge(
        text: String,
        textColor: Int,
        bg: Int,
        stroke: Int
    ) = TextView(this).apply {
        this.text = text
        textSize = 10f
        setTextColor(textColor)
        setTypeface(null, Typeface.BOLD)
        setPadding(dp(10), dp(5), dp(10), dp(5))
        background = roundRect(bg, stroke, 20)
    }

    private fun roundRect(
        bg: Int,
        stroke: Int,
        radiusDp: Int
    ) = GradientDrawable().apply {
        setColor(bg)
        cornerRadius = dp(radiusDp).toFloat()
        setStroke(dp(1), stroke)
    }

    private fun oval(
        bg: Int,
        stroke: Int
    ) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(bg)
        setStroke(dp(1), stroke)
    }

    private fun txt(
        t: String,
        s: Float,
        c: Int,
        b: Boolean = false
    ) = TextView(this).apply {
        text = t
        textSize = s
        setTextColor(c)

        if (b) {
            setTypeface(null, Typeface.BOLD)
        }
    }
}
