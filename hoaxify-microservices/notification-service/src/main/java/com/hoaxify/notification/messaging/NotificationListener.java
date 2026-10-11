package com.hoaxify.notification.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.hoaxify.common.event.PasswordResetRequestedEvent;
import com.hoaxify.common.event.UserRegisteredEvent;
import com.hoaxify.notification.configuration.RabbitConfig;
import com.hoaxify.notification.services.INotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationListener {

	private final INotificationService notificationService;

	@RabbitListener(queues = RabbitConfig.USER_REGISTERED_QUEUE)
	public void onUserRegistered(UserRegisteredEvent event) {
		notificationService.sendActivationMail(event);
	}

	@RabbitListener(queues = RabbitConfig.PASSWORD_RESET_QUEUE)
	public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
		notificationService.sendPasswordResetMail(event);
	}
}
