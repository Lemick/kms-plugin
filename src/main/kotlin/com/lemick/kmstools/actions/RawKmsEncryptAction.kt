package com.lemick.kmstools.actions

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
import com.lemick.kmstools.dialogs.SelectKmsKeyDialog
import com.lemick.kmstools.model.KeyWithAliases
import com.lemick.kmstools.services.KmsService
import com.lemick.kmstools.services.NotificationService
import com.lemick.kmstools.services.SettingsService


class RawKmsEncryptAction : AnAction() {

    private val kmsService = service<KmsService>()
    private val settingsService = service<SettingsService>()
    private val notificationService = service<NotificationService>()

    override fun update(e: AnActionEvent) {
        val editor: Editor = e.getRequiredData(CommonDataKeys.EDITOR)
        e.presentation.isVisible = editor.caretModel.primaryCaret.hasSelection()
        super.update(e)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val availableKmsKeysIds = fetchKmsKeys(project, e) ?: return
        if (availableKmsKeysIds.isEmpty()) {
            notificationService.notify(
                project,
                "No existing KMS Key on this region, please create one",
                NotificationType.ERROR
            )
            return
        }

        val selectKmsKeyDialog = SelectKmsKeyDialog(availableKmsKeysIds)
        if (selectKmsKeyDialog.showAndGet()) {
            settingsService.encryptionKmsKeyId = selectKmsKeyDialog.selectedKmsKeyId
            encryptSelectedText(project, editor, selectKmsKeyDialog.selectedKmsKeyId)
        }
    }

    private fun fetchKmsKeys(project: Project, e: AnActionEvent): List<KeyWithAliases>? {
        try {
            return kmsService.listKeysWithAliases()
        } catch (exception: Exception) {
            notificationService.notify(project, "Error fetching key list: $exception", NotificationType.ERROR)
            e.actionManager.getAction("SetupKmsSettingsAction").actionPerformed(e)
            return null
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
        } catch (exception: Exception) {
            notificationService.notify(project, "Error during encryption: ${exception.message}", NotificationType.ERROR)
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
