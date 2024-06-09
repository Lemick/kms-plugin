package com.lemick.kmstools.services

import ai.grazie.utils.mpp.Base64
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.lemick.kmstools.model.KeyWithAliases
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.DecryptRequest
import software.amazon.awssdk.services.kms.model.EncryptRequest
import software.amazon.awssdk.services.kms.model.ListAliasesRequest
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
