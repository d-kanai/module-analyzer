package com.example.user.application;

import com.example.notification.expose.SendNotificationApi;

public class SignupCommand {
    private SendNotificationApi notificationApi;

    public void processUser(UserDto dto) {
        // Send notification when user is processed
        notificationApi.sendNotification(dto.getId(), "User processed");
    }
}
