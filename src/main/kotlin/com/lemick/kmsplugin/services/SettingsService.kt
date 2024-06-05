package com.lemick.kmsplugin.services

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.Service


@Service(Service.Level.APP)
class SettingsService {

    private val properties = PropertiesComponent.getInstance()
    private val pluginId = "com.lemick.kmsdecrypt"

    var encryptionKmsKeyId: String
        get() = properties.getValue("$pluginId.encryption-key-id", "")
        set(value) = properties.setValue("$pluginId.encryption-key-id", value)

    var kmsEndpoint: String
        get() = properties.getValue("$pluginId.kms-endpoint", "http://localhost:4566")
        set(value) = properties.setValue("$pluginId.kms-endpoint", value)
}
