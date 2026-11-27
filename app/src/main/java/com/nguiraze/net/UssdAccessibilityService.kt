package com.nguiraze.net

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class UssdAccessibilityService : AccessibilityService() {

    private val preferencias by lazy {
        getSharedPreferences("ussd_debug", MODE_PRIVATE)
    }

    private val pedidoPrefs by lazy {
        getSharedPreferences("ussd_pedido_ativo", MODE_PRIVATE)
    }

    // 1 = escolher "8", 2 = escolher "2", 3 = enviar megas, 4 = enviar numero, 5 = confirmar "1"
    private var etapaAtual = 1
    private var ultimaLeituraProcessada = ""
    private var ultimoEnvioMs = 0L
    private var ultimoCliqueSimMs = 0L

    private val padraoSucesso = Regex(
        """Transferiste\s+com\s+sucesso\s+(\d+)\s*MB.*?para\s+o\s+numero\s+(258\d{9})""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pacote = event.packageName?.toString() ?: "Desconhecida"
        if (pacote == "com.nguiraze.net") return

        val raiz = rootInActiveWindow ?: return
        val textos = mutableListOf<String>()

        event.text?.forEach {
            if (it.toString().isNotBlank()) textos.add(it.toString())
        }
        analisarElementos(raiz, 0, textos)

        val leitura = textos.distinct().joinToString("\n").trim()
        if (leitura.isBlank()) return

        val agora = System.currentTimeMillis()
        val pedidoAtivo = pedidoPrefs.getBoolean("ativo", false)
        val timestampPedido = pedidoPrefs.getLong("timestamp", 0L)
        val sessaoValida = pedidoAtivo && (agora - timestampPedido in 0..90_000L)

        preferencias.edit()
            .putString("ultima_leitura", leitura)
            .putString("ultima_janela", pacote)
            .putBoolean("sessao_ativa", sessaoValida)
            .apply()

        Log.d("NGUIRAZE_USSD", "[$pacote | etapa=$etapaAtual | ativo=$sessaoValida]\n$leitura")

        if (!sessaoValida) {
            etapaAtual = 1
            ultimaLeituraProcessada = ""
            if (pedidoAtivo && (agora - timestampPedido > 90_000L)) {
                pedidoPrefs.edit().putBoolean("ativo", false).apply()
            }
            return
        }

        // Só interage em janelas de telefone / sistema / USSD
        if (!ehJanelaSistemaOuTelefone(pacote, leitura)) {
            return
        }

        // 1. Seleção de SIM ("Fazer chamadas com")
        if (ehDialogoEscolhaSim(leitura)) {
            if (agora - ultimoCliqueSimMs < 1200L) return
            val simDesejado = pedidoPrefs.getInt("sim", 1).coerceIn(1, 2)
            if (selecionarCartaoSim(raiz, simDesejado)) {
                ultimoCliqueSimMs = agora
                etapaAtual = 1
                ultimaLeituraProcessada = ""
            }
            return
        }

        // 2. Confirmação final da Vodacom
        if (leitura.contains("Transferiste", ignoreCase = true)) {
            val textoLimpo = leitura.replace(Regex("\\s+"), " ").trim()
            val resultado = padraoSucesso.find(textoLimpo)
            if (resultado != null) {
                val megas = resultado.groupValues[1].toIntOrNull() ?: return
                val numero = resultado.groupValues[2]
                if (megas > 0) {
                    val assinatura = "$numero-$megas"
                    val assinaturaAnterior = preferencias.getString("ultima_assinatura", "")
                    val horaAnterior = preferencias.getLong("hora_ultima_assinatura", 0L)

                    if (assinatura != assinaturaAnterior || agora - horaAnterior >= 10000) {
                        preferencias.edit()
                            .putString("ultima_assinatura", assinatura)
                            .putLong("hora_ultima_assinatura", agora)
                            .putString("ultimo_resultado", "SUCESSO: $megas MB para $numero")
                            .apply()
                    }
                }
            }
            finalizarSessaoUssd()
            return
        }

        // Evita reprocessar exatamente a mesma leitura em loop rápido
        if (leitura == ultimaLeituraProcessada && (agora - ultimoEnvioMs) < 2200L) {
            return
        }

        // Aguarda um respiro mínimo entre envios para o operador atualizar a tela
        if ((agora - ultimoEnvioMs) < 900L) {
            return
        }

        val numeroCliente = (pedidoPrefs.getString("numero", "") ?: "").trim()
        val megasPedido = pedidoPrefs.getInt("megas", 0)

        val resposta = obterRespostaPorSequencia(leitura, numeroCliente, megasPedido) ?: return

        if (preencherEEnviar(raiz, resposta, 0)) {
            ultimoEnvioMs = agora
            ultimaLeituraProcessada = leitura
            avancarEtapaAposEnvio()
        }
    }

    private fun obterRespostaPorSequencia(
        leitura: String,
        numeroCliente: String,
        megas: Int
    ): String? {
        val t = leitura.lowercase()

        return when (etapaAtual) {
            1 -> {
                if (t.contains("internet") || t.contains("8.") || t.contains("wftti")) {
                    "8"
                } else {
                    null
                }
            }
            2 -> {
                if (t.contains("transferir") || t.contains("megas") || t.contains("2.")) {
                    "2"
                } else {
                    null
                }
            }
            3 -> {
                if (megas > 0) megas.toString() else null
            }
            4 -> {
                if (numeroCliente.isNotBlank()) numeroCliente else null
            }
            5 -> {
                if (t.contains("confirmar") || t.contains("1.") || t.contains("1 ")) {
                    "1"
                } else {
                    null
                }
            }
            else -> null
        }
    }

    private fun avancarEtapaAposEnvio() {
        etapaAtual = when (etapaAtual) {
            1 -> 2
            2 -> 3
            3 -> 4
            4 -> 5
            else -> 5
        }
    }

    private fun finalizarSessaoUssd() {
        etapaAtual = 1
        ultimaLeituraProcessada = ""
        pedidoPrefs.edit()
            .putBoolean("ativo", false)
            .apply()
    }

    private fun ehJanelaSistemaOuTelefone(pacote: String, leitura: String): Boolean {
        val p = pacote.lowercase()
        val l = leitura.lowercase()
        return p.contains("phone") ||
            p.contains("dialer") ||
            p.contains("incallui") ||
            p.contains("stk") ||
            p.contains("android") ||
            l.contains("enviar") ||
            l.contains("cancelar") ||
            l.contains("fazer chamadas com") ||
            l.contains("transferiste")
    }

    private fun ehDialogoEscolhaSim(leitura: String): Boolean {
        val t = leitura.lowercase()
        return t.contains("fazer chamadas com") ||
            t.contains("escolher sim") ||
            t.contains("selecionar sim") ||
            (t.contains("sim 1") && t.contains("sim 2"))
    }

    private fun selecionarCartaoSim(root: AccessibilityNodeInfo, simDesejado: Int): Boolean {
        val rotuloAlvo = if (simDesejado == 2) "SIM 2" else "SIM 1"

        if (clicarPorTexto(root, rotuloAlvo, 0)) {
            return true
        }

        val clicaveis = mutableListOf<AccessibilityNodeInfo>()
        coletarClicaveis(root, 0, clicaveis)

        for (item in clicaveis) {
            val textoItem = coletarTextoDoNo(item).trim()
            if (simDesejado == 1 && (textoItem.startsWith("1") || textoItem.contains("SIM 1", true))) {
                if (item.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            }
            if (simDesejado == 2 && (textoItem.startsWith("2") || textoItem.contains("SIM 2", true))) {
                if (item.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            }
        }

        val indice = simDesejado - 1
        if (indice in clicaveis.indices) {
            return clicaveis[indice].performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        return false
    }

    private fun coletarTextoDoNo(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        node.text?.let { sb.append(it).append(" ") }
        node.contentDescription?.let { sb.append(it).append(" ") }
        for (i in 0 until node.childCount) {
            val filho = node.getChild(i) ?: continue
            sb.append(coletarTextoDoNo(filho)).append(" ")
        }
        return sb.toString()
    }

    private fun coletarClicaveis(
        node: AccessibilityNodeInfo,
        nivel: Int,
        lista: MutableList<AccessibilityNodeInfo>
    ) {
        if (nivel > 8) return
        if (node.isClickable && node.isEnabled) {
            lista.add(node)
        }
        for (i in 0 until node.childCount) {
            val filho = node.getChild(i) ?: continue
            coletarClicaveis(filho, nivel + 1, lista)
        }
    }

    private fun clicarPorTexto(
        node: AccessibilityNodeInfo,
        alvo: String,
        nivel: Int
    ): Boolean {
        if (nivel > 8) return false

        val textoNo = (node.text?.toString() ?: "") + " " + (node.contentDescription?.toString() ?: "")
        if (textoNo.contains(alvo, ignoreCase = true)) {
            if (clicarNoOuPai(node)) return true
        }

        for (i in 0 until node.childCount) {
            val filho = node.getChild(i) ?: continue
            if (clicarPorTexto(filho, alvo, nivel + 1)) return true
        }
        return false
    }

    private fun clicarNoOuPai(node: AccessibilityNodeInfo): Boolean {
        var atual: AccessibilityNodeInfo? = node
        var passos = 0
        while (atual != null && passos < 4) {
            if (atual.isClickable) {
                return atual.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            atual = atual.parent
            passos++
        }
        return false
    }

    private fun preencherEEnviar(
        root: AccessibilityNodeInfo,
        texto: String,
        nivel: Int
    ): Boolean {
        val campoEditavel = encontrarCampoEditavel(root, nivel) ?: return false

        campoEditavel.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, texto)
        }
        val preencheu = campoEditavel.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (!preencheu) return false

        return clicarBotaoEnvio(root, 0)
    }

    private fun encontrarCampoEditavel(
        node: AccessibilityNodeInfo,
        nivel: Int
    ): AccessibilityNodeInfo? {
        if (nivel > 8) return null
        if (node.isEditable) return node

        for (i in 0 until node.childCount) {
            val filho = node.getChild(i) ?: continue
            val encontrado = encontrarCampoEditavel(filho, nivel + 1)
            if (encontrado != null) return encontrado
        }
        return null
    }

    private fun clicarBotaoEnvio(node: AccessibilityNodeInfo, nivel: Int): Boolean {
        if (nivel > 8) return false

        val texto = ((node.text?.toString() ?: "") + " " + (node.contentDescription?.toString() ?: ""))
            .trim()
        val ehBotaoEnviar = texto.equals("Enviar", true) ||
            texto.equals("OK", true) ||
            texto.equals("Send", true)

        if (ehBotaoEnviar && clicarNoOuPai(node)) {
            return true
        }

        for (i in 0 until node.childCount) {
            val filho = node.getChild(i) ?: continue
            if (clicarBotaoEnvio(filho, nivel + 1)) return true
        }
        return false
    }

    private fun analisarElementos(
        node: AccessibilityNodeInfo,
        nivel: Int,
        textos: MutableList<String>
    ) {
        if (nivel > 8) return

        node.text?.toString()?.let {
            if (it.isNotBlank()) textos.add(it)
        }

        node.contentDescription?.toString()?.let {
            if (it.isNotBlank()) textos.add(it)
        }

        for (i in 0 until node.childCount) {
            val filho = node.getChild(i) ?: continue
            analisarElementos(filho, nivel + 1, textos)
        }
    }

    override fun onInterrupt() {
        Log.d("NGUIRAZE_USSD", "Serviço interrompido")
    }
}
