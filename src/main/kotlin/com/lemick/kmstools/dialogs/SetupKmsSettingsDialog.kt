package com.lemick.kmstools.dialogs

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class SetupKmsSettingsDialog(initialKmsEndpoint: String, initialRegion: String, availableRegions: List<String>) :
    DialogWrapper(true) {

    private val kmsEndpointUrlField = JTextField(initialKmsEndpoint, 50)
    private val kmsRegionField = ComboBox(availableRegions.toTypedArray())

    val selectedKmsEndpointUrl: String get() = kmsEndpointUrlField.text
    val selectedRegion: String get() = kmsRegionField.selectedItem as String

    init {
        init()
        title = "Setup KMS"
        kmsRegionField.selectedItem = initialRegion
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridLayout(4, 1))

        panel.add(JLabel("KMS Endpoint URL:"))
        panel.add(kmsEndpointUrlField)

        panel.add(JLabel("Region:"))
        panel.add(kmsRegionField)

        return panel
    }

    override fun isOKActionEnabled(): Boolean {
        return selectedKmsEndpointUrl.isNotEmpty() && kmsRegionField.selectedItem != null
    }
}

