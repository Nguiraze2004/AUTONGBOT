package com.nguiraze.net

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import android.text.InputType

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
    var totalMBEnviados: Int = 0,
    var enviosEmCurso: Int = 0,
    var falhas: Int = 0
)

class MainActivity : Activity() {

    private val fundo = Color.rgb(5, 8, 14)
    private val cartao = Color.rgb(14, 19, 28)
    private val cartao2 = Color.rgb(20, 26, 37)
    private val branco = Color.WHITE
    private val cinza = Color.rgb(145, 153, 168)
    private val vermelho = Color.rgb(230, 10, 35)
    private val verde = Color.rgb(40, 200, 125)
    private val azul = Color.rgb(55, 135, 240)
    private val amarelo = Color.rgb(245, 185, 55)

    private lateinit var raiz: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var navigation: LinearLayout

    private var paginaAtual = "Início"
    private var filtroConclusao = "TODOS"

    private val transferencias = mutableListOf<Transferencia>()
    private val pedidosFila = mutableListOf<Pedido>()

    private val executores = mutableListOf(
        Executor("Executor 01", "OFFLINE"),
        Executor("Executor 02", "OFFLINE")
    )

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = fundo
        window.navigationBarColor = fundo

        raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(fundo)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }

        scroll.addView(content)

        navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(10, 14, 22))
            setPadding(dp(4), dp(9), dp(4), dp(9))
        }

        raiz.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        raiz.addView(
            navigation,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(68)
            )
        )

        setContentView(raiz)
        mostrarPagina("Início")
    }

    private fun corEstado(estado: String): Int {
        return when (estado.uppercase()) {
            "CONCLUÍDO", "CONCLUIDO", "LIVRE", "ONLINE" -> verde
            "ERRO", "FALHOU", "BLOQUEADO" -> vermelho
            "INCERTO", "EM ESPERA", "AGUARDANDO" -> amarelo
            "EM CURSO", "OCUPADO", "PROCESSANDO" -> azul
            else -> cinza
        }
    }

    private fun fundoArredondado(
        cor: Int,
        raio: Int = 16,
        borda: Int = Color.TRANSPARENT
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(cor)
            cornerRadius = dp(raio).toFloat()
            if (borda != Color.TRANSPARENT) {
                setStroke(dp(1), borda)
            }
        }
    }

    private fun texto(
        valor: String,
        tamanho: Float = 14f,
        cor: Int = branco,
        negrito: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = valor
            textSize = tamanho
            setTextColor(cor)
            if (negrito) {
                setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun espaco(
        pai: LinearLayout,
        altura: Int = 12
    ) {
        pai.addView(
            View(this),
            LinearLayout.LayoutParams(
                1,
                dp(altura)
            )
        )
    }

    private fun painel(
        pai: LinearLayout,
        padding: Int = 14
    ): LinearLayout {
        val caixa = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
            background = fundoArredondado(cartao, 17, Color.rgb(35, 43, 57))
        }

        pai.addView(
            caixa,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        )

        return caixa
    }

    private fun linha(
        pai: LinearLayout,
        esquerda: View,
        direita: View
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        row.addView(
            esquerda,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        row.addView(
            direita,
            LinearLayout.LayoutParams(-2, -2)
        )

        pai.addView(
            row,
            LinearLayout.LayoutParams(-1, -2)
        )
    }

    private fun botao(
        pai: LinearLayout,
        nome: String,
        cor: Int = cartao2,
        acao: (() -> Unit)? = null
    ) {
        val b = TextView(this).apply {
            text = nome
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(branco)
            setPadding(dp(14), dp(11), dp(14), dp(11))
            background = fundoArredondado(cor, 12)
            if (acao != null) {
                setOnClickListener { acao() }
            }
        }

        pai.addView(
            b,
            LinearLayout.LayoutParams(-2, -2).apply {
                rightMargin = dp(7)
                bottomMargin = dp(5)
            }
        )
    }

    private fun titulo(
        pai: LinearLayout,
        nome: String,
        descricao: String = ""
    ) {
        pai.addView(
            texto(nome, 21f, branco, true)
        )

        if (descricao.isNotEmpty()) {
            espaco(pai, 4)
            pai.addView(texto(descricao, 12f, cinza))
        }

        espaco(pai, 17)
    }

    private fun mostrarPagina(nome: String) {
        paginaAtual = nome
        content.removeAllViews()

        when (nome) {
            "Início" -> mostrarInicio()
            "Fila" -> mostrarFila()
            "Conclusão" -> mostrarConclusao()
            "Pagamentos" -> mostrarPagamentos()
            "Config" -> mostrarConfiguracoes()
            "SIMs" -> mostrarSIMs()
            "Diagnóstico" -> mostrarDiagnostico()
        }

        atualizarNavegacao()
    }

    // --------------------------------------------------
    // INÍCIO
    // --------------------------------------------------

    private fun mostrarInicio() {
        val cabecalho = painel(content, 18)
        cabecalho.background = fundoArredondado(vermelho, 17)

        cabecalho.addView(
            texto("NGUIRAZE NET", 23f, branco, true)
        )

        espaco(cabecalho, 5)
        cabecalho.addView(
            texto("PAINEL DE CONTROLO • 24/24H", 12f, branco)
        )

        espaco(content, 2)

        val estado = painel(content)

        estado.addView(texto("ESTADO DO SISTEMA", 12f, cinza, true))
        espaco(estado, 10)

        linha(
            estado,
            texto("Automação", 15f, branco, true),
            texto("ESTRUTURA VISUAL", 11f, amarelo, true)
        )

        espaco(estado, 8)
        estado.addView(
            texto(
                "As funções automáticas serão ligadas na próxima fase.",
                12f,
                cinza
            )
        )

        val atalhos = painel(content)
        atalhos.addView(texto("ACESSO RÁPIDO", 14f, branco, true))
        espaco(atalhos, 12)

        val row1 = LinearLayout(this)
        row1.orientation = LinearLayout.HORIZONTAL

        botao(row1, "📋 Fila") { mostrarPagina("Fila") }
        botao(row1, "✅ Conclusão") { mostrarPagina("Conclusão") }

        atalhos.addView(row1)

        val row2 = LinearLayout(this)
        row2.orientation = LinearLayout.HORIZONTAL

        botao(row2, "📱 SIMs") { mostrarPagina("SIMs") }
        botao(row2, "💳 Pagamentos") { mostrarPagina("Pagamentos") }

        atalhos.addView(row2)

        val resumo = painel(content)
        resumo.addView(texto("RESUMO DO DIA", 14f, branco, true))
        espaco(resumo, 12)

        indicador(resumo, "Transferências concluídas", "0", verde)
        indicador(resumo, "Transferências em curso", "0", azul)
        indicador(resumo, "Pedidos em espera", pedidosFila.size.toString(), amarelo)
        indicador(resumo, "Erros registados", "0", vermelho)

        val sim = painel(content)
        sim.addView(texto("CARTÕES SIM", 14f, branco, true))
        espaco(sim, 8)

        sim.addView(
            texto("Configuração e controlo dos cartões SIM.", 12f, cinza)
        )

        espaco(sim, 10)
        botao(sim, "GERIR SIMs", vermelho) {
            mostrarPagina("SIMs")
        }

        val diagnostico = painel(content)
        diagnostico.addView(texto("DIAGNÓSTICO USSD", 14f, branco, true))
        espaco(diagnostico, 7)
        diagnostico.addView(
            texto("Consultar as leituras recolhidas pelo serviço de acessibilidade.", 12f, cinza)
        )
        espaco(diagnostico, 8)
        botao(diagnostico, "ABRIR DIAGNÓSTICO", cartao2) {
            mostrarPagina("Diagnóstico")
        }
    }

    private fun indicador(
        pai: LinearLayout,
        nome: String,
        valor: String,
        cor: Int
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        row.addView(
            texto("●  $nome", 13f, cinza),
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        row.addView(
            texto(valor, 17f, cor, true)
        )

        pai.addView(row)
        espaco(pai, 10)
    }

    // --------------------------------------------------
    // FILA
    // --------------------------------------------------

    val pesquisa = EditText(this).apply {
    hint = "Pesquisar número..."
    setHintTextColor(cinza)
    setTextColor(branco)
    textSize = 14f
    maxLines = 1
    inputType = InputType.TYPE_CLASS_TEXT
    setSingleLine(true)
    setPadding(dp(14), dp(12), dp(14), dp(12))
    background = fundoArredondado(cartao, 13, Color.rgb(40, 48, 62))
    }

        content.addView(
            pesquisa,
            LinearLayout.LayoutParams(-1, dp(48)).apply {
                bottomMargin = dp(14)
            }
        )

        val estados = painel(content)
        estados.addView(texto("ESTADO GERAL", 13f, branco, true))
        espaco(estados, 12)

        indicador(
            estados,
            "Online",
            executores.count { it.estado == "LIVRE" }.toString(),
            verde
        )

        indicador(
            estados,
            "Em curso",
            executores.sumOf { it.enviosEmCurso }.toString(),
            azul
        )

        indicador(
            estados,
            "Em espera",
            pedidosFila.size.toString(),
            amarelo
        )

        indicador(
            estados,
            "Bloqueados",
            executores.count { it.estado == "BLOQUEADO" }.toString(),
            vermelho
        )

        val manual = painel(content)
        manual.addView(texto("ENVIO MANUAL", 15f, branco, true))
        espaco(manual, 5)
        manual.addView(texto("Adicionar um pedido à fila.", 12f, cinza))
        espaco(manual, 12)

        val numero = EditText(this).apply {
            hint = "Número do cliente"
            inputType = InputType.TYPE_CLASS_PHONE
            setTextColor(branco)
            setHintTextColor(cinza)
            background = fundoArredondado(cartao2, 12)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        manual.addView(
            numero,
            LinearLayout.LayoutParams(-1, dp(48))
        )

        espaco(manual, 9)

        val mb = EditText(this).apply {
            hint = "Quantidade de MB"
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextColor(branco)
            setHintTextColor(cinza)
            background = fundoArredondado(cartao2, 12)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        manual.addView(
            mb,
            LinearLayout.LayoutParams(-1, dp(48))
        )

        espaco(manual, 12)

        botao(manual, "＋  ADICIONAR À FILA", vermelho) {
            val n = numero.text.toString().trim()
            val quantidade = mb.text.toString().toIntOrNull()

            if (n.length < 9) {
                Toast.makeText(this, "Verifica o número do cliente.", Toast.LENGTH_SHORT).show()
            } else if (quantidade == null || quantidade < 100 || quantidade > 10240) {
                Toast.makeText(this, "Indica uma quantidade entre 100 e 10240 MB.", Toast.LENGTH_SHORT).show()
            } else {
                pedidosFila.add(
                    Pedido(
                        n,
                        quantidade,
                        "Por atribuir",
                        "--",
                        "EM FILA"
                    )
                )

                Toast.makeText(
                    this,
                    "Pedido adicionado à fila.",
                    Toast.LENGTH_SHORT
                ).show()

                mostrarPagina("Fila")
            }
        }

        val equipa = painel(content)
        linha(
            equipa,
            texto("EXECUTORES", 14f, branco, true),
            texto("${executores.size} registados", 11f, cinza)
        )

        espaco(equipa, 12)

        executores.forEachIndexed { index, executor ->
            cartaoExecutor(equipa, executor, index)
        }

        botao(equipa, "＋ ADICIONAR EXECUTOR", cartao2) {
            adicionarExecutor()
        }

        val curso = painel(content)
        curso.addView(texto("EM CURSO AGORA", 14f, branco, true))
        espaco(curso, 10)

        val emCurso = transferencias.filter {
            it.estado == "EM CURSO"
        }

        if (emCurso.isEmpty()) {
            curso.addView(texto("Nenhuma transferência em curso.", 12f, cinza))
        } else {
            emCurso.forEach {
                cartaoTransferencia(curso, it)
            }
        }

        val fila = painel(content)
        fila.addView(texto("FILA DE ESPERA", 14f, branco, true))
        espaco(fila, 10)

        if (pedidosFila.isEmpty()) {
            fila.addView(texto("Não existem pedidos em espera.", 12f, cinza))
        } else {
            pedidosFila.forEach {
                fila.addView(
                    texto(
                        "${it.numeroCliente}  •  ${it.quantidadeMB} MB",
                        13f,
                        branco,
                        true
                    )
                )
                fila.addView(texto(it.estado, 11f, amarelo))
                espaco(fila, 9)
            }
        }
    }

    private fun cartaoExecutor(
        pai: LinearLayout,
        executor: Executor,
        index: Int
    ) {
        val caixa = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = fundoArredondado(cartao2, 13)
        }

        pai.addView(
            caixa,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(9)
            }
        )

        linha(
            caixa,
            texto(executor.nome, 14f, branco, true),
            texto(executor.estado, 11f, corEstado(executor.estado), true)
        )

        espaco(caixa, 8)

        caixa.addView(
            texto(
                "Concluídos: ${executor.vendasConcluidas}   •   Falhas: ${executor.falhas}",
                11f,
                cinza
            )
        )

        espaco(caixa, 8)

        botao(caixa, "ALTERAR ESTADO", cartao) {
            val opcoes = arrayOf("LIVRE", "OFFLINE", "BLOQUEADO")

            AlertDialog.Builder(this)
                .setTitle("Estado do executor")
                .setItems(opcoes) { _, escolha ->
                    executor.estado = opcoes[escolha]
                    mostrarPagina("Fila")
                }
                .show()
        }
    }

    private fun adicionarExecutor() {
        val campo = EditText(this)
        campo.hint = "Nome do executor"

        AlertDialog.Builder(this)
            .setTitle("Novo executor")
            .setView(campo)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Adicionar") { _, _ ->
                val nome = campo.text.toString().trim()

                if (nome.isNotEmpty()) {
                    executores.add(Executor(nome, "OFFLINE"))
                    mostrarPagina("Fila")
                }
            }
            .show()
    }

    // --------------------------------------------------
    // CONCLUSÃO
    // --------------------------------------------------

    private fun mostrarConclusao() {
        val cabecalho = painel(content, 18)
        cabecalho.background = fundoArredondado(vermelho, 17)

        cabecalho.addView(
            texto("PAINEL DE CONCLUSÃO", 21f, branco, true)
        )

        espaco(cabecalho, 5)

        cabecalho.addView(
            texto("Acompanhamento das transferências", 12f, branco)
        )

        espaco(content, 4)

        val filtros = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val nomesFiltros = listOf(
            "TODOS",
            "CONCLUÍDOS",
            "ERROS",
            "INCERTO",
            "EM FILA"
        )

        nomesFiltros.forEach { nome ->
            val ativo = filtroConclusao == nome

            val item = TextView(this).apply {
                text = nome
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(if (ativo) branco else cinza)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = fundoArredondado(
                    if (ativo) vermelho else cartao,
                    30
                )

                setOnClickListener {
                    filtroConclusao = nome
                    mostrarPagina("Conclusão")
                }
            }

            filtros.addView(
                item,
                LinearLayout.LayoutParams(-2, -2).apply {
                    rightMargin = dp(6)
                }
            )
        }

        val scrollFiltros = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(filtros)
        }

        content.addView(
            scrollFiltros,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(15)
            }
        )

        val lista = when (filtroConclusao) {
            "CONCLUÍDOS" -> transferencias.filter {
                it.estado == "CONCLUÍDO"
            }

            "ERROS" -> transferencias.filter {
                it.estado == "ERRO"
            }

            "INCERTO" -> transferencias.filter {
                it.estado == "INCERTO"
            }

            "EM FILA" -> transferencias.filter {
                it.estado == "EM FILA"
            }

            else -> transferencias
        }

        val resumo = painel(content)

        linha(
            resumo,
            texto("REGISTOS", 13f, cinza, true),
            texto("${lista.size}", 21f, branco, true)
        )

        espaco(resumo, 5)

        resumo.addView(
            texto("Transferências apresentadas neste filtro", 11f, cinza)
        )

        espaco(content, 2)

        if (lista.isEmpty()) {
            val vazio = painel(content)
            vazio.gravity = Gravity.CENTER
            vazio.setPadding(dp(20), dp(35), dp(20), dp(35))

            vazio.addView(
                texto("▣", 35f, cinza, true).apply {
                    gravity = Gravity.CENTER
                }
            )

            espaco(vazio, 12)

            vazio.addView(
                texto("Nenhuma transferência encontrada", 14f, branco, true).apply {
                    gravity = Gravity.CENTER
                }
            )

            espaco(vazio, 7)

            vazio.addView(
                texto(
                    "Quando existirem registos reais, aparecerão aqui.",
                    12f,
                    cinza
                ).apply {
                    gravity = Gravity.CENTER
                }
            )
        } else {
            lista.forEach {
                cartaoTransferencia(content, it)
            }
        }

        val totalMB = transferencias
            .filter { it.estado == "CONCLUÍDO" }
            .sumOf { it.quantidadeMB }

        val totalHoje = painel(content)
        totalHoje.background = fundoArredondado(
            Color.rgb(12, 45, 35),
            17,
            verde
        )

        totalHoje.addView(
            texto("MB ENVIADOS HOJE", 12f, verde, true)
        )

        espaco(totalHoje, 7)

        totalHoje.addView(
            texto("${totalMB} MB", 25f, branco, true)
        )

        totalHoje.addView(
            texto("Somente transferências confirmadas", 11f, cinza)
        )
    }

    private fun cartaoTransferencia(
        pai: LinearLayout,
        t: Transferencia
    ) {
        val cor = corEstado(t.estado)

        val caixa = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = fundoArredondado(
                cartao,
                16,
                Color.rgb(42, 50, 63)
            )
        }

        pai.addView(
            caixa,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(11)
            }
        )

        linha(
            caixa,
            texto(t.numeroCliente, 16f, branco, true),
            texto(t.estado, 11f, cor, true)
        )

        espaco(caixa, 9)

        caixa.addView(
            texto("${t.quantidadeMB} MB", 19f, branco, true)
        )

        espaco(caixa, 5)

        caixa.addView(
            texto("Executor: ${t.executor}", 11f, cinza)
        )

        caixa.addView(
            texto("Data: ${t.dataHora}", 11f, cinza)
        )
    }

    // --------------------------------------------------
    // PAGAMENTOS
    // --------------------------------------------------

    private fun mostrarPagamentos() {
        titulo(content, "Pagamentos", "Gestão de pagamentos recebidos")

        val p = painel(content)
        p.addView(texto("M-PESA", 15f, branco, true))
        espaco(p, 8)
        p.addView(texto("Integração de pagamentos será ligada na próxima fase.", 12f, cinza))

        val e = painel(content)
        e.addView(texto("E-MOLA", 15f, branco, true))
        espaco(e, 8)
        e.addView(texto("Leitura de comprovativos e mensagens será implementada depois.", 12f, cinza))

        val historico = painel(content)
        historico.addView(texto("HISTÓRICO DE PAGAMENTOS", 14f, branco, true))
        espaco(historico, 8)
        historico.addView(texto("Ainda não existem registos.", 12f, cinza))
    }

    // --------------------------------------------------
    // CONFIGURAÇÕES
    // --------------------------------------------------

    private fun mostrarConfiguracoes() {
        titulo(content, "Configurações", "Preferências do sistema")

        val geral = painel(content)
        geral.addView(texto("SISTEMA", 14f, branco, true))
        espaco(geral, 12)

        linha(
            geral,
            texto("Limite diário por SIM", 13f, branco),
            texto("10 envios", 13f, amarelo, true)
        )

        espaco(geral, 12)

        linha(
            geral,
            texto("Transferências simultâneas", 13f, branco),
            texto("1", 13f, azul, true)
        )

        espaco(geral, 12)

        linha(
            geral,
            texto("Confirmação automática", 13f, branco),
            texto("Por configurar", 12f, amarelo, true)
        )

        val admin = painel(content)
        admin.addView(texto("ADMINISTRAÇÃO", 14f, branco, true))
        espaco(admin, 10)

        botao(admin, "GERIR EXECUTORES", cartao2) {
            mostrarPagina("Fila")
        }

        botao(admin, "GERIR CARTÕES SIM", cartao2) {
            mostrarPagina("SIMs")
        }

        botao(admin, "DIAGNÓSTICO USSD", cartao2) {
            mostrarPagina("Diagnóstico")
        }
    }

    // --------------------------------------------------
    // SIMs
    // --------------------------------------------------

    private fun mostrarSIMs() {
        titulo(content, "Cartões SIM", "Gestão dos cartões e limites diários")

        val painelSim = painel(content)

        painelSim.addView(texto("SIM 1", 16f, branco, true))
        espaco(painelSim, 7)

        painelSim.addView(texto("Estado: Por configurar", 12f, amarelo))
        espaco(painelSim, 7)

        painelSim.addView(texto("Transferências utilizadas: 0 / 10", 12f, cinza))
        espaco(painelSim, 7)

        painelSim.addView(texto("Saldo de dados: Não consultado", 12f, cinza))

        val painelSim2 = painel(content)

        painelSim2.addView(texto("SIM 2", 16f, branco, true))
        espaco(painelSim2, 7)

        painelSim2.addView(texto("Estado: Por configurar", 12f, amarelo))
        espaco(painelSim2, 7)

        painelSim2.addView(texto("Transferências utilizadas: 0 / 10", 12f, cinza))
        espaco(painelSim2, 7)

        painelSim2.addView(texto("Saldo de dados: Não consultado", 12f, cinza))

        val nota = painel(content)
        nota.addView(texto("CONTROLO DE TRANSFERÊNCIAS", 13f, branco, true))
        espaco(nota, 8)

        nota.addView(
            texto(
                "Os contadores reais serão ligados às confirmações da operadora. Nenhum envio será contado sem confirmação.",
                12f,
                cinza
            )
        )
    }

    // --------------------------------------------------
    // DIAGNÓSTICO USSD
    // --------------------------------------------------

    private fun mostrarDiagnostico() {
        titulo(content, "Diagnóstico USSD", "Leituras do serviço de acessibilidade")

        val diagnostico = painel(content)

        val prefs = getSharedPreferences("ussd_debug", MODE_PRIVATE)

        val leitura = prefs.getString(
            "ultima_leitura",
            "Ainda não foi recolhida nenhuma leitura."
        )

        val janela = prefs.getString(
            "ultima_janela",
            "Desconhecida"
        )

        diagnostico.addView(texto("ÚLTIMA JANELA", 12f, cinza, true))
        espaco(diagnostico, 6)

        diagnostico.addView(texto(janela ?: "Desconhecida", 13f, branco))

        espaco(diagnostico, 15)

        diagnostico.addView(texto("TEXTO DETETADO", 12f, cinza, true))
        espaco(diagnostico, 7)

        diagnostico.addView(
            texto(leitura ?: "Sem leitura", 13f, branco)
        )

        espaco(diagnostico, 15)

        botao(diagnostico, "ATUALIZAR LEITURA", vermelho) {
            mostrarPagina("Diagnóstico")
        }
    }

    // --------------------------------------------------
    // NAVEGAÇÃO INFERIOR
    // --------------------------------------------------

    private fun atualizarNavegacao() {
        navigation.removeAllViews()

        val paginas = listOf(
            "Início",
            "Fila",
            "Conclusão",
            "Pagamentos",
            "Config"
        )

        val icones = listOf(
            "⌂",
            "☷",
            "✓",
            "▣",
            "⚙"
        )

        paginas.forEachIndexed { index, nome ->
            val selecionado = paginaAtual == nome

            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(2), dp(4), dp(2), dp(4))
                setOnClickListener {
                    mostrarPagina(nome)
                }
            }

            item.addView(
                texto(
                    icones[index],
                    20f,
                    if (selecionado) vermelho else cinza,
                    true
                ).apply {
                    gravity = Gravity.CENTER
                }
            )

            item.addView(
                texto(
                    nome,
                    9f,
                    if (selecionado) branco else cinza,
                    selecionado
                ).apply {
                    gravity = Gravity.CENTER
                }
            )

            navigation.addView(
                item,
                LinearLayout.LayoutParams(0, -1, 1f)
            )
        }
    }
}
