
package com.nguiraze.net

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private val fundo = Color.rgb(9, 9, 11)
    private val vermelho = Color.rgb(220, 38, 38)
    private val branco = Color.WHITE
    private val cinza = Color.rgb(161, 161, 170)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = fundo
        window.navigationBarColor = fundo

        val principal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 35, 24, 24)
            setBackgroundColor(fundo)
        }

        val topo = TextView(this).apply {
            text = "NGUIRAZE NET"
            textSize = 25f
            setTextColor(branco)
            typeface = Typeface.DEFAULT_BOLD
        }
        principal.addView(topo)

        val subtitulo = TextView(this).apply {
            text = "PAINEL DE CONTROLO"
            textSize = 12f
            setTextColor(cinza)
            setPadding(0, 5, 0, 25)
        }
        principal.addView(subtitulo)

        val cartao = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 22, 20, 22)
            setBackgroundColor(Color.rgb(35, 12, 15))
        }

        val estado = TextView(this).apply {
            text = "● SISTEMA PRONTO"
            textSize = 13f
            setTextColor(Color.rgb(248, 113, 113))
        }
        cartao.addView(estado)

        val titulo = TextView(this).apply {
            text = "Bem-vindo ao NGUIRAZE NET"
            textSize = 21f
            setTextColor(branco)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 18, 0, 8)
        }
        cartao.addView(titulo)

        val descricao = TextView(this).apply {
            text = "Gestão inteligente de megas e clientes."
            textSize = 14f
            setTextColor(cinza)
        }
        cartao.addView(descricao)

        principal.addView(cartao)

        val menuTitulo = TextView(this).apply {
            text = "MENU PRINCIPAL"
            textSize = 13f
            setTextColor(cinza)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 30, 0, 15)
        }
        principal.addView(menuTitulo)

        adicionarMenu(principal, "📡  Tabela de Megas", "Consultar e gerir preços")
        adicionarMenu(principal, "💰  Pagamentos", "M-Pesa e E-Mola")
        adicionarMenu(principal, "📨  Transferências", "Gestão de envio de megas")
        adicionarMenu(principal, "👥  Clientes", "Lista e histórico de clientes")
        adicionarMenu(principal, "📊  Relatórios", "Movimentos e resultados")
        adicionarMenu(principal, "⚙️  Definições", "Configuração do aplicativo")

        setContentView(principal)
    }

    private fun adicionarMenu(
        layout: LinearLayout,
        titulo: String,
        descricao: String
    ) {
        val cartao = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 15, 18, 15)
            setBackgroundColor(Color.rgb(24, 24, 27))
        }

        val nome = TextView(this).apply {
            text = titulo
            textSize = 17f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        val detalhe = TextView(this).apply {
            text = descricao
            textSize = 12f
            setTextColor(Color.rgb(161, 161, 170))
            setPadding(0, 5, 0, 0)
        }

        cartao.addView(nome)
        cartao.addView(detalhe)

        val parametros = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        parametros.bottomMargin = 12
        layout.addView(cartao, parametros)
    }
}
