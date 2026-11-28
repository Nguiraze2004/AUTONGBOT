
package com.nguiraze.net

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class SmsOperadoraReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val mensagens =
            Telephony.Sms.Intents.getMessagesFromIntent(intent)

        val prefs = context.getSharedPreferences(
            "sms_recebidos",
            Context.MODE_PRIVATE
        )

        val historico = try {
            JSONArray(prefs.getString("historico_sms", "[]"))
        } catch (e: Exception) {
            JSONArray()
        }

        for (mensagem in mensagens) {

            val remetente = mensagem.originatingAddress ?: ""
            val texto = mensagem.messageBody ?: ""
            val hora = mensagem.timestampMillis

            val novoSms = JSONObject().apply {
                put("remetente", remetente)
                put("mensagem", texto)
                put("hora", hora)
            }

            historico.put(novoSms)

            Log.i(
                "NGUIRAZE_SMS",
                "SMS guardado: $remetente | $texto"
            )
        }

        // Manter apenas os últimos 200 registos
        val limite = JSONArray()

        val inicio = (historico.length() - 200).coerceAtLeast(0)

        for (i in inicio until historico.length()) {
            limite.put(historico.getJSONObject(i))
        }

        prefs.edit()
            .putString("historico_sms", limite.toString())
            .apply()
    }
}
