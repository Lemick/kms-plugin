package com.lemick.kmstools.dialogs

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.lemick.kmstools.model.KeyWithAliases
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class SelectKmsKeyDialog(availableKeys: List<KeyWithAliases>) : DialogWrapper(true) {
    private val kmsKeyIdComboBox = ComboBox(availableKeys.toTypedArray())

    val selectedKmsKeyId: String get() = (kmsKeyIdComboBox.selectedItem as KeyWithAliases).keyId

    init {
        init()
        title = "Select KMS Key"
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridLayout(2, 1))

        val keyLabel = JLabel("KMS Key ID:")
        panel.add(keyLabel)
        panel.add(kmsKeyIdComboBox)

        return panel
    }

    override fun isOKActionEnabled(): Boolean {
        return kmsKeyIdComboBox.selectedItem != null
    }

    override fun getInitialSize(): Dimension {
        return Dimension(500, 200)
    }

    override fun isResizable(): Boolean {
        return false
    }
}

