package net.lifenet.core.contact

import android.content.Context
import android.provider.ContactsContract
import android.telephony.SmsManager

class ContactEngine(private val context: Context) {

    fun getLifenetContacts(): List<String> {
        val result = mutableListOf<String>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val number = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                if (isLifenetUser(number)) result.add(number)
            }
        }
        return result
    }

    fun sendSmsInvite(number: String) {
        try {
            SmsManager.getDefault().sendTextMessage(
                number,
                null,
                "LIFENET'e Katıl: lifenet://join",
                null,
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isLifenetUser(number: String): Boolean = false
}
