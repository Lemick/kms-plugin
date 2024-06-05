package com.lemick.kmsplugin.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.lemick.kmsplugin.dialogs.SelectKmsKeyDialog
import com.lemick.kmsplugin.dialogs.SelectKmsEndpointDialog
import com.lemick.kmsplugin.services.SettingsService
import com.lemick.kmsplugin.services.KmsService


class EncryptWithKmsAction : AnAction() {

    private val kmsService = service<KmsService>()
    private val settingsService = service<SettingsService>()

    override fun update(e: AnActionEvent) {
        val editor: Editor = e.getRequiredData(CommonDataKeys.EDITOR)
        e.presentation.isVisible = editor.caretModel.primaryCaret.hasSelection()
        super.update(e)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val availableKmsKeysIds = fetchKmsKeys(project) ?: return
        if (availableKmsKeysIds.isEmpty()) {
            notify(project, "No existing KMS Key, please create one", NotificationType.ERROR)
            return
        }

        val selectKmsKeyDialog = SelectKmsKeyDialog(availableKmsKeysIds)
        if (selectKmsKeyDialog.showAndGet()) {
            settingsService.encryptionKmsKeyId = selectKmsKeyDialog.kmsKeyId
            encryptSelectedText(project, editor, selectKmsKeyDialog.kmsKeyId)
        }
    }

    private fun fetchKmsKeys(project: Project): List<String>? {
        while (true) {
            try {
                return kmsService.listKeys().map { it.keyId() }
            } catch (exception: Exception) {
                notify(project, "Error fetching key list: $exception", NotificationType.ERROR)
                val dialog = SelectKmsEndpointDialog(settingsService.kmsEndpoint)
                if (!dialog.showAndGet()) {
                    return null
                }
                settingsService.kmsEndpoint = dialog.kmsEndpointUrl
            }
        }
    }

    private fun encryptSelectedText(project: Project, editor: Editor, kmsKeyId: String) {
        val document = editor.document
        val primaryCaret = editor.caretModel.primaryCaret
        val start = primaryCaret.selectionStart
        val end = primaryCaret.selectionEnd
        val selectedText = document.getText(TextRange(start, end))

        try {
            val encryptedValue = kmsService.encrypt(selectedText, kmsKeyId)
            WriteCommandAction.runWriteCommandAction(project) {
                document.replaceString(start, end, encryptedValue)
            }
            primaryCaret.removeSelection()
            notify(project, "Encryption successful", NotificationType.INFORMATION)
        } catch (exception: Exception) {
            notify(project, "Error during encryption: ${exception.message}", NotificationType.ERROR)
        }
    }

    private fun notify(project: Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Encrypt With KMS")
            .createNotification(content, type)
            .notify(project)
    }


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
