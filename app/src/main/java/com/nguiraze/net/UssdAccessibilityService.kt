package com.nguiraze.net

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.text.Normalizer

class UssdAccessibilityService : AccessibilityService() {

    private val preferencias by lazy {
        getSharedPreferences("ussd_debug", MODE_PRIVATE)
    }

    private val pedidoPrefs by lazy {
        getSharedPreferences("ussd_pedido_ativo", MODE_PRIVATE)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val tempoLimiteEtapaMs = 8_000L

    private var passoAtual = 1 // 1=internet, 2=transferir megas, 3=saldo de dados, 4=recipiente, 5=aguardar resultado
    private var aguardandoProximaJanelaDesdeMs = 0L
    private var ultimoEnvioMs = 0L
    private var ultimaLeituraEnviada = ""
    private var ultimoCliqueSimMs = 0L

    private val timeoutRunnable = Runnable {
        if (!pedidoPrefs.getBoolean("ativo", false)) return@Runnable

        val motivo = if (passoAtual == 5) {
            "8s esgotados sem 'transferiste com sucesso' (nao concluido)"
        } else {
            "8s esgotados aguardando janela do passo $passoAtual"
        }

        preferencias.edit()
            .putString("ultimo_erro_ussd", motivo)
            .putString("ultimo_estado_ussd", if (passoAtual == 5) "INCERTO_OU_FALHA" else "ENCERRADO_TIMEOUT")
            .apply()

        Log.d("NGUIRAZE_USSD", "Encerrado automaticamente: $motivo")
        encerrarExecucao("TIMEOUT_8S")
    }

    private val padraoSucesso = Regex(
        """transferiste\s+com\s+sucesso\s+(\d+)\s*mb.*?para\s+o\s+numero\s+(258\d{9})""",
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

        Log.d("NGUIRAZE_USSD", "[$pacote | passo=$passoAtual | ativo=$sessaoValida]\n$leitura")

        if (!sessaoValida) {
            cancelarTimer8s()
            if (pedidoAtivo) {
                encerrarExecucao("SESSAO_EXPIRADA")
            }
            return
        }

        // Garante que o cronômetro de 8s está armado para a etapa atual
        if (aguardandoProximaJanelaDesdeMs == 0L) {
            armarTimer8s(agora)
        }

        // Escolha de SIM ("Fazer chamadas com")
        if (ehDialogoEscolhaSim(leitura)) {
            if (agora - ultimoCliqueSimMs < 1200L) return
            val simDesejado = pedidoPrefs.getInt("sim", 1).coerceIn(1, 2)
            if (selecionarCartaoSim(raiz, simDesejado)) {
                ultimoCliqueSimMs = agora
                passoAtual = 1
                ultimaLeituraEnviada = ""
                armarTimer8s(agora)
            }
            return
        }

        if (!ehJanelaSistemaOuTelefone(pacote, leitura)) {
            return
        }

        val normalizada = normalizarTexto(leitura)

        // Etapa 5: Aguarda mensagem final e verifica "transferiste com sucesso"
        if (passoAtual == 5 || normalizada.contains("transferiste")) {
            cancelarTimer8s()
            val textoLimpo = normalizada.replace(Regex("\\s+"), " ").trim()

            if (textoLimpo.contains("transferiste com sucesso")) {
                val match = padraoSucesso.find(textoLimpo)
                val megas = match?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: pedidoPrefs.getInt("megas", 0)
                val numero = match?.groupValues?.getOrNull(2)
                    ?: (pedidoPrefs.getString("numero", "") ?: "")

                val assinatura = "$numero-$megas"
                val assinaturaAnterior = preferencias.getString("ultima_assinatura", "")
                val horaAnterior = preferencias.getLong("hora_ultima_assinatura", 0L)

                if (assinatura != assinaturaAnterior || agora - horaAnterior >= 10_000L) {
                    preferencias.edit()
                        .putString("ultima_assinatura", assinatura)
                        .putLong("hora_ultima_assinatura", agora)
                        .putString("ultimo_resultado", "CONCLUIDO: $megas MB para $numero")
                        .putString("ultimo_estado_ussd", "CONCLUIDO")
                        .apply()
                }
                encerrarExecucao("CONCLUIDO")
            } else {
                // Não contém "transferiste com sucesso" -> marca como não concluído / outro estado
                preferencias.edit()
                    .putString("ultimo_resultado", "NAO_CONCLUIDO: $textoLimpo")
                    .putString("ultimo_estado_ussd", "SEM_CONFIRMACAO_SUCESSO")
                    .apply()
                encerrarExecucao("SEM_CONFIRMACAO_SUCESSO")
            }
            return
        }

        // Evita reenviar na mesma tela antes de mudar ou antes do intervalo mínimo
        if (leitura == ultimaLeituraEnviada && (agora - ultimoEnvioMs) < 2000L) {
            return
        }
        if ((agora - ultimoEnvioMs) < 700L) {
            return
        }

        val numeroCliente = (pedidoPrefs.getString("numero", "") ?: "").trim()
        val megasPedido = pedidoPrefs.getInt("megas", 0)

        val resposta = resolverPassoAtual(normalizada, numeroCliente, megasPedido) ?: return

        if (preencherEEnviar(raiz, resposta, 0)) {
            ultimoEnvioMs = agora
            ultimaLeituraEnviada = leitura

            passoAtual = when (passoAtual) {
                1 -> 2
                2 -> 3
                3 -> 4
                4 -> 5
                else -> 5
            }

            // Reinicia os 8 segundos à espera da próxima janela
            armarTimer8s(agora)
        }
    }

    private fun resolverPassoAtual(
        textoNormalizado: String,
        numeroCliente: String,
        megas: Int
    ): String? {
        return when (passoAtual) {
            1 -> {
                if (textoNormalizado.contains("servico de internet")) "8" else null
            }
            2 -> {
                if (textoNormalizado.contains("transferir megas")) "2" else null
            }
            3 -> {
                if (textoNormalizado.contains("saldo de dados") && megas > 0) {
                    megas.toString()
                } else {
                    null
                }
            }
            4 -> {
                if (textoNormalizado.contains("digita o numero do recipiente") &&
                    numeroCliente.isNotBlank()
                ) {
                    numeroCliente
                } else {
                    null
                }
            }
            else -> null
        }
    }

    private fun armarTimer8s(agora: Long) {
        cancelarTimer8s()
        aguardandoProximaJanelaDesdeMs = agora
        handler.postDelayed(timeoutRunnable, tempoLimiteEtapaMs)
    }

    private fun cancelarTimer8s() {
        handler.removeCallbacks(timeoutRunnable)
        aguardandoProximaJanelaDesdeMs = 0L
    }

    private fun encerrarExecucao(motivo: String) {
        cancelarTimer8s()
        passoAtual = 1
        ultimaLeituraEnviada = ""
        ultimoEnvioMs = 0L
        pedidoPrefs.edit()
            .putBoolean("ativo", false)
            .putString("ultimo_fim_motivo", motivo)
            .apply()
    }

    private fun normalizarTexto(entrada: String): String {
        val semAcento = Normalizer.normalize(entrada.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return semAcento.replace(Regex("\\s+"), " ").trim()
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
        val t = normalizarTexto(leitura)
        return t.contains("fazer chamadas com") ||
            t.contains("escolher sim") ||
            t.contains("selecionar sim") ||
            (t.contains("sim 1") && t.contains("sim 2"))
    }

    private fun selecionarCartaoSim(root: AccessibilityNodeInfo, simDesejado: Int): Boolean {
        val rotuloAlvo = if (simDesejado == 2) "SIM 2" else "SIM 1"
        if (clicarPorTexto(root, rotuloAlvo, 0)) return true

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
        cancelarTimer8s()
        Log.d("NGUIRAZE_USSD", "Serviço interrompido")
    }

    override fun onDestroy() {
        cancelarTimer8s()
        super.onDestroy()
    }
}
