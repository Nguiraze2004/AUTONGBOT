
package com.nguiraze.net

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class UssdAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (event == null) return

        val textos = event.text
            .map { it.toString() }
            .filter { it.isNotBlank() }

        if (textos.isNotEmpty()) {
            Log.d(
                "NGUIRAZE_USSD",
                "Janela: ${event.packageName} | Texto: $textos"
            )
        }

        // Consultar os elementos visíveis da janela
        val raiz = rootInActiveWindow

        if (raiz != null) {
            Log.d(
                "NGUIRAZE_USSD",
                "A analisar janela: ${raiz.packageName}"
            )

            analisarElementos(raiz, 0)
        }
    }

    private fun analisarElementos(
        node: AccessibilityNodeInfo,
        nivel: Int
    ) {
        if (nivel > 8) return

        val texto = node.text?.toString().orEmpty()
        val descricao = node.contentDescription?.toString().orEmpty()
        val identificador = node.viewIdResourceName ?: ""

        if (
            texto.isNotBlank() ||
            descricao.isNotBlank() ||
            node.isEditable ||
            node.isClickable
        ) {
            Log.d(
                "NGUIRAZE_USSD",
                "Elemento: classe=${node.className} | " +
                "texto=$texto | descrição=$descricao | " +
                "id=$identificador | " +
                "editável=${node.isEditable} | " +
                "clicável=${node.isClickable}"
            )
        }

        for (i in 0 until node.childCount) {
            val filho = node.getChild(i) ?: continue
            analisarElementos(filho, nivel + 1)
        }
    }

    override fun onInterrupt() {
        Log.d(
            "NGUIRAZE_USSD",
            "Serviço interrompido"
        )
    }
}
   
