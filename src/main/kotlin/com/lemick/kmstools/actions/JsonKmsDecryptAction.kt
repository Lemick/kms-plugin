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
import com.lemick.kmstools.model.KeyWithAliases
import com.lemick.kmstools.services.KmsService
import com.lemick.kmstools.services.NotificationService
import com.lemick.kmstools.services.SettingsService


class JsonKmsDecryptAction : AnAction() {

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

        decryptSelectedText(project, editor)
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

    private fun decryptSelectedText(project: Project, editor: Editor) {
        val document = editor.document
        val primaryCaret = editor.caretModel.primaryCaret
        val start = primaryCaret.selectionStart
        val end = primaryCaret.selectionEnd
        val selectedText = document.getText(TextRange(start, end))

        try {
            val decryptedValue = kmsService.decryptJsonWithDataKey(selectedText)
            WriteCommandAction.runWriteCommandAction(project) {
                document.replaceString(start, end, decryptedValue)
            }
            primaryCaret.removeSelection()
        } catch (exception: Exception) {
            notificationService.notify(project, "Error during decryption: ${exception.message}", NotificationType.ERROR)
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
