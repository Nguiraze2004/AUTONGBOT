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
private var filtroConclusao = "Todos"

// Estado Operacional  
private var filaPausada = false  
private var gbVendidosHoje = 1980.0  
private var metaGb = 100  
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
        addView(pillBadge("+ Gerir SIMs", cyan, cardDark, cyan).apply {

setOnClickListener { navegarPara("SIMs") }
})
}
content.addView(simHeader)
// Cartões SIM 1 e SIM 2 Lado a Lado
val simsRow = LinearLayout(this).apply {
orientation = LinearLayout.HORIZONTAL
layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) }
}
fun miniSimBox(num: Int, usados: Int): LinearLayout {
val ativo = simSelecionado == num
return cardBox(if (ativo) Color.rgb(10, 38, 36) else cardDark, if (ativo) green else borderBlue).apply {
val top = LinearLayout(this@MainActivity).apply {
orientation = LinearLayout.HORIZONTAL
addView(txt("SIM $num", 12f, if (ativo) green else lightBlue, true), LinearLayout.LayoutParams(0, -2, 1f))
if (ativo) addView(txt("●", 12f, green))
}
addView(top)
addView(txt("$usados/$limiteSim", 22f, white, true))
addView(txt("transferências", 11f, lightBlue).apply { setPadding(0, 0, 0, dp(8)) })
val btns = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
if (!ativo) {
btns.addView(smallActionBtn("Usar", green) {
simSelecionado = num; navegarPara("Início")
}, LinearLayout.LayoutParams(0, dp(30), 1f).apply { rightMargin = dp(4) })
}
btns.addView(smallActionBtn("Reset", yellow) {
if (num == 1) sim1Usados = 0 else sim2Usados = 0
navegarPara("Início")
}, LinearLayout.LayoutParams(0, dp(30), 1f))
addView(btns)
}
}
simsRow.addView(miniSimBox(1, sim1Usados), LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(6) })
simsRow.addView(miniSimBox(2, sim2Usados), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(6) })
content.addView(simsRow)
content.addView(txt("Reset às 00:00 · faltam 9h 46m", 11f, lightBlue).apply {
setPadding(0, 0, 0, dp(10))
})
// Barra FILA DE ESPERA + Retomar/Pausar
val filaBar = cardBox(cardDark, borderBlue).apply {
orientation = LinearLayout.HORIZONTAL
gravity = Gravity.CENTER_VERTICAL
addView(txt("FILA DE ESPERA (${filaEspera.size})", 12f, lightBlue, true), LinearLayout.LayoutParams(0, -2, 1f))
addView(smallActionBtn(if (filaPausada) "▶ Retomar" else "❚❚ Pausar", yellow) {
filaPausada = !filaPausada; navegarPara("Início")
}, LinearLayout.LayoutParams(dp(95), dp(32)))
}
content.addView(filaBar)
// Últimos Pedidos
content.addView(txt("Últimos pedidos", 15f, white, true).apply { setPadding(0, dp(4), 0, dp(8)) })
concluidos.take(3).forEach { item ->
content.addView(buildTransacaoRow(item))
}
}
// =====================================================================
// TELA 2: FILA & EXECUTORES (IGUAL À ÚLTIMA IMAGEM GERADA)
// =====================================================================
private fun telaFilaExecutores() {
// Cabeçalho Fila & Executores
val topBar = LinearLayout(this).apply {
orientation = LinearLayout.HORIZONTAL
gravity = Gravity.CENTER_VERTICAL
layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
val back = txt("‹", 24f, white, true).apply {
setPadding(0, 0, dp(12), 0)
setOnClickListener { navegarPara("Início") }
}
addView(back)
val titleCol = LinearLayout(this@MainActivity).apply {
orientation = LinearLayout.VERTICAL
addView(txt("Fila & Executores", 18f, white, true))
val pendentes = filaEspera.size + (if (pedidoEmCurso != null) 1 else 0)
addView(txt("$pendentes pedidos pendentes · ${executores.size} executores ativos", 11f, lightBlue))
}
addView(titleCol, LinearLayout.LayoutParams(0, -2, 1f))
addView(pillBadge(if (filaPausada) "● Pausada" else "● Ativa", if (filaPausada) yellow else green, greenBg, borderBlue))
}
content.addView(topBar)
// Botões Pausar Fila | Limpar
val controlRow = LinearLayout(this).apply {
orientation = LinearLayout.HORIZONTAL
layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
addView(smallActionBtn(if (filaPausada) "▶ Retomar Fila" else "❚❚ Pausar Fila", yellow) {
filaPausada = !filaPausada; navegarPara("Fila")
}, LinearLayout.LayoutParams(0, dp(38), 1f).apply { rightMargin = dp(6) })
addView(smallActionBtn("🗑 Limpar", red) {
filaEspera.clear(); navegarPara("Fila")
}, LinearLayout.LayoutParams(0, dp(38), 1f).apply { leftMargin = dp(6) })
}
content.addView(controlRow)
// Cartão ENVIO MANUAL
val manualCard = cardBox(cardNavy, borderBlue).apply {
addView(txt("➤ ENVIO MANUAL", 13f, cyan, true))
addView(txt("Despacha direto para o primeiro Agente livre · 100MB - 10GB", 11f, lightBlue).apply {
setPadding(0, dp(2), 0, dp(10))
})
val inputNumero = EditText(this@MainActivity).apply {
hint = "Número do cliente (ex: 84xxxxxxx)"
setText("847337160")
inputType = InputType.TYPE_CLASS_PHONE
setTextColor(white)
setHintTextColor(lightBlue)
textSize = 14f
setPadding(dp(12), dp(10), dp(12), dp(10))
background = roundRect(Color.rgb(6, 18, 42), borderBlue, 10)
}
val inputMegas = EditText(this@MainActivity).apply {
hint = "Megabytes (ex: 100)"
setText("10240")
inputType = InputType.TYPE_CLASS_NUMBER
setTextColor(white)
setHintTextColor(lightBlue)
textSize = 14f
setPadding(dp(12), dp(10), dp(12), dp(10))
background = roundRect(Color.rgb(6, 18, 42), borderBlue, 10)
}
// Pacotes Rápidos
val pacotesRow = LinearLayout(this@MainActivity).apply {
orientation = LinearLayout.HORIZONTAL
layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
}
listOf("450MB · 10 MT" to "450", "1GB · 20 MT" to "1024", "10GB · 150 MT" to "10240").forEachIndexed { idx, (labelPct, mbVal) ->
val chip = txt(labelPct, 11f, if (idx == 0) green else white, true).apply {
gravity = Gravity.CENTER
setPadding(dp(6), dp(6), dp(6), dp(6))
background = roundRect(Color.rgb(6, 22, 48), if (idx == 0) green else borderBlue, 16)
setOnClickListener { inputMegas.setText(mbVal) }
}
pacotesRow.addView(chip, LinearLayout.LayoutParams(0, -2, 1f).apply {
if (idx > 0) leftMargin = dp(6)
})
}
addView(pacotesRow)
addView(inputNumero, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
addView(inputMegas, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
addView(txt("Aceite entre 100MB e 10240MB (10GB).", 10f, lightBlue).apply { setPadding(0, 0, 0, dp(10)) })
val btnDespachar = Button(this@MainActivity).apply {
text = "➤  Despachar para um Agente"
isAllCaps = false
setTextColor(white)
setTypeface(null, Typeface.BOLD)
background = GradientDrawable(
GradientDrawable.Orientation.LEFT_RIGHT,
intArrayOf(Color.rgb(12, 115, 195), Color.rgb(0, 175, 225))
).apply { cornerRadius = dp(12).toFloat() }
setOnClickListener {
val num = inputNumero.text.toString().trim()
val mb = inputMegas.text.toString().toIntOrNull() ?: 0
if (num.length < 9 || mb < 100) {
Toast.makeText(this@MainActivity, "Preencha número válido e mínimo 100MB", Toast.LENGTH_SHORT).show()
return@setOnClickListener
}
val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
filaEspera.add(PedidoFila("${filaEspera.size + 1}", num, mb, hora))
Toast.makeText(this@MainActivity, "Adicionado à fila para o Agente!", Toast.LENGTH_SHORT).show()
navegarPara("Fila")
}
}
addView(btnDespachar, LinearLayout.LayoutParams(-1, dp(44)))
}
content.addView(manualCard)
// Secção EXECUTORES (2)
content.addView(txt("EXECUTORES (${executores.size})", 12f, lightBlue, true).apply { setPadding(0, dp(4), 0, dp(8)) })
val execRow = LinearLayout(this).apply {
orientation = LinearLayout.HORIZONTAL
layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
}
executores.forEachIndexed { idx, ag ->
val cardAg = cardBox(if (ag.online) Color.rgb(10, 36, 38) else cardDark, if (ag.online) green else borderBlue).apply {
val top = LinearLayout(this@MainActivity).apply {
orientation = LinearLayout.HORIZONTAL
gravity = Gravity.CENTER_VERTICAL
val badgeSigla = txt(ag.sigla, 12f, if (ag.online) green else yellow, true).apply {
gravity = Gravity.CENTER
background = oval(Color.rgb(16, 48, 44), if (ag.online) green else yellow)
}
addView(badgeSigla, LinearLayout.LayoutParams(dp(32), dp(32)))
val nameCol = LinearLayout(this@MainActivity).apply {
orientation = LinearLayout.VERTICAL
setPadding(dp(8), 0, 0, 0)
addView(txt(ag.nome, 13f, white, true))
addView(txt("#${ag.id}", 11f, lightBlue))
}
addView(nameCol)
}
addView(top)
addView(txt(if (ag.online) "● Online" else "● Standby", 11f, if (ag.online) green else yellow, true).apply {
setPadding(0, dp(6), 0, dp(8))
})
val bot = LinearLayout(this@MainActivity).apply {
orientation = LinearLayout.HORIZONTAL
gravity = Gravity.BOTTOM
val vCol = LinearLayout(this@MainActivity).apply {
orientation = LinearLayout.VERTICAL
addView(txt("${ag.vendas}", 18f, if (ag.online) green else white, true))
addView(txt("vendas", 10f, lightBlue))
}
addView(vCol, LinearLayout.LayoutParams(0, -2, 1f))
addView(pillBadge(ag.simTag, lightBlue, Color.rgb(14, 34, 68), borderBlue))
}
addView(bot)
setOnClickListener {
ag.online = !ag.online
navegarPara("Fila")
}
}
execRow.addView(cardAg, LinearLayout.LayoutParams(0, -2, 1f).apply {
if (idx == 0) rightMargin = dp(5) else leftMargin = dp(5)
})
}
content.addView(execRow)
// Secção EM CURSO AGORA
content.addView(txt("⌛ EM CURSO AGORA", 12f, lightBlue, true).apply { setPadding(0, 0, 0, dp(6)) })
val runningCard = cardBox(Color.rgb(8, 32, 72), cyan).apply {
val atual = pedidoEmCurso
if (atual != null) {
val r1 = LinearLayout(this@MainActivity).apply {
orientation = LinearLayout.HORIZONTAL
addView(txt(atual.numeroCliente, 16f, white, true), LinearLayout.LayoutParams(0, -2, 1f))
addView(txt("${atual.megas} MB", 15f, cyan, true))
}
addView(r1)
addView(txt("Enviando via Este aparelho #1 (SIM 2)", 11f, lightBlue).apply { setPadding(0, dp(2), 0, dp(8)) })
addView(txt("◌ Discando comando USSD *111#...", 11f, cyan).apply {
setPadding(dp(10), dp(6), dp(10), dp(6))
background = roundRect(Color.rgb(6, 20, 48), borderBlue, 8)
})
} else {
addView(txt("Nenhuma ordem em curso no momento.", 12f, lightBlue))
}
}
content.addView(runningCard)
// Secção FILA DE ESPERA
content.addView(txt("FILA DE ESPERA (${filaEspera.size})", 12f, lightBlue, true).apply { setPadding(0, dp(6), 0, dp(8)) })
filaEspera.forEachIndexed { index, ped ->
val pRow = cardBox(cardDark, borderBlue).apply {
orientation = LinearLayout.HORIZONTAL
gravity = Gravity.CENTER_VERTICAL
val info = LinearLayout(this@MainActivity).apply {
orientation = LinearLayout.VERTICAL
addView(txt("#${index + 1} · ${ped.numeroCliente}", 14f, white, true))
addView(txt("${ped.megas} MB · ${ped.hora}", 11f, lightBlue))
}
addView(info, LinearLayout.LayoutParams(0, -2, 1f))
addView(smallActionBtn("▲ Subir", cyan) {
if (index > 0) {
val item = filaEspera.removeAt(index)
filaEspera.add(0, item)
navegarPara("Fila")
}
}, LinearLayout.LayoutParams(dp(72), dp(30)).apply { rightMargin = dp(6) })
addView(smallActionBtn("✕ Remover", red) {
filaEspera.removeAt(index)
navegarPara("Fila")
}, LinearLayout.LayoutParams(dp(84), dp(30)))
}
content.addView(pRow)
}
}
// =====================================================================
// TELA 3: CONCLUÍDOS
// =====================================================================

private fun telaConcluidos() {

    content.addView(txt("Conclusão", 19f, white, true))

    content.addView(
        txt("Histórico de transferências", 12f, lightBlue).apply {
            setPadding(0, 0, 0, dp(14))
        }
    )

    // FILTROS
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

    // SELECIONAR REGISTOS
    val listaConcluidos = when (filtroConclusao) {
    "Todos" -> concluidos
    "Entregue" -> concluidos.filter { it.status == "Entregue" }
    "Falhou" -> concluidos.filter { it.status == "Falhou" }
    "Incerto" -> concluidos.filter { it.status == "Incerto" }
    "Em fila" -> emptyList()
    else -> emptyList()
    }

    val mostrarFila = filtroConclusao == "Todos" ||
                      filtroConclusao == "Em fila"

    val total = listaConcluidos.size +
            if (mostrarFila) filaEspera.size else 0

    content.addView(
        txt("$total REGISTOS", 12f, cyan, true).apply {
            setPadding(0, 0, 0, dp(10))
        }
    )

    // REGISTOS CONCLUÍDOS
    listaConcluidos.forEach { item ->
        content.addView(buildTransacaoRow(item))
    }

    // PEDIDOS NA FILA
    if (mostrarFila) {
        filaEspera.forEach { pedido ->

            val linha = cardBox(cardDark, borderBlue).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val info = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL

                    addView(
                        txt(pedido.numeroCliente, 15f, white, true)
                    )

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

    // ESTADO VAZIO: SEM ÍCONE
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

// =====================================================================
// TELA 4: GESTÃO DE SIMs
// =====================================================================
private fun telaSims() {
content.addView(txt("Gestão de SIMs", 19f, white, true))
content.addView(txt("Reset diário às 00:00", 12f, lightBlue).apply { setPadding(0, 0, 0, dp(12)) })
listOf(1 to "VODACOM · 852194010", 2 to "MOVITEL · Slot 2").forEach { (id, desc) ->
val ativo = simSelecionado == id
val usados = if (id == 1) sim1Usados else sim2Usados
val box = cardBox(if (ativo) Color.rgb(10, 38, 36) else cardDark, if (ativo) green else borderBlue).apply {
addView(txt("SIM $id ${if (ativo) "· ACTIVO" else ""}", 16f, if (ativo) green else white, true))
addView(txt(desc, 12f, lightBlue))
addView(txt("Transferências: $usados / $limiteSim", 14f, white, true).apply { setPadding(0, dp(6), 0, dp(8)) })
if (!ativo) {
addView(smallActionBtn("Usar este SIM", green) {
simSelecionado = id; navegarPara("SIMs")
}, LinearLayout.LayoutParams(-1, dp(36)))
}
}
content.addView(box)
}
}
// =====================================================================
// TELA 5: CONFIGURAÇÕES
// =====================================================================
private fun telaConfig() {
content.addView(txt("Configurações", 19f, white, true))
content.addView(txt("Ajustes do motor USSD e Acessibilidade", 12f, lightBlue).apply { setPadding(0, 0, 0, dp(12)) })
content.addView(cardBox(cardDark, borderBlue).apply {
addView(txt("Serviço de Acessibilidade USSD", 15f, white, true))
addView(txt("Ative nas definições do Android para ler respostas da Vodacom/Movitel.", 12f, lightBlue))
})
}
// =====================================================================
// BARRA DE NAVEGAÇÃO INFERIOR & HELPERS VISUAIS
// =====================================================================
private fun renderizarBarraInferior() {
navBar.removeAllViews()
val abas = listOf("Início" to "⌂", "Fila" to "☰", "Concluídos" to "✓", "SIMs" to "▤", "Config" to "⚙")
abas.forEach { (nome, ico) ->
val sel = nome == abaAtual
val item = LinearLayout(this).apply {
orientation = LinearLayout.VERTICAL
gravity = Gravity.CENTER
setPadding(dp(2), dp(4), dp(2), dp(4))
if (sel) background = roundRect(Color.rgb(12, 46, 98), cyan, 12)
addView(txt(ico, 16f, if (sel) cyan else lightBlue, true).apply { gravity = Gravity.CENTER })
addView(txt(nome, 10f, if (sel) cyan else lightBlue, sel).apply { gravity = Gravity.CENTER })
setOnClickListener { navegarPara(nome) }
}
navBar.addView(item, LinearLayout.LayoutParams(0, -1, 1f).apply {
marginStart = dp(2); marginEnd = dp(2)
})
}
}
private fun buildTransacaoRow(item: TransacaoConcluida): LinearLayout {

val ok = item.status == "Entregue"
val incerto = item.status == "Incerto"

val statusText = if (ok) "✓ Entregue"
else if (incerto) "● Incerto"
else "✕ Falhou"

val corEstado = when {
    ok -> green
    incerto -> yellow
    else -> red
}

val fundoEstado = when {
    ok -> greenBg
    incerto -> Color.rgb(55, 43, 12)
    else -> Color.rgb(65, 18, 26)
}
return cardBox(cardDark, borderBlue).apply {
orientation = LinearLayout.HORIZONTAL
gravity = Gravity.CENTER_VERTICAL
val icon = txt(if (ok) "✓" else "✕", 14f, if (ok) green else red, true).apply {
gravity = Gravity.CENTER
background = oval(if (ok) greenBg else Color.rgb(65, 18, 26), if (ok) green else red)
}
addView(icon, LinearLayout.LayoutParams(dp(36), dp(36)))
val mid = LinearLayout(this@MainActivity).apply {
orientation = LinearLayout.VERTICAL
setPadding(dp(10), 0, 0, 0)
addView(txt(item.numeroCliente, 15f, white, true))
addView(txt("Megas · ${item.hora} · ${item.simInfo}", 11f, lightBlue))
}
addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
val right = LinearLayout(this@MainActivity).apply {
orientation = LinearLayout.VERTICAL
gravity = Gravity.END
addView(txt("${item.megas} MB", 13f, cyan, true))
addView(pillBadge(
if (ok) "✓ Entregue" else "✕ Falhou",
if (ok) green else red,
if (ok) greenBg else Color.rgb(65, 18, 26),
if (ok) green else red
))
}
addView(right)
}
}
private inner class RingProgressView(
ctx: Context,
private val pct: Float,
private val mainVal: String,
private val subVal: String
) : View(ctx) {
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
