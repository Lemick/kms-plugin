package com.lemick.kmstools.services

import AES
import AESType
import ai.grazie.utils.mpp.Base64
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.lemick.kmstools.model.JsonObject
import com.lemick.kmstools.model.KeyWithAliases
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.*
import java.net.URI


@Service(Service.Level.APP)
class KmsService {

    private val settingsService = service<SettingsService>()

    fun encrypt(dataToEncrypt: String, keyId: String): String {
        val encryptRequest = EncryptRequest.builder()
            .keyId(keyId)
            .plaintext(SdkBytes.fromUtf8String(dataToEncrypt))
            .build()

        val response = createClient().encrypt(encryptRequest)
        return Base64.encode(response.ciphertextBlob().asByteArray())
    }

    fun encryptJsonWithDatakey(dataToEncrypt: String, keyId: String): String {
        val generateDataKeyRequest = GenerateDataKeyRequest.builder()
            .keyId(keyId)
            .keySpec("AES_256")
            .build()

        val datakeyResult = createClient().generateDataKey(generateDataKeyRequest)
        val cipherTextKey = datakeyResult.ciphertextBlob()

        val aesEncryptor = AES.create(AESType.GCM, datakeyResult.plaintext().asByteArray())
        val aesEncryptedResult = aesEncryptor.encrypt(dataToEncrypt.toByteArray())

        val json = JsonObject()
        json.put("ciphered_key", Base64.encode(cipherTextKey.asByteArray()))
        json.put("ciphered_payload", Base64.encode(aesEncryptedResult.first))
        json.put("nonce", Base64.encode(aesEncryptedResult.second))

        return json.toString()
    }

    fun decryptJsonWithDataKey(json: String): String {
        val encryptedJson = JsonObject.parse(json)

        val cipheredKey = Base64.decode(encryptedJson.get("ciphered_key"))
        val cipheredPayload = Base64.decode(encryptedJson.get("ciphered_payload"))
        val nonce = Base64.decode(encryptedJson.get("nonce"))

        val decryptRequest = DecryptRequest.builder().ciphertextBlob(SdkBytes.fromByteArray(cipheredKey)).build()
        val datakeyResult = createClient().decrypt(decryptRequest).plaintext().asByteArray()

        for (type in AESType.values()) {
            try {
                val aesEncryptor = AES.create(type, datakeyResult)
                val aesEncryptedResult = aesEncryptor.decrypt(cipheredPayload, nonce)
                return String(aesEncryptedResult)
            } catch (_: Exception) {
            }
        }

        throw IllegalArgumentException("AES decryption failed")
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
