
package com.nguiraze.net

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
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
    }

    override fun onInterrupt() {
        Log.d("NGUIRAZE_USSD", "Serviço interrompido")
    }
}
