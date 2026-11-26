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

        preferencias.edit()
            .putString("ultima_leitura", leitura)
            .putString("ultima_janela", pacote)
            .apply()

        Log.d("NGUIRAZE_USSD", leitura)

        // 1. Janela "Fazer chamadas com" -> escolher SIM 1 ou SIM 2
        if (leitura.contains("Fazer chamadas com", ignoreCase = true)) {
            val simDesejado = pedidoPrefs.getInt("sim", 1)
            val alvo = if (simDesejado == 2) "SIM 2" else "SIM 1"
            if (clicarPorTexto(raiz, alvo, 0)) {
                return
            }
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
                    val agora = System.currentTimeMillis()
                    val assinaturaAnterior = preferencias.getString("ultima_assinatura", "")
                    val horaAnterior = preferencias.getLong("hora_ultima_assinatura", 0L)

                    if (assinatura != assinaturaAnterior || agora - horaAnterior >= 10000) {
                        preferencias.edit()
                            .putString("ultima_assinatura", assinatura)
                            .putLong("hora_ultima_assinatura", agora)
                            .apply()
                        Log.d("NGUIRAZE_USSD", "SUCESSO: $megas MB enviados para $numero")
                    }
                }
            }
            return
        }

        // 3. Responder às etapas do menu *162# se houver pedido recente (até 2 min)
        val agora = System.currentTimeMillis()
        val timestampPedido = pedidoPrefs.getLong("timestamp", 0L)
        if (agora - timestampPedido > 120_000L) return

        val numeroCliente = pedidoPrefs.getString("numero", "") ?: ""
        val megasPedido = pedidoPrefs.getInt("megas", 0)

        val resposta = escolherRespostaMenu(leitura, numeroCliente, megasPedido) ?: return

        // Evita reenviar exatamente a mesma resposta em menos de 1.5s
        if (resposta == ultimaRespostaEnviada && agora - horaUltimaResposta < 1500L) {
            return
        }

        if (preencherEEnviar(raiz, resposta, 0)) {
            ultimaRespostaEnviada = resposta
            horaUltimaResposta = agora
        }
    }

    private fun escolherRespostaMenu(
        leitura: String,
        numeroCliente: String,
        megas: Int
    ): String? {
        val texto = leitura.lowercase()

        return when {
            // Passo 1: Menu principal -> 8. Serviço de internet
            (texto.contains("serviço de internet") ||
                texto.contains("servico de internet") ||
                texto.contains("internet")) &&
                !texto.contains("transferir megas") -> "8"

            // Passo 2: Menu internet -> 2. Transferir megas
            texto.contains("transferir megas") -> "2"

            // Passo 3: Pedir quantidade / megas / saldo
            texto.contains("megas") ||
                texto.contains("saldo") ||
                texto.contains("quantidade") ||
                texto.contains("valor") ||
                texto.contains("montante") -> {
                if (megas > 0) megas.toString() else null
            }

            // Passo 4: Pedir número do recipiente / destino
            texto.contains("recipiente") ||
                texto.contains("número") ||
                texto.contains("numero") ||
                texto.contains("destino") ||
                texto.contains("contacto") -> {
                numeroCliente.ifBlank { null }
            }

            // Passo 5: Confirmação numérica (quando houver opção 1 para confirmar)
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
