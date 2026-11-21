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
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =========================================================================
// 1. MODELOS DE DADOS
// =========================================================================
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
    val status: String // "Entregue", "Falhou", "Incerto"
)

data class AgenteExecutor(
    val id: Int,
    val nome: String,
    val sigla: String,
    var online: Boolean,
    var vendas: Int,
    val simTag: String
)

// =========================================================================
// 2. ACTIVITY PRINCIPAL (NGUIRAZE NET - TEMA AZUL CIANO)
// =========================================================================
class MainActivity : Activity() {

    // Paleta de Cores (Dark Navy & Cyan)
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

    // Estado Operacional
    private var filaPausada = false
    private var gbVendidosHoje = 1980.0
    private var metaGb = 1000
    private var saldoHojeMT = 0
    private var simSelecionado = 2
    private var sim1Usados = 0
    private var sim2Usados = 0
    private val limiteSim = 10

    // Dados em Memória
    private var pedidoEmCurso: PedidoFila? = PedidoFila("0", "847337160", 10240, "14:39")

    private val filaEspera = mutableListOf(
        PedidoFila("1", "856942180", 1000, "14:40"),
        PedidoFila("2", "842910482", 500, "14:41")
    )

    private val executores = mutableListOf(
        AgenteExecutor(1, "Este aparelho", "ES", true, 4, "SIM 2 · Vodacom"),
        AgenteExecutor(2, "Agente 2", "AG", false, 0, "SIM 1 · Movitel")
    )

    private val concluidos = mutableListOf(
        TransacaoConcluida("847337160", 10240, 150, "23:24", "SIM 2 (Vodacom)", "Entregue"),
        TransacaoConcluida("853767234", 10240, 150, "23:17", "SIM 1 (Movitel)", "Entregue"),
        TransacaoConcluida("850387303", 10240, 150, "23:09", "SIM 2 (Vodacom)", "Entregue"),
        TransacaoConcluida("843119024", 5120, 75, "22:45", "SIM 1 (Vodacom)", "Falhou")
    )

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

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

        val scroll = ScrollView(this).apply { clipToPadding = false }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(24))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        navBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(6, 16, 40))
            setPadding(dp(4), dp(6), dp(4), dp(6))
        }
        root.addView(navBar, LinearLayout.LayoutParams(-1, dp(64)))

        setContentView(root)
        navegarPara("Início")
    }

    private fun navegarPara(aba: String) {
        abaAtual = aba
        renderizarBarraInferior()
        content.removeAllViews()

        when (aba) {
            "Início" -> telaInicio()
            "Fila" -> telaFilaExecutores()
            "Concluídos" -> telaConcluidos()
            "SIMs" -> telaSims()
            "Config" -> telaConfig()
        }
    }

    // =====================================================================
    // TELA 1: INÍCIO (DASHBOARD PRINCIPAL)
    // =====================================================================
    private fun telaInicio() {
        // Cabeçalho com Robô + Título + Online
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val avatar = txt("🤖", 19f, white).apply {
                gravity = Gravity.CENTER
                background = oval(Color.rgb(12, 42, 95), cyan)
            }
            addView(avatar, LinearLayout.LayoutParams(dp(42), dp(42)))

            val titleCol = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), 0, 0, 0)
                val sp = SpannableString("NGUIRAZE NET").apply {
                    setSpan(ForegroundColorSpan(cyan), 9, 12, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                addView(TextView(this@MainActivity).apply {
                    text = sp; textSize = 16f; setTextColor(white); setTypeface(null, Typeface.BOLD)
                })
                addView(txt("Versão instalada: 1.86 (86)", 11f, lightBlue))
            }
            addView(titleCol, LinearLayout.LayoutParams(0, -2, 1f))

            addView(pillBadge("● Online", green, greenBg, Color.rgb(18, 110, 95)))
        }
        content.addView(header)

        // Tag icone de megas
        content.addView(pillBadge("icone de megas", lightBlue, Color.rgb(10, 38, 92), borderBlue).apply {
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8); bottomMargin = dp(12) }
        })

        // Cartão Principal: Anel GB Vendidos + Resumo de Transferências
        val summaryCard = cardBox(cardNavy, borderBlue).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val gbStr = String.format(Locale("pt", "MZ"), "%.2f", gbVendidosHoje)
            addView(RingProgressView(this@MainActivity, 0.78f, gbStr, "GB vendidos"), LinearLayout.LayoutParams(dp(140), dp(140)))

            val rightStats = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), 0, 0, 0)
                val totalConcluidas = concluidos.count { it.status == "Entregue" } + 37
                addView(txt("$totalConcluidas", 28f, white, true))
                addView(txt("transferências", 13f, white))
                addView(txt("Concluídos hoje", 12f, green, true).apply { setPadding(0, 0, 0, dp(10)) })
                addView(txt("Meta: $metaGb GB", 12f, lightBlue))
                addView(txt("Saldo hoje: $saldoHojeMT MT", 12f, white, true))
            }
            addView(rightStats, LinearLayout.LayoutParams(0, -2, 1f))
        }
        content.addView(summaryCard)

        // Linha dos 4 Ícones: Enviar megas | Fila | Histórico | Meta
        val iconsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6); bottomMargin = dp(16) }
        }
        val atalhos = listOf(
            Triple("➤", "Enviar megas") { navegarPara("Fila") },
            Triple("☰", "Fila") { navegarPara("Fila") },
            Triple("▤", "Histórico") { navegarPara("Concluídos") },
            Triple("◉", "Meta") { Toast.makeText(this, "Meta atual: $metaGb GB", Toast.LENGTH_SHORT).show() }
        )
        atalhos.forEach { (ico, nome, acao) ->
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                val circle = txt(ico, 17f, white, true).apply {
                    gravity = Gravity.CENTER
                    background = oval(Color.rgb(14, 65, 165), Color.rgb(45, 135, 255))
                }
                addView(circle, LinearLayout.LayoutParams(dp(52), dp(52)))
                addView(txt(nome, 11f, white, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(5), 0, 0) })
                setOnClickListener { acao() }
            }
            iconsRow.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        }
        content.addView(iconsRow)

        // Cabeçalho CARTÕES SIM + Botão + Gerir SIMs
        val simHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
            addView(txt("CARTÕES SIM", 13f, lightBlue, true), LinearLayout.LayoutParams(0, -2, 1f))
