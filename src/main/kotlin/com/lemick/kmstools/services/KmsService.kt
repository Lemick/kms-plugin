package com.lemick.kmstools.services

import ai.grazie.utils.mpp.Base64
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.lemick.kmstools.model.KeyWithAliases
import net.minidev.json.JSONObject
import net.minidev.json.parser.JSONParser
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.*
import java.net.URI
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


@Service(Service.Level.APP)
class KmsService {

    private val settingsService = service<SettingsService>()
    private val parser: JSONParser = JSONParser()

    fun encrypt(dataToEncrypt: String, keyId: String): String {
        val encryptRequest = EncryptRequest.builder()
            .keyId(keyId)
            .plaintext(SdkBytes.fromUtf8String(dataToEncrypt))
            .build()

        val response = createClient().encrypt(encryptRequest)
        return Base64.encode(response.ciphertextBlob().asByteArray())
    }

    fun encryptJsonWithDatakey(dataToEncrypt: String, keyId: String): String? {
        val generateDataKeyRequest = GenerateDataKeyRequest.builder()
            .keyId(keyId)
            .keySpec("AES_256")
            .build()

        val datakeyResult = createClient().generateDataKey(generateDataKeyRequest)
        val cipherTextKey = datakeyResult.ciphertextBlob()

        val secretKey = SecretKeySpec(datakeyResult.plaintext().asByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val nonce = cipher.iv
        val cipheredPayload = cipher.doFinal(dataToEncrypt.toByteArray())

        val json = JSONObject()
        json.put("ciphered_payload", Base64.encode(cipheredPayload))
        json.put("ciphered_key", Base64.encode(cipherTextKey.asByteArray()))
        json.put("nonce", Base64.encode(nonce))

        return json.toJSONString()
    }

    fun decryptJsonWithDataKey(json: String): String {
        val encryptedData = parser.parse(json) as JSONObject

        val cipheredPayload = Base64.decode(encryptedData.getAsString("ciphered_payload"))
        val cipheredKey = Base64.decode(encryptedData.getAsString("ciphered_key"))
        val nonce = Base64.decode(encryptedData.getAsString("nonce"))

        val decryptRequest = DecryptRequest.builder().ciphertextBlob(SdkBytes.fromByteArray(cipheredKey)).build()
        val decryptedKey = createClient().decrypt(decryptRequest).plaintext().asByteArray()

        val secretKey = SecretKeySpec(decryptedKey, "AES")
        val gcmSpec = GCMParameterSpec(128, nonce)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedPayload = cipher.doFinal(cipheredPayload)
        return String(decryptedPayload)
    }

    fun decrypt(dataToDecrypt: String): String {
        val decryptRequest = DecryptRequest.builder()
            .ciphertextBlob(SdkBytes.fromByteArray(Base64.decode(dataToDecrypt)))
            .build()

        val response = createClient().decrypt(decryptRequest)
        return response.plaintext().asUtf8String()
    }

    fun listKeysWithAliases(): List<KeyWithAliases> {
        val client = createClient()

        val keysList = client
            .listKeys(ListKeysRequest.builder().limit(100).build())
            .keys()
        val aliasesList = client
            .listAliases(ListAliasesRequest.builder().limit(100).build())
            .aliases()

        return keysList.map { keyEntry ->
            val aliasesForKey = aliasesList.filter { it.targetKeyId() == keyEntry.keyId() }.map { it.aliasName() }
            KeyWithAliases(keyEntry.keyId(), aliasesForKey)
        }
    }

    fun getAvailableRegions(): List<String> {
        return Region.regions().map { it.id() }.sorted()
    }

    private fun createClient(): KmsClient {
        return KmsClient.builder()
            .endpointOverride(URI(settingsService.kmsEndpoint))
            .region(Region.of(settingsService.kmsRegion))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("key-id", "secret")))
            .build()
    }
}
