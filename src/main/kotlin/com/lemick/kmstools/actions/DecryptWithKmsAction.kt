package com.lemick.kmstools.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.util.TextRange
import com.lemick.kmstools.services.KmsService
import com.lemick.kmstools.services.NotificationService
import software.amazon.awssdk.core.exception.SdkClientException

class DecryptWithKmsAction : AnAction() {

    private val kmsService = service<KmsService>()
    private val notificationService = service<NotificationService>()

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isVisible = editor?.caretModel?.primaryCaret?.hasSelection() == true
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return

        val primaryCaret = editor.caretModel.primaryCaret
        val start = primaryCaret.selectionStart
        val end = primaryCaret.selectionEnd
        val selectedText = editor.document.getText(TextRange(start, end))

        try {
            val decryptedValue = kmsService.decrypt(selectedText)
            WriteCommandAction.runWriteCommandAction(project) {
                editor.document.replaceString(start, end, decryptedValue)
            }
            editor.caretModel.primaryCaret.removeSelection()
            notificationService.notify(project, "Decryption successful", NotificationType.INFORMATION)

        } catch (e: SdkClientException) {
            notificationService.notify(
                project,
                "Error when connecting to endpoint: ${e.message}",
                NotificationType.ERROR
            )
            event.actionManager.getAction("SetupKmsSettingsAction").actionPerformed(event)

        } catch (e: Exception) {
            notificationService.notify(project, "Error during decryption: ${e.message}", NotificationType.ERROR)
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
