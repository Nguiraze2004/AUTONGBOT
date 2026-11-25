
package com.nguiraze.net

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class UssdAccessibilityService : AccessibilityService() {

    private val preferencias by lazy {
        getSharedPreferences("ussd_debug", MODE_PRIVATE)
    }

    private val padraoSucesso = Regex(
        """Transferiste\s+com\s+sucesso\s+(\d+)\s*MB.*?para\s+o\s+numero\s+(258\d{9})""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val textos = mutableListOf<String>()

        event.text?.forEach {
            if (it.toString().isNotBlank()) {
                textos.add(it.toString())
            }
        }

        val raiz = rootInActiveWindow

        if (raiz != null) {
            analisarElementos(raiz, 0, textos)
        }

        val leitura = textos.distinct().joinToString("\n")

        if (leitura.isBlank()) return

        preferencias.edit()
            .putString("ultima_leitura", leitura)
            .putString(
                "ultima_janela",
                event.packageName?.toString() ?: "Desconhecida"
            )
            .apply()

        Log.d("NGUIRAZE_USSD", leitura)

        // PROCURAR CONFIRMAÇÃO DE SUCESSO
        if (!leitura.contains("Transferiste", ignoreCase = true)) {
            return
        }

        val textoLimpo = leitura.replace(
            Regex("\\s+"),
            " "
        ).trim()

        val resultado = padraoSucesso.find(textoLimpo) ?: return

        val megas = resultado.groupValues[1].toIntOrNull() ?: return
        val numero = resultado.groupValues[2]

        if (megas <= 0) return

        // EVITAR CONTAR O MESMO AVISO REPETIDAMENTE
        val assinatura = "$numero-$megas"
        val agora = System.currentTimeMillis()

        val assinaturaAnterior =
            preferencias.getString("ultima_assinatura", "")

        val horaAnterior =
            preferencias.getLong("hora_ultima_assinatura", 0L)

        if (
            assinatura == assinaturaAnterior &&
            agora - horaAnterior < 10000
        ) {
            return
        }

        // GUARDAR CONFIRMAÇÃO REAL
        preferencias.edit()
            .putString("ultima_assinatura", assinatura)
            .putLong("hora_ultima_assinatura", agora)
            .putString("ussd_sucesso_numero", numero)
            .putInt("ussd_sucesso_megas", megas)
            .putLong("ussd_sucesso_hora", agora)
            .putBoolean("ussd_sucesso_pendente", true)
            .apply()

        Log.d(
            "NGUIRAZE_USSD",
            "SUCESSO: $megas MB enviados para $numero"
        )
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
