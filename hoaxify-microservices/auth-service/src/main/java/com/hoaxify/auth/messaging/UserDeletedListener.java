package com.hoaxify.auth.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.hoaxify.auth.configuration.RabbitConfig;
import com.hoaxify.auth.services.IAccountService;
import com.hoaxify.common.event.UserDeletedEvent;

import lombok.RequiredArgsConstructor;

/** user-service'te profil silinince hesabı ve tüm oturumları kapatır */
@Component
@RequiredArgsConstructor
public class UserDeletedListener {

	private final IAccountService accountService;

	@RabbitListener(queues = RabbitConfig.USER_DELETED_QUEUE)
	public void onUserDeleted(UserDeletedEvent event) {
		accountService.deleteAccount(event.userId());
	}
}
