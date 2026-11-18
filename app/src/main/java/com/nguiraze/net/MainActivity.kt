
package com.nguiraze.net

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private val fundo = Color.rgb(9, 9, 11)
    private val vermelho = Color.rgb(220, 25, 40)
    private val branco = Color.WHITE
    private val cinza = Color.rgb(160, 160, 160)

    private lateinit var conteudo: LinearLayout
    private lateinit var menu: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = fundo
        window.navigationBarColor = fundo

        val principal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(fundo)
        }

        val topo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 20)
        }

        val titulo = TextView(this).apply {
            text = "NGUIRAZE NET"
            textSize = 25f
            setTextColor(branco)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val estado = TextView(this).apply {
            text = "● SISTEMA PRONTO"
            textSize = 12f
            setTextColor(Color.rgb(60, 210, 110))
            setPadding(0, 8, 0, 0)
        }

        topo.addView(titulo)
        topo.addView(estado)
        principal.addView(topo)

        conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        val scroll = android.widget.ScrollView(this)
        scroll.addView(conteudo)

        principal.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        menu = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(18, 18, 21))
            setPadding(3, 12, 3, 12)
        }

        principal.addView(menu)

        setContentView(principal)

        mostrarInicio()
        atualizarMenu("INÍCIO")
    }

    private fun adicionarTexto(
        texto: String,
        tamanho: Float,
        cor: Int,
        negrito: Boolean = false
    ) {
        val item = TextView(this).apply {
            text = texto
            textSize = tamanho
            setTextColor(cor)
            if (negrito) {
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            setPadding(0, 10, 0, 10)
        }
        conteudo.addView(item)
    }

    private fun mostrarInicio() {
        conteudo.removeAllViews()

        adicionarTexto("BEM-VINDO AO PAINEL", 13f, cinza, true)
        adicionarTexto("NGUIRAZE NET", 26f, branco, true)

        val cartao = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 22, 20, 22)
            setBackgroundColor(Color.rgb(35, 16, 20))
        }

        val saldo = TextView(this).apply {
            text = "SALDO DISPONÍVEL"
            textSize = 13f
            setTextColor(cinza)
        }

        val valor = TextView(this).apply {
            text = "0 MB"
            textSize = 32f
            setTextColor(branco)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 8, 0, 0)
        }

        cartao.addView(saldo)
        cartao.addView(valor)

        conteudo.addView(
            cartao,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        )

        adicionarTexto("RESUMO DO SISTEMA", 16f, branco, true)
        adicionarTexto("Pedidos na fila             0", 15f, cinza)
        adicionarTexto("Megas enviados              0", 15f, cinza)
        adicionarTexto("Pagamentos recebidos        0", 15f, cinza)

        adicionarTexto("ESTADO DO SERVIÇO", 16f, branco, true)
        adicionarTexto("Sistema preparado para iniciar.", 14f, cinza)
    }

    private fun mostrarPagina(nome: String) {
        conteudo.removeAllViews()
        adicionarTexto(nome, 25f, branco, true)
        adicionarTexto("Área de $nome", 16f, cinza)
        adicionarTexto("Esta secção será desenvolvida na próxima etapa.", 14f, cinza)
    }

    private fun atualizarMenu(selecionado: String) {
        menu.removeAllViews()

        val nomes = listOf(
            "INÍCIO",
            "FILA",
            "CONCLUSÃO",
            "PAGAMENTOS",
            "CONFIGURAÇÃO"
        )

        for (nome in nomes) {
            val item = TextView(this).apply {
                text = nome
                textSize = 9f
                gravity = Gravity.CENTER
                setPadding(4, 12, 4, 12)
                setTextColor(
                    if (nome == selecionado) vermelho else cinza
                )
                setTypeface(
                    null,
                    if (nome == selecionado)
                        android.graphics.Typeface.BOLD
                    else
                        android.graphics.Typeface.NORMAL
                )
                setOnClickListener {
                    if (nome == "INÍCIO") {
                        mostrarInicio()
                    } else {
                        mostrarPagina(nome)
                    }
                    atualizarMenu(nome)
                }
            }

            menu.addView(
                item,
                LinearLayout.LayoutParams(0, 55, 1f)
            )
        }
    }
}
