
package com.nguiraze.net

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
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

    private val clipboard by lazy {
        getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
    }

    private val handler = Handler(Looper.getMainLooper())

    private val tempoLimiteEtapaMs = 8_000L
    private val atrasoCliqueEnviarMs = 400L

    // 1 = Serviços de Internet
    // 2 = Transferir megas
    // 3 = Quantidade de MB
    // 4 = Número do recipiente
    // 5 = Aguardar resultado final

    private var passoAtual = 1
    private var aguardandoProximaJanelaDesdeMs = 0L
    private var ultimoEnvioMs = 0L
    private var ultimaLeituraEnviada = ""
    private var ultimoCliqueSimMs = 0L
    private var envioEmAndamento = false
    private var resultadoJaTratado = false

    private val timeoutRunnable = Runnable {
        if (!pedidoPrefs.getBoolean("ativo", false)) return@Runnable

        val motivo = if (passoAtual >= 5) {
            "Tempo esgotado sem resultado final da operadora"
        } else {
            "Tempo esgotado aguardando passo $passoAtual"
        }

        preferencias.edit()
            .putString("ultimo_erro_ussd", motivo)
            .putString(
                "ultimo_estado_ussd",
                if (passoAtual >= 5) {
                    "INCERTO"
                } else {
                    "ENCERRADO_TIMEOUT"
                }
            )
            .apply()

        Log.d("NGUIRAZE_USSD", motivo)

        encerrarExecucao(
            if (passoAtual >= 5) "RESULTADO_INCERTO" else "TIMEOUT_8S"
        )
    }

    private val padraoSucesso = Regex(
        """transferiste\s+com\s+sucesso\s+(\d+)\s*mb.*?para\s+o\s+numero\s+(258\d{9})""",
        setOf(
            RegexOption.IGNORE_CASE,
            RegexOption.DOT_MATCHES_ALL
        )
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pacote = event.packageName?.toString() ?: "Desconhecida"

        if (pacote == "com.nguiraze.net") return

        val raiz = rootInActiveWindow ?: return

        val textos = mutableListOf<String>()

        event.text?.forEach {
            if (it.toString().isNotBlank()) {
                textos.add(it.toString())
            }
        }

        analisarElementos(raiz, 0, textos)

        val leitura = textos.distinct().joinToString("\n").trim()

        if (leitura.isBlank()) return

        val agora = System.currentTimeMillis()

        val pedidoAtivo = pedidoPrefs.getBoolean("ativo", false)
        val timestampPedido = pedidoPrefs.getLong("timestamp", 0L)

        val sessaoValida = pedidoAtivo &&
            (agora - timestampPedido in 0..90_000L)

        preferencias.edit()
            .putString("ultima_leitura", leitura)
            .putString("ultima_janela", pacote)
            .putBoolean("sessao_ativa", sessaoValida)
            .apply()

        Log.d(
            "NGUIRAZE_USSD",
            "[$pacote | passo=$passoAtual | ativo=$sessaoValida]\n$leitura"
        )

        if (!sessaoValida) {
            cancelarTimers()

            if (pedidoAtivo) {
                encerrarExecucao("SESSAO_EXPIRADA")
            }

            return
        }

        if (aguardandoProximaJanelaDesdeMs == 0L) {
            armarTimer8s(agora)
        }

        // Escolha do SIM
        if (ehDialogoEscolhaSim(leitura)) {

            if (agora - ultimoCliqueSimMs < 1200L) return

            val simDesejado = pedidoPrefs.getInt("sim", 1).coerceIn(1, 2)

            if (selecionarCartaoSim(raiz, simDesejado)) {
                ultimoCliqueSimMs = agora
                passoAtual = 1
                ultimaLeituraEnviada = ""

                armarTimer8s(agora)

                Log.d(
                    "NGUIRAZE_USSD",
                    "SIM $simDesejado selecionado"
                )
            }

            return
        }

        if (!ehJanelaSistemaOuTelefone(pacote, leitura)) {
            return
        }

        val normalizada = normalizarTexto(leitura)

        val textoLimpo = normalizada
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ")

        val temOk = temBotaoOk(raiz, 0)

        val temCampoEditavel =
            encontrarCampoEditavel(raiz, 0) != null

        val mensagemSucesso =
            textoLimpo.contains("transferiste com sucesso")

        val mensagemFalha =
            textoLimpo.contains("transacao sem sucesso")

                // Evita tratar uma pergunta de confirmação como resultado final.
        val pareceConfirmacao =
            textoLimpo.contains("deseja confirmar") ||
            textoLimpo.contains("pretende confirmar") ||
            textoLimpo.contains("confirma a transferencia") ||
            textoLimpo.contains("confirmar transferencia") ||
            textoLimpo.contains("confirme a transferencia") ||
            textoLimpo.contains("para confirmar")

        // Identifica o resultado final.
        val ehTelaFinal =
            mensagemSucesso ||
            mensagemFalha ||
            (
                passoAtual >= 5 &&
                temOk &&
                !temCampoEditavel &&
                !pareceConfirmacao
            )

        // Resultado final:
        // 1. Mensagem explícita de sucesso.
        // 2. Mensagem explícita de falha.
        // 3. Janela final com OK, sem campo de texto,
        //    depois do envio do número e sem indicação de confirmação.   

        if (ehTelaFinal && !resultadoJaTratado) {

            resultadoJaTratado = true
            cancelarTimers()

            // Clica OK em qualquer resultado final.
            val clicouOk = clicarBotaoOk(raiz, 0)

            if (mensagemSucesso) {

                val match = padraoSucesso.find(textoLimpo)

                val megasOperadora =
                    match?.groupValues?.getOrNull(1)?.toIntOrNull()

                val numeroOperadora =
                    match?.groupValues?.getOrNull(2) ?: ""

                val megasPedido =
                    pedidoPrefs.getInt("megas", 0)

                val numeroPedido =
                    (pedidoPrefs.getString("numero", "") ?: "")
                        .trim()

                // Só aceita sucesso se os dados da operadora
                // forem encontrados e corresponderem ao pedido.
                val dadosValidos =
                    megasOperadora != null &&
                    numeroOperadora.isNotBlank() &&
                    megasOperadora == megasPedido &&
                    numeroOperadora == numeroPedido

                if (dadosValidos) {

                    val assinatura = "$numeroOperadora-$megasOperadora"

                    val assinaturaAnterior =
                        preferencias.getString("ultima_assinatura", "")

                    val horaAnterior =
                        preferencias.getLong("hora_ultima_assinatura", 0L)

                    // Evita duplicar o registo da mesma transferência.
                    if (
                        assinatura != assinaturaAnterior ||
                        agora - horaAnterior >= 10_000L
                    ) {
                        preferencias.edit()
                            .putString("ultima_assinatura", assinatura)
                            .putLong("hora_ultima_assinatura", agora)
                            .putString(
                                "ultimo_resultado",
                                "CONCLUIDO: $megasOperadora MB para $numeroOperadora"
                            )
                            .putString("ultimo_estado_ussd", "CONCLUIDO")
                            .putString(
                                "ultimo_erro_ussd",
                                if (clicouOk) {
                                    "Sucesso confirmado e OK acionado"
                                } else {
                                    "Sucesso confirmado, mas OK não foi acionado"
                                }
                            )
                            .apply()
                    }

                    encerrarExecucao("CONCLUIDO")

                } else {

                    preferencias.edit()
                        .putString(
                            "ultimo_resultado",
                            "RESULTADO_INCERTO: dados da operadora não correspondem ao pedido"
                        )
                        .putString("ultimo_estado_ussd", "INCERTO")
                        .putString(
                            "ultimo_erro_ussd",
                            "Mensagem de sucesso encontrada, mas MB ou número não foram validados"
                        )
                        .apply()

                    encerrarExecucao("SUCESSO_NAO_VALIDADO")
                }

            } else {

                // Não apareceu confirmação de sucesso.
                // Clica OK, mas não conta os MB.
                preferencias.edit()
                    .putString(
                        "ultimo_resultado",
                        "NAO_CONCLUIDO: $textoLimpo"
                    )
                    .putString("ultimo_estado_ussd", "SEM_CONFIRMACAO_SUCESSO")
                    .putString(
                        "ultimo_erro_ussd",
                        if (clicouOk) {
                            "Resultado sem sucesso: OK acionado"
                        } else {
                            "Resultado sem sucesso: OK não encontrado"
                        }
                    )
                    .apply()

                encerrarExecucao("SEM_CONFIRMACAO_SUCESSO")
            }

            return
        }

        if (envioEmAndamento) return

        if (
            leitura == ultimaLeituraEnviada &&
            (agora - ultimoEnvioMs) < 2000L
        ) {
            return
        }

        if ((agora - ultimoEnvioMs) < 700L) return

        val numeroCliente =
            (pedidoPrefs.getString("numero", "") ?: "").trim()

        val megasPedido =
            pedidoPrefs.getInt("megas", 0)

        val resposta = resolverPassoAtual(
            normalizada,
            numeroCliente,
            megasPedido
        ) ?: return

        preencherEAguardarEnviar(resposta, leitura)
    }

    private fun resolverPassoAtual(
        textoNormalizado: String,
        numeroCliente: String,
        megas: Int
    ): String? {

        return when (passoAtual) {

            1 -> {
                if (
                    textoNormalizado.contains("servicos de internet") ||
                    textoNormalizado.contains("servico de internet")
                ) {
                    "8"
                } else {
                    null
                }
            }

            2 -> {
                if (textoNormalizado.contains("transferir megas")) {
                    "2"
                } else {
                    null
                }
            }

            3 -> {
                if (
                    textoNormalizado.contains("saldo de dados") &&
                    megas > 0
                ) {
                    megas.toString()
                } else {
                    null
                }
            }

            4 -> {
                if (
                    textoNormalizado.contains("digita o numero do recipiente") &&
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

    private fun preencherCampoTexto(
        campo: AccessibilityNodeInfo,
        texto: String
    ): Boolean {

        campo.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        campo.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                texto
            )
        }

        if (
            campo.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                args
            )
        ) {
            return true
        }

        val cm = clipboard ?: return false

        cm.setPrimaryClip(
            ClipData.newPlainText("ussd_input", texto)
        )

        return campo.performAction(
            AccessibilityNodeInfo.ACTION_PASTE
        )
    }

    private fun preencherEAguardarEnviar(
        texto: String,
        leituraAtual: String
    ) {

        val raizAtual = rootInActiveWindow ?: return

        val campoEditavel =
            encontrarCampoEditavel(raizAtual, 0)

        if (campoEditavel == null) {
            preferencias.edit()
                .putString(
                    "ultimo_erro_ussd",
                    "Campo editável não encontrado"
                )
                .apply()
            return
        }

        envioEmAndamento = true

        val preencheu =
            preencherCampoTexto(campoEditavel, texto)

        if (!preencheu) {
            envioEmAndamento = false

            preferencias.edit()
                .putString(
                    "ultimo_erro_ussd",
                    "Falha ao preencher campo: $texto"
                )
                .apply()

            return
        }

        handler.postDelayed({

            try {

                if (!pedidoPrefs.getBoolean("ativo", false)) {
                    envioEmAndamento = false
                    return@postDelayed
                }

                val janelaParaEnviar =
                    rootInActiveWindow ?: raizAtual

                val clicouEnviar =
                    clicarBotaoEnvio(janelaParaEnviar, 0)

                if (clicouEnviar) {

                    val horaEnvio = System.currentTimeMillis()

                    ultimoEnvioMs = horaEnvio
                    ultimaLeituraEnviada = leituraAtual

                    passoAtual = when (passoAtual) {
                        1 -> 2
                        2 -> 3
                        3 -> 4
                        4 -> 5
                        else -> 5
                    }

                    preferencias.edit()
                        .putString(
                            "ultimo_erro_ussd",
                            "Enviado: $texto"
                        )
                        .apply()

                    armarTimer8s(horaEnvio)

                    Log.d(
                        "NGUIRAZE_USSD",
                        "Resposta enviada: $texto | próximo passo=$passoAtual"
                    )

                } else {

                    preferencias.edit()
                        .putString(
                            "ultimo_erro_ussd",
                            "Botão Enviar não clicado: $texto"
                        )
                        .apply()
                }

            } finally {
                envioEmAndamento = false
            }

        }, atrasoCliqueEnviarMs)
    }

    private fun armarTimer8s(agora: Long) {
        cancelarTimers()

        aguardandoProximaJanelaDesdeMs = agora

        handler.postDelayed(
            timeoutRunnable,
            tempoLimiteEtapaMs
        )
    }

    private fun cancelarTimers() {
        handler.removeCallbacks(timeoutRunnable)
        aguardandoProximaJanelaDesdeMs = 0L
    }

    private fun encerrarExecucao(motivo: String) {

        cancelarTimers()

        envioEmAndamento = false
        resultadoJaTratado = false

        passoAtual = 1
        ultimaLeituraEnviada = ""
        ultimoEnvioMs = 0L

        pedidoPrefs.edit()
            .putBoolean("ativo", false)
            .putString("ultimo_fim_motivo", motivo)
            .apply()

        Log.d(
            "NGUIRAZE_USSD",
            "Execução encerrada: $motivo"
        )
    }

    private fun normalizarTexto(entrada: String): String {

        val semAcento = Normalizer.normalize(
            entrada.lowercase(),
            Normalizer.Form.NFD
        ).replace(
            Regex("\\p{InCombiningDiacriticalMarks}+"),
            ""
        )

        return semAcento
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()
    }

    private fun ehJanelaSistemaOuTelefone(
        pacote: String,
        leitura: String
    ): Boolean {

        val p = pacote.lowercase()
        val l = normalizarTexto(leitura)

        return p.contains("phone") ||
            p.contains("dialer") ||
            p.contains("incallui") ||
            p.contains("telecom") ||
            p.contains("stk") ||
            p.contains("android") ||
            l.contains("enviar") ||
            l.contains("cancelar") ||
            l.contains("servicos de internet") ||
            l.contains("servico de internet") ||
            l.contains("transferir megas") ||
            l.contains("saldo de dados") ||
            l.contains("digita o numero do recipiente") ||
            l.contains("fazer chamadas com") ||
            l.contains("transferiste") ||
            l.contains("transacao sem sucesso")
    }

    private fun ehDialogoEscolhaSim(leitura: String): Boolean {

        val t = normalizarTexto(leitura)

        return t.contains("fazer chamadas com") ||
            t.contains("escolher sim") ||
            t.contains("selecionar sim") ||
            (t.contains("sim 1") && t.contains("sim 2"))
    }

    private fun selecionarCartaoSim(
        root: AccessibilityNodeInfo,
        simDesejado: Int
    ): Boolean {

        val rotuloAlvo =
            if (simDesejado == 2) "SIM 2" else "SIM 1"

        if (clicarPorTexto(root, rotuloAlvo, 0)) {
            return true
        }

        val clicaveis =
            mutableListOf<AccessibilityNodeInfo>()

        coletarClicaveis(root, 0, clicaveis)

        for (item in clicaveis) {

            val textoItem =
                coletarTextoDoNo(item).trim()

            if (
                simDesejado == 1 &&
                (
                    textoItem.startsWith("1") ||
                    textoItem.contains("SIM 1", true)
                )
            ) {
                if (
                    item.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                    )
                ) {
                    return true
                }
            }

            if (
                simDesejado == 2 &&
                (
                    textoItem.startsWith("2") ||
                    textoItem.contains("SIM 2", true)
                )
            ) {
                if (
                    item.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                    )
                ) {
                    return true
                }
            }
        }

        // Não clicar num elemento aleatório se não identificar o SIM.
        return false
    }

    private fun coletarTextoDoNo(
        node: AccessibilityNodeInfo
    ): String {

        val sb = StringBuilder()

        node.text?.let {
            sb.append(it).append(" ")
        }

        node.contentDescription?.let {
            sb.append(it).append(" ")
        }

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

        val textoNo =
            (node.text?.toString() ?: "") +
                " " +
                (node.contentDescription?.toString() ?: "")

        if (textoNo.contains(alvo, ignoreCase = true)) {
            if (clicarNoOuPai(node)) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val filho = node.getChild(i) ?: continue

            if (clicarPorTexto(filho, alvo, nivel + 1)) {
                return true
            }
        }

        return false
    }

    private fun clicarNoOuPai(
        node: AccessibilityNodeInfo
    ): Boolean {

        var atual: AccessibilityNodeInfo? = node
        var passos = 0

        while (atual != null && passos < 7) {

            if (atual.isClickable && atual.isEnabled) {

                if (
                    atual.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                    )
                ) {
                    return true
                }
            }

            atual = atual.parent
            passos++
        }

        return false
    }

    private fun tocarCoordenadas(
        x: Float,
        y: Float
    ): Boolean {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }

        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0L,
                    60L
                )
            )
            .build()

        return dispatchGesture(gesture, null, null)
    }

    private fun tocarCentroDoNo(
        node: AccessibilityNodeInfo
    ): Boolean {

        val rect = Rect()

        node.getBoundsInScreen(rect)

        if (rect.width() <= 0 || rect.height() <= 0) {
            return false
        }

        return tocarCoordenadas(
            rect.exactCenterX(),
            rect.exactCenterY()
        )
    }

    private fun encontrarCampoEditavel(
        node: AccessibilityNodeInfo,
        nivel: Int
    ): AccessibilityNodeInfo? {

        if (nivel > 10) return null

        if (node.isEditable && node.isEnabled) {
            return node
        }

        for (i in 0 until node.childCount) {

            val filho = node.getChild(i) ?: continue

            val encontrado =
                encontrarCampoEditavel(filho, nivel + 1)

            if (encontrado != null) {
                return encontrado
            }
        }

        return null
    }

    private fun encontrarNoPorRotulo(
        node: AccessibilityNodeInfo,
        alvos: Set<String>,
        nivel: Int
    ): AccessibilityNodeInfo? {

        if (nivel > 8) return null

        val texto = (
            (node.text?.toString() ?: "") +
                " " +
                (node.contentDescription?.toString() ?: "")
            ).trim()

        if (alvos.any { texto.equals(it, ignoreCase = true) }) {
            return node
        }

        for (i in 0 until node.childCount) {

            val filho = node.getChild(i) ?: continue

            val achado =
                encontrarNoPorRotulo(filho, alvos, nivel + 1)

            if (achado != null) {
                return achado
            }
        }

        return null
    }

    private fun temBotaoOk(
        node: AccessibilityNodeInfo,
        nivel: Int
    ): Boolean {

        return encontrarNoPorRotulo(
            node,
            setOf("OK"),
            nivel
        ) != null
    }

    private fun clicarBotaoEnvio(
        node: AccessibilityNodeInfo,
        nivel: Int
    ): Boolean {

        val alvo = encontrarNoPorRotulo(
            node,
            setOf("Enviar", "OK", "Send"),
            nivel
        ) ?: return false

        // Primeiro tenta o clique de acessibilidade.
        if (clicarNoOuPai(alvo)) {
            return true
        }

        // Só usa coordenadas se o primeiro método falhar.
        return tocarCentroDoNo(alvo)
    }

    private fun clicarBotaoOk(
        node: AccessibilityNodeInfo,
        nivel: Int
    ): Boolean {

        val alvo = encontrarNoPorRotulo(
            node,
            setOf("OK"),
            nivel
        ) ?: return false

        // Primeiro tenta o clique de acessibilidade.
        if (clicarNoOuPai(alvo)) {
            Log.d("NGUIRAZE_USSD", "OK clicado por acessibilidade")
            return true
        }

        // Se falhar, tenta tocar no centro do botão.
        val tocou = tocarCentroDoNo(alvo)

        Log.d(
            "NGUIRAZE_USSD",
            "Tentativa de toque em OK: $tocou"
        )

        return tocou
    }

    private fun analisarElementos(
        node: AccessibilityNodeInfo,
        nivel: Int,
        textos: MutableList<String>
    ) {

        if (nivel > 8) return

        node.text?.toString()?.let {
            if (it.isNotBlank()) {
                textos.add(it)
            }
        }

        node.contentDescription?.toString()?.let {
            if (it.isNotBlank()) {
                textos.add(it)
            }
        }

        for (i in 0 until node.childCount) {
            val filho = node.getChild(i) ?: continue
            analisarElementos(filho, nivel + 1, textos)
        }
    }

    override fun onInterrupt() {
        cancelarTimers()
        handler.removeCallbacksAndMessages(null)
        envioEmAndamento = false

        Log.d(
            "NGUIRAZE_USSD",
            "Serviço interrompido"
        )
    }

    override fun onDestroy() {
        cancelarTimers()
        handler.removeCallbacksAndMessages(null)
        envioEmAndamento = false

        super.onDestroy()
    }
}
