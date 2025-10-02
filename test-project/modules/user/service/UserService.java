package com.example.user.service;

import com.example.user.expose.UserApi;
import com.example.user.expose.UserDto;
import com.example.notification.expose.NotificationApi;

public class UserService {
    private UserApi userApi;
    private NotificationApi notificationApi;

    public void processUser(UserDto dto) {
        // Send notification when user is processed
        notificationApi.sendNotification(dto.getId(), "User processed");
    }
}
