package com.lemick.kmsplugin.dialogs

import com.intellij.openapi.ui.DialogWrapper
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class SelectKmsEndpointDialog(kmsEndpointUrl: String) : DialogWrapper(true) {
    private val kmsEndpointUrlField = JTextField(kmsEndpointUrl, 50)

    val kmsEndpointUrl: String get() = kmsEndpointUrlField.text

    init {
        init()
        title = "Enter KMS Endpoint"
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridLayout(2, 1))

        val urlLabel = JLabel("KMS Endpoint URL:")
        panel.add(urlLabel)
        panel.add(kmsEndpointUrlField)

        return panel
    }

    override fun isOKActionEnabled(): Boolean {
        return kmsEndpointUrl.isNotEmpty()
    }
}

