
package com.nguiraze.net

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class UssdAccessibilityService : AccessibilityService() {

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

        if (leitura.isNotBlank()) {
            getSharedPreferences(
                "ussd_debug",
                MODE_PRIVATE
            ).edit()
                .putString("ultima_leitura", leitura)
                .putString(
                    "ultima_janela",
                    event.packageName?.toString() ?: "Desconhecida"
                )
                .apply()

            Log.d("NGUIRAZE_USSD", leitura)
        }
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
