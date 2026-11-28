package com.nguiraze.net

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsOperadoraReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val mensagens = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        for (mensagem in mensagens) {

            val remetente = mensagem.originatingAddress ?: ""
            val texto = mensagem.messageBody ?: ""
            val hora = mensagem.timestampMillis

            Log.i(
                "NGUIRAZE_SMS",
                "Remetente: $remetente | Mensagem: $texto | Hora: $hora"
            )
        }
    }
}
