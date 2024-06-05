package com.lemick.kmsplugin.services

import ai.grazie.utils.mpp.Base64
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.DecryptRequest
import software.amazon.awssdk.services.kms.model.EncryptRequest
import software.amazon.awssdk.services.kms.model.KeyListEntry
import software.amazon.awssdk.services.kms.model.ListKeysRequest
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

    fun decrypt(dataToDecrypt: String): String {
        val decryptRequest = DecryptRequest.builder()
            .ciphertextBlob(SdkBytes.fromByteArray(Base64.decode(dataToDecrypt)))
            .build()

        val response = createClient().decrypt(decryptRequest)
        return response.plaintext().asUtf8String()
    }

    fun listKeys(): MutableList<KeyListEntry> {
        val listKeysRequest = ListKeysRequest.builder()
            .limit(100)
            .build()

        val response = createClient().listKeys(listKeysRequest)
        return response.keys()
    }


    private fun createClient(): KmsClient {
        return KmsClient.builder()
            .endpointOverride(URI(settingsService.kmsEndpoint))
            .region(Region.US_EAST_1)
            .credentialsProvider(AnonymousCredentialsProvider.create())
            .build()
    }
}
