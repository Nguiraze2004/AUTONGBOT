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

    private var ultimaRespostaEnviada = ""
    private var horaUltimaResposta = 0L
    private var ultimoCliqueSim = 0L

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

        val leitura = textos.distinct().joinToString("\n")
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

        Log.d("NGUIRAZE_USSD", "[$pacote | ativo=$sessaoValida]\n$leitura")

        // Se não há pedido real ativo, apenas monitoriza/regista diagnóstico e não toca em nada
        if (!sessaoValida) {
            if (pedidoAtivo && (agora - timestampPedido > 90_000L)) {
                pedidoPrefs.edit().putBoolean("ativo", false).apply()
            }
            return
        }

        // 1. Janela de escolha de SIM ("Fazer chamadas com" / seleção de cartão SIM)
        if (ehDialogoEscolhaSim(leitura)) {
            if (agora - ultimoCliqueSim < 1200L) return

            val simDesejado = pedidoPrefs.getInt("sim", 1).coerceIn(1, 2)
            if (selecionarCartaoSim(raiz, simDesejado)) {
                ultimoCliqueSim = agora
                Log.d("NGUIRAZE_USSD", "SIM $simDesejado selecionado automaticamente")
            }
            return
        }

        // 2. Confirmar sucesso da transferência
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

                        pedidoPrefs.edit().putBoolean("ativo", false).apply()
                        Log.d("NGUIRAZE_USSD", "SUCESSO: $megas MB enviados para $numero")
                    }
                }
            }
            return
        }

        // 3. Responder às etapas do menu *162#
        val numeroCliente = pedidoPrefs.getString("numero", "") ?: ""
        val megasPedido = pedidoPrefs.getInt("megas", 0)

        val resposta = escolherRespostaMenu(leitura, numeroCliente, megasPedido) ?: return

        if (resposta == ultimaRespostaEnviada && agora - horaUltimaResposta < 1800L) {
            return
        }

        if (preencherEEnviar(raiz, resposta, 0)) {
            ultimaRespostaEnviada = resposta
            horaUltimaResposta = agora
        }
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

        // Tentativa 1: procurar texto direto "SIM 1" ou "SIM 2"
        if (clicarPorTexto(root, rotuloAlvo, 0)) {
            return true
        }

        // Tentativa 2: procurar linha que comece por "1" ou "2" ou contenha o número do slot
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

        // Tentativa 3: fallback por índice na lista de opções clicáveis (0 = SIM 1, 1 = SIM 2)
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

    private fun escolherRespostaMenu(
        leitura: String,
        numeroCliente: String,
        megas: Int
    ): String? {
        val texto = leitura.lowercase()

        return when {
            (texto.contains("servicos de internet") ||
                texto.contains("serviços de internet") ||
                texto.contains("servico de internet") ||
                texto.contains("serviço de internet") ||
                texto.contains("8. servico") ||
                texto.contains("8. serviço")) &&
                !texto.contains("transferir megas") -> "8"

            texto.contains("transferir megas") ||
                texto.contains("transferir") -> "2"

            texto.contains("megas") ||
                texto.contains("saldo") ||
                texto.contains("quantidade") ||
                texto.contains("valor") ||
                texto.contains("montante") -> {
                if (megas > 0) megas.toString() else null
            }

            texto.contains("recipiente") ||
                texto.contains("número") ||
                texto.contains("numero") ||
                texto.contains("destino") ||
                texto.contains("contacto") -> {
                numeroCliente.ifBlank { null }
            }

            texto.contains("confirmar") &&
                (texto.contains("1.") || texto.contains("1 ")) -> "1"

            else -> null
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
