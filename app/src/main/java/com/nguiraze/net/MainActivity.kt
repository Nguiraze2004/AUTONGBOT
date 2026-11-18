
package com.nguiraze.net

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private val fundo = Color.rgb(5, 8, 14)
    private val cartao = Color.rgb(14, 19, 28)
    private val branco = Color.WHITE
    private val cinza = Color.rgb(155, 165, 180)
    private val vermelho = Color.rgb(230, 15, 40)
    private val verde = Color.rgb(25, 210, 125)
    private val azul = Color.rgb(40, 135, 255)
    private val amarelo = Color.rgb(245, 180, 45)

    private lateinit var conteudo: LinearLayout
    private lateinit var barra: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = fundo
        window.navigationBarColor = fundo

        val principal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(fundo)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }

        conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 20)
        }

        scroll.addView(conteudo)

        principal.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        barra = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(12, 16, 24))
            setPadding(3, 7, 3, 7)
        }

        principal.addView(barra)

        setContentView(principal)

        mostrarInicio()
        atualizarBarra("Início")
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
            if (negrito) setTypeface(null, Typeface.BOLD)
        }
    }

    private fun espaco(altura: Int) {
        conteudo.addView(View(this), LinearLayout.LayoutParams(1, altura))
    }

    private fun titulo(valor: String) {
        val t = texto(valor, 13f, cinza, true)
        t.setPadding(2, 16, 2, 9)
        conteudo.addView(t)
    }

    private fun cartaoView(
        padding: Int = 14,
        cor: Int = cartao
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(cor)
        }
    }

    private fun adicionarCartao(view: View, margem: Int = 9) {
        conteudo.addView(
            view,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = margem
            }
        )
    }

    private fun linha(vararg views: View): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            views.forEach {
                addView(
                    it,
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        marginEnd = 5
                        marginStart = 5
                    }
                )
            }
        }
    }

    private fun mostrarInicio() {
        conteudo.removeAllViews()

        // CABEÇALHO
        val cabecalho = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(17, 18, 17, 18)
            setBackgroundColor(vermelho)
        }

        val marca = texto("N", 27f, branco, true)
        val nome = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(texto("NGUIRAZE NET", 21f, branco, true))
            addView(texto("● Conectado • Online", 12f, branco))
        }

        cabecalho.addView(marca)
        cabecalho.addView(
            nome,
            LinearLayout.LayoutParams(0, -2, 1f).apply {
                leftMargin = 15
            }
        )
        cabecalho.addView(texto("24H", 17f, branco, true))
        adicionarCartao(cabecalho, 12)

        // BOT ONLINE
        val bot = cartaoView()
        bot.addView(texto("🤖  BOT ONLINE", 18f, verde, true))
        bot.addView(texto("       Ligado ao sistema", 12f, cinza))
        adicionarCartao(bot)

        // AUTOMAÇÃO
        val automacao = cartaoView()
        automacao.addView(
            texto("⚙  Serviço de automação: ATIVO", 15f, branco, true)
        )
        automacao.addView(
            texto("    Transferências automáticas de megas", 12f, cinza)
        )
        adicionarCartao(automacao)

        // ATALHOS
        titulo("ACESSO RÁPIDO")

        adicionarAtalhos(
            listOf(
                "▥\nResumo" to verde,
                "📡\nSIMs" to vermelho,
                "◉\nCredencial" to azul,
                "ⓢ\nCrédito" to amarelo
            )
        )

        adicionarAtalhos(
            listOf(
                "⚙\nAvançado" to amarelo,
                "➤\nSMS" to verde,
                "⌛\nAgendar" to amarelo
            )
        )

        // VELOCIDADE USSD
        val ussd = cartaoView()
        ussd.addView(texto("▶  Velocidade do USSD", 15f, branco, true))
        ussd.addView(texto("     Modo: Automático • Toque para ajustar", 12f, cinza))
        adicionarCartao(ussd, 5)

        // ESTATÍSTICAS
        titulo("RESUMO DAS TRANSFERÊNCIAS")

        val estatisticas = linha(
            criarEstatistica("☰", "Na fila", "0", vermelho),
            criarEstatistica("✓", "Concluídas", "0", verde)
        )
        adicionarCartao(estatisticas)

        // META
        titulo("META DIÁRIA")

        val meta = cartaoView()
        meta.addView(texto("🎯  Meta de hoje", 15f, branco, true))
        meta.addView(texto("0 MT   •   0% concluído", 13f, cinza))
        meta.addView(texto("Meta ainda não configurada", 11f, cinza))
        adicionarCartao(meta)

        // ÚLTIMA VENDA
        titulo("ÚLTIMA VENDA")

        val venda = cartaoView()
        venda.addView(texto("Nenhuma venda registada", 14f, cinza))
        adicionarCartao(venda)

        // VENDAS POR HORA
        titulo("VENDAS POR HORA (HOJE)")

        val vendas = cartaoView()
        vendas.addView(texto("Ainda não existem vendas hoje", 13f, cinza))
        vendas.addView(texto("0 vendas  •  Faturação: 0 MT", 12f, verde))
        adicionarCartao(vendas)

        // CARTÕES SIM
        val tituloSim = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(
                texto("CARTÕES SIM", 13f, cinza, true),
                LinearLayout.LayoutParams(0, -2, 1f)
            )
            addView(texto("Gerir →", 12f, verde, true))
        }
        conteudo.addView(tituloSim)
        espaco(8)

        val sims = linha(
            criarSim("SIM 1"),
            criarSim("SIM 2")
        )
        adicionarCartao(sims)

        // FILA DE ESPERA
        titulo("FILA DE ESPERA")

        val fila = cartaoView()
        fila.addView(texto("◉  Sem pedidos na fila", 13f, cinza))
        adicionarCartao(fila)

        // EQUIPA
        titulo("EQUIPA")

        val equipa = cartaoView()
        equipa.addView(texto("Área de equipa", 14f, cinza))
        adicionarCartao(equipa)
    }

    private fun adicionarAtalhos(
        itens: List<Pair<String, Int>>
    ) {
        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        itens.forEach { item ->
            val botao = TextView(this).apply {
                text = item.first
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(item.second)
                setTypeface(null, Typeface.BOLD)
                setPadding(3, 13, 3, 13)
                setBackgroundColor(cartao)
            }

            linha.addView(
                botao,
                LinearLayout.LayoutParams(
                    0,
                    78,
                    1f
                ).apply {
                    marginEnd = 4
                    marginStart = 4
                    bottomMargin = 8
                }
            )
        }

        conteudo.addView(linha)
    }

    private fun criarEstatistica(
        simbolo: String,
        nome: String,
        numero: String,
        cor: Int
    ): LinearLayout {
        val caixa = cartaoView()
        caixa.addView(texto(simbolo + "  " + nome, 13f, cor, true))
        caixa.addView(texto(numero, 27f, branco, true))
        return caixa
    }

    private fun criarSim(nome: String): LinearLayout {
        val caixa = cartaoView(12)
        caixa.addView(texto(nome, 14f, verde, true))
        caixa.addView(texto("0 / 10", 24f, branco, true))
        caixa.addView(texto("Transferências", 11f, cinza))
        caixa.addView(texto("Limite diário: 10", 11f, cinza))
        return caixa
    }

    private fun mostrarPagina(nome: String) {
        conteudo.removeAllViews()

        espaco(20)
        conteudo.addView(texto("NGUIRAZE NET", 22f, vermelho, true))
        espaco(20)
        conteudo.addView(texto(nome, 25f, branco, true))
        espaco(12)

        val caixa = cartaoView(20)
        caixa.addView(texto("Secção $nome", 17f, branco, true))
        caixa.addView(
            texto(
                "Esta área será implementada na próxima etapa.",
                14f,
                cinza
            )
        )
        adicionarCartao(caixa)
    }

    private fun atualizarBarra(selecionado: String) {
        barra.removeAllViews()

        val nomes = listOf(
            "Início",
            "Fila",
            "Conclusão",
            "Pagamentos",
            "Configuração"
        )

        val icones = listOf("⌂", "☷", "✓", "ⓢ", "⚙")

        nomes.forEachIndexed { indice, nome ->
            val ativo = nome == selecionado

            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(2, 8, 2, 8)
                setBackgroundColor(
                    if (ativo) vermelho else Color.TRANSPARENT
                )

                addView(
                    texto(
                        icones[indice],
                        21f,
                        if (ativo) branco else cinza,
                        true
                    ).apply { gravity = Gravity.CENTER }
                )

                addView(
                    texto(
                        nome,
                        9f,
                        if (ativo) branco else cinza,
                        ativo
                    ).apply { gravity = Gravity.CENTER }
                )

                setOnClickListener {
                    if (nome == "Início") {
                        mostrarInicio()
                    } else {
                        mostrarPagina(nome)
                    }
                    atualizarBarra(nome)
                }
            }

            barra.addView(
                item,
                LinearLayout.LayoutParams(
                    0,
                    62,
                    1f
                )
            )
        }
    }
}
