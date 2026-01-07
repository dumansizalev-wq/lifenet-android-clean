package net.lifenet.core.utils

import android.content.Context
import android.provider.ContactsContract
import net.lifenet.core.data.Contact
import net.lifenet.core.data.LifenetDatabase

class ContactMapper(private val context: Context) {

    private val db = LifenetDatabase.getInstance(context)
    private val contactDao = db.contactDao()

    /**
     * Resolves a Node ID or Phone Number to a Contact Name from the Android Phonebook.
     */
    fun resolveNameFromPhone(phoneNumber: String): String? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?"
        val selectionArgs = arrayOf(phoneNumber)

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }

    /**
     * Syncs a mesh contact name if they have a phone number associated.
     */
    suspend fun syncContactName(meshId: String, phoneNumber: String?) {
        if (phoneNumber == null) return
        
        val resolvedName = resolveNameFromPhone(phoneNumber)
        if (resolvedName != null) {
            val existing = contactDao.getContact(meshId)
            if (existing != null) {
                contactDao.insertContact(existing.copy(name = resolvedName, isSyncedFromPhone = true))
            } else {
                contactDao.insertContact(Contact(
                    id = meshId, 
                    name = resolvedName, 
                    phoneNumber = phoneNumber, 
                    isSyncedFromPhone = true
                ))
            }
        }
    }
}
