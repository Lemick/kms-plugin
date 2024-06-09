package com.lemick.kmstools.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project


@Service(Service.Level.APP)
class NotificationService {

    fun notify(project: Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Encrypt With KMS")
            .createNotification(content, type)
            .notify(project)
    }
}
