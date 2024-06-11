import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


typealias Iv = ByteArray
typealias CipheredPayload = ByteArray

enum class AESType {
    GCM, CBC
}

interface AES {
    fun encrypt(data: ByteArray): Pair<CipheredPayload, Iv>
    fun decrypt(data: CipheredPayload, iv: Iv): ByteArray

    companion object {
        fun create(type: AESType, key: ByteArray): AES {
            val secretKey = SecretKeySpec(key, "AES")
            return when (type) {
                AESType.GCM -> AESGCM(secretKey)
                AESType.CBC -> AESCBC(secretKey)
            }
        }
    }
}

class AESGCM(private val key: Key) : AES {
    private val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
    private val gcmTagLength = 128

    override fun encrypt(data: ByteArray): Pair<CipheredPayload, Iv> {
        val iv = generateIV()
        val parameterSpec = GCMParameterSpec(gcmTagLength, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)
        return Pair(cipher.doFinal(data), iv)
    }

    override fun decrypt(data: CipheredPayload, iv: Iv): ByteArray {
        val parameterSpec = GCMParameterSpec(gcmTagLength, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)
        return cipher.doFinal(data)
    }

    private fun generateIV(): ByteArray {
        val iv = ByteArray(12)
        java.security.SecureRandom().nextBytes(iv)
        return iv
    }
}

class AESCBC(private val key: Key) : AES {
    private val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

    override fun encrypt(data: ByteArray): Pair<CipheredPayload, Iv> {
        val iv = generateIV()
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        return Pair(cipher.doFinal(data), iv)
    }

    override fun decrypt(data: CipheredPayload, iv: Iv): ByteArray {
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        return cipher.doFinal(data)
    }

    private fun generateIV(): ByteArray {
        val iv = ByteArray(16)
        java.security.SecureRandom().nextBytes(iv)
        return iv
    }
}
