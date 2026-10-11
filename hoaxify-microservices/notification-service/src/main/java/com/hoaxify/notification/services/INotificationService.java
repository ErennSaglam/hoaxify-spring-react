package com.hoaxify.notification.services;

import com.hoaxify.common.event.PasswordResetRequestedEvent;
import com.hoaxify.common.event.UserRegisteredEvent;

public interface INotificationService {

	void sendActivationMail(UserRegisteredEvent event);

	void sendPasswordResetMail(PasswordResetRequestedEvent event);
}
