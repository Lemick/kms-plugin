package com.lemick.kmstools.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.lemick.kmstools.dialogs.SetupKmsSettingsDialog
import com.lemick.kmstools.services.SettingsService
import com.lemick.kmstools.services.KmsService
import com.lemick.kmstools.services.NotificationService


class SetupKmsSettingsAction : AnAction() {

    private val kmsService = service<KmsService>()
    private val settingsService = service<SettingsService>()
    private val notificationService = service<NotificationService>()

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val selectKmsKeyDialog = SetupKmsSettingsDialog(settingsService.kmsEndpoint,settingsService.kmsRegion,  kmsService.getAvailableRegions())

        if (selectKmsKeyDialog.showAndGet()) {
            settingsService.kmsEndpoint = selectKmsKeyDialog.selectedKmsEndpointUrl
            settingsService.kmsRegion = selectKmsKeyDialog.selectedRegion

            notificationService.notify(project, "Settings saved", NotificationType.INFORMATION)
        }
    }
}
