package com.example.user.service;

import com.example.user.expose.UserApi;
import com.example.user.expose.UserDto;
import com.example.notification.expose.SendNotificationApi;

public class UserService {
    private UserApi userApi;
    private SendNotificationApi notificationApi;

    public void processUser(UserDto dto) {
        // Send notification when user is processed
        notificationApi.sendNotification(dto.getId(), "User processed");
    }
}
