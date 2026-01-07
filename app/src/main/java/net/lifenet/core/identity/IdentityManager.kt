package net.lifenet.core.identity

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.*
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class IdentityManager(private val context: Context) {

    private val PREFS_NAME = "lifenet_identity_secure"
    private val KEY_NODE_ID = "node_id"
    private val KEY_ROTATION_TIME = "last_rotation_time"
    private val KEY_PUBLIC_KEY = "public_key"
    private val KEY_PRIVATE_KEY = "private_key"

    private val sharedPrefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    init {
        ensureIdentityExists()
    }

    private fun ensureIdentityExists() {
        if (!sharedPrefs.contains(KEY_NODE_ID)) {
            rotateNodeId()
        }
        if (!sharedPrefs.contains(KEY_PUBLIC_KEY)) {
            generateKeyPair()
        }
    }

    /**
     * Generates a new random Node ID for anonymity.
     */
    fun rotateNodeId() {
        val newId = "NODE_" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        sharedPrefs.edit().apply {
            putString(KEY_NODE_ID, newId)
            putLong(KEY_ROTATION_TIME, System.currentTimeMillis())
            apply()
        }
    }

    fun getCurrentNodeId(): String {
        return sharedPrefs.getString(KEY_NODE_ID, "UNKNOWN") ?: "UNKNOWN"
    }

    fun getLastRotationTime(): Long {
        return sharedPrefs.getLong(KEY_ROTATION_TIME, 0L)
    }

    private fun generateKeyPair() {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(256)
        val keyPair = keyPairGenerator.generateKeyPair()
        
        sharedPrefs.edit().apply {
            putString(KEY_PUBLIC_KEY, Base64.encodeToString(keyPair.public.encoded, Base64.DEFAULT))
            putString(KEY_PRIVATE_KEY, Base64.encodeToString(keyPair.private.encoded, Base64.DEFAULT))
            apply()
        }
    }

    fun getPublicKey(): String? {
        return sharedPrefs.getString(KEY_PUBLIC_KEY, null)
    }

    fun getPrivateKey(): String? {
        return sharedPrefs.getString(KEY_PRIVATE_KEY, null)
    }

    /**
     * Signs data using the internal Private Key.
     */
    fun signData(data: String): String {
        val privateKeyBytes = Base64.decode(getPrivateKey(), Base64.DEFAULT)
        val keyFactory = KeyFactory.getInstance("EC")
        val privateKey = keyFactory.generatePrivate(java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes))

        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data.toByteArray())
        return Base64.encodeToString(signature.sign(), Base64.DEFAULT)
    }

    /**
     * Verifies data signature using a provided Public Key.
     */
    fun verifySignature(data: String, signatureStr: String, publicKeyStr: String): Boolean {
        return try {
            val publicKeyBytes = Base64.decode(publicKeyStr, Base64.DEFAULT)
            val keyFactory = KeyFactory.getInstance("EC")
            val publicKey = keyFactory.generatePublic(java.security.spec.X509EncodedKeySpec(publicKeyBytes))

            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initVerify(publicKey)
            signature.update(data.toByteArray())
            signature.verify(Base64.decode(signatureStr, Base64.DEFAULT))
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Encrypts a message content using a simple AES-GCM approach for mesh.
     * In a full implementation, this would use a derived shared secret from X3DH.
     * For now, this provides basic symmetric encryption placeholder for P2P.
     */
    fun encryptPayload(data: String, secret: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = SecretKeySpec(sha256(secret), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data.toByteArray())
        
        // combine IV + encrypted
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decryptPayload(data: String, secret: String): String {
        val combined = Base64.decode(data, Base64.DEFAULT)
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = SecretKeySpec(sha256(secret), "AES")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        return String(cipher.doFinal(encrypted))
    }

    private fun sha256(input: String): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray())
    }

    /**
     * Returns a persistent passphrase for database encryption.
     */
    fun getDatabasePassphrase(): String {
        val key = "db_passphrase"
        if (!sharedPrefs.contains(key)) {
            val passphrase = UUID.randomUUID().toString()
            sharedPrefs.edit().putString(key, passphrase).apply()
        }
        return sharedPrefs.getString(key, null)!!
    }
}
