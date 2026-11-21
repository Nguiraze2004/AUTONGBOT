package com.nguiraze.net

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
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
    val status: String // "Entregue", "Falhou", "Incerto", "Em fila"
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

    // Configurações Ajustáveis pelo Utilizador
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

    // Listas Reais (Começam vazias e crescem conforme os acontecimentos)
    private var pedidoEmCurso: PedidoFila? = null
    private val filaEspera = mutableListOf<PedidoFila>()
    private val concluidos = mutableListOf<TransacaoConcluida>()

    private val executores = mutableListOf(
        AgenteExecutor(1, "Este aparelho", "ES", true, 0, "SIM 1 · Vodacom"),
        AgenteExecutor(2, "Agente 2", "AG", false, 0, "SIM 2 · Movitel")
    )

    // =====================================================================
    // PROPRIEDADES CALCULADAS DINAMICAMENTE (ZERO VALORES FIXOS)
    // =====================================================================
    private val totalMegasEntreguesHoje: Long
        get() = concluidos.filter { it.status == "Entregue" }.sumOf { it.megas.toLong() }

    private val gbVendidosHoje: Double
        get() = totalMegasEntreguesHoje / 1024.0

    private val totalTransferenciasConcluidas: Int
        get() = concluidos.count { it.status == "Entregue" }

    private val saldoFaturadoHojeMT: Int
        get() = concluidos.filter { it.status == "Entregue" }.sumOf { it.valorMT }

    private val totalNaFila: Int
        get() = filaEspera.size + (if (pedidoEmCurso != null) 1 else 0)

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
    // MOTOR DE FILA E ACONTECIMENTOS (TRANSFORMA PEDIDOS EM CONCLUÍDOS)
    // =====================================================================
    private fun despacharPedido(numero: String, megas: Int, valorMT: Int) {
        val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val novo = PedidoFila("${System.currentTimeMillis()}", numero, megas, valorMT, hora)

        if (pedidoEmCurso == null && !filaPausada) {
            pedidoEmCurso = novo
        } else {
            filaEspera.add(novo)
        }
    }

    private fun concluirPedidoAtual(sucesso: Boolean = true) {
        val atual = pedidoEmCurso ?: return
        val dataHora = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val opNome = if (simSelecionado == 1) "SIM 1 (Vodacom)" else "SIM 2 (Movitel)"

        if (sucesso) {
            concluidos.add(0, TransacaoConcluida(atual.numeroCliente, atual.megas, atual.valorMT, dataHora, opNome, "Entregue"))
            if (simSelecionado == 1) sim1Usados++ else sim2Usados++
            executores.firstOrNull { it.online }?.let { it.vendas++ }

            // Auto-rotação se atingir o limite
            if (autoRotacaoSim && simSelecionado == 1 && sim1Usados >= limiteSim) {
                simSelecionado = 2
            }
        } else {
            concluidos.add(0, TransacaoConcluida(atual.numeroCliente, atual.megas, atual.valorMT, dataHora, opNome, "Falhou"))
        }

        // Puxa o próximo da fila automaticamente se não estiver pausada
        pedidoEmCurso = if (!filaPausada && filaEspera.isNotEmpty()) {
            filaEspera.removeAt(0)
        } else {
            null
        }
    }

    // =====================================================================
    // TELA 1: INÍCIO (100% REATIVO AOS ACONTECIMENTOS)
    // =====================================================================
    private fun telaInicio() {
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
                addView(txt("Automação USSD · ${if (filaPausada) "Pausada" else "Em escuta"}", 11f, lightBlue))
            }
            addView(titleCol, LinearLayout.LayoutParams(0, -2, 1f))

            addView(
                pillBadge(
                    if (filaPausada) "● Pausado" else "● Online",
                    if (filaPausada) yellow else green,
                    if (filaPausada) Color.rgb(60, 48, 12) else greenBg,
                    if (filaPausada) yellow else Color.rgb(18, 110, 95)
                )
            )
        }
        content.addView(header)

        content.addView(pillBadge("icone de megas", lightBlue, Color.rgb(10, 38, 92), borderBlue).apply {
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8); bottomMargin = dp(12) }
        })

        // Cartão Principal Dinâmico
        val summaryCard = cardBox(cardNavy, borderBlue).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val gbStr = String.format(Locale("pt", "MZ"), "%.2f", gbVendidosHoje)
            val progressoAnel = if (metaGb > 0) (gbVendidosHoje / metaGb).toFloat().coerceIn(0f, 1f) else 0f
            addView(RingProgressView(this@MainActivity, progressoAnel, gbStr, "GB vendidos"), LinearLayout.LayoutParams(dp(140), dp(140)))

            val rightStats = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), 0, 0, 0)
private val pBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
style = Paint.Style.STROKE; strokeWidth = dp(9).toFloat(); color = Color.rgb(14, 52, 118)
}
private val pFg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
style = Paint.Style.STROKE; strokeWidth = dp(9).toFloat(); strokeCap = Paint.Cap.ROUND; color = cyan
}
private val pBig = Paint(Paint.ANTI_ALIAS_FLAG).apply {
color = white; textSize = dp(19).toFloat(); typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
}
private val pSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
color = lightBlue; textSize = dp(11).toFloat(); textAlign = Paint.Align.CENTER
}
override fun onDraw(c: Canvas) {
val s = width.coerceAtMost(height).toFloat()
val pad = dp(12).toFloat()
val rect = RectF(pad, pad, s - pad, s - pad)
c.drawArc(rect, 0f, 360f, false, pBg)
c.drawArc(rect, -90f, 360f * pct, false, pFg)
c.drawText(mainVal, s / 2f, s / 2f + dp(2), pBig)
c.drawText(subVal, s / 2f, s / 2f + dp(18), pSub)
}
}
private fun cardBox(bg: Int, stroke: Int) = LinearLayout(this).apply {
orientation = LinearLayout.VERTICAL
setPadding(dp(12), dp(12), dp(12), dp(12))
layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
background = roundRect(bg, stroke, 14)
}
private fun smallActionBtn(label: String, stroke: Int, onClick: () -> Unit) = TextView(this).apply {
text = label
textSize = 11f
setTextColor(stroke)
setTypeface(null, Typeface.BOLD)
gravity = Gravity.CENTER
background = roundRect(Color.rgb(8, 22, 48), stroke, 10)
setOnClickListener { onClick() }
}
private fun pillBadge(text: String, textColor: Int, bg: Int, stroke: Int) = TextView(this).apply {
this.text = text
textSize = 10f
setTextColor(textColor)
setTypeface(null, Typeface.BOLD)
setPadding(dp(10), dp(4), dp(10), dp(4))
background = roundRect(bg, stroke, 20)
}
private fun roundRect(bg: Int, stroke: Int, radiusDp: Int) = GradientDrawable().apply {
setColor(bg); cornerRadius = dp(radiusDp).toFloat(); setStroke(dp(1), stroke)
}
private fun oval(bg: Int, stroke: Int) = GradientDrawable().apply {
shape = GradientDrawable.OVAL; setColor(bg); setStroke(dp(1), stroke)
}
private fun txt(t: String, s: Float, c: Int, b: Boolean = false) = TextView(this).apply {
text = t; textSize = s; setTextColor(c)
if (b) setTypeface(null, Typeface.BOLD)
}
        }
