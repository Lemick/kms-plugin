package com.lemick.kmsplugin.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.lemick.kmsplugin.dialogs.SelectKmsEndpointDialog
import com.lemick.kmsplugin.services.KmsService
import com.lemick.kmsplugin.services.SettingsService
import software.amazon.awssdk.core.exception.SdkClientException

class DecryptWithKmsAction : AnAction() {

    private val kmsService = service<KmsService>()
    private val settingsService = service<SettingsService>()

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isVisible = editor?.caretModel?.primaryCaret?.hasSelection() == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val primaryCaret = editor.caretModel.primaryCaret
        val start = primaryCaret.selectionStart
        val end = primaryCaret.selectionEnd
        val selectedText = editor.document.getText(TextRange(start, end))

        decryptText(project, editor, selectedText, start, end)
    }

    private fun decryptText(project: Project, editor: Editor, selectedText: String, start: Int, end: Int) {
        while (true) {
            try {
                val decryptedValue = kmsService.decrypt(selectedText)
                WriteCommandAction.runWriteCommandAction(project) {
                    editor.document.replaceString(start, end, decryptedValue)
                }
                editor.caretModel.primaryCaret.removeSelection()
                notify(project, "Decryption successful", NotificationType.INFORMATION)
                return
            } catch (e: SdkClientException) {
                notify(project, "Error when connecting to endpoint: ${e.message}", NotificationType.ERROR)
                val dialog = SelectKmsEndpointDialog(settingsService.kmsEndpoint)
                if (!dialog.showAndGet()) {
                    return
                }
                settingsService.kmsEndpoint = dialog.kmsEndpointUrl
            } catch (e: Exception) {
                notify(project, "Error during decryption: ${e.message}", NotificationType.ERROR)
                return
            }
        }
    }

    private fun notify(project: Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Decrypt With KMS")
            .createNotification(content, type)
            .notify(project)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
