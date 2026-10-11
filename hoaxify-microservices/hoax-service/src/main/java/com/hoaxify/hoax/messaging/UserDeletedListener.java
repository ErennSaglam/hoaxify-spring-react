package com.hoaxify.hoax.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.hoaxify.common.event.UserDeletedEvent;
import com.hoaxify.hoax.configuration.RabbitConfig;
import com.hoaxify.hoax.services.IHoaxService;

import lombok.RequiredArgsConstructor;

/** Monolith'teki "User silinince hoax'ları da silinsin" (cascade = REMOVE) davranışının olay tabanlı karşılığı */
@Component
@RequiredArgsConstructor
public class UserDeletedListener {

	private final IHoaxService hoaxService;

	@RabbitListener(queues = RabbitConfig.USER_DELETED_QUEUE)
	public void onUserDeleted(UserDeletedEvent event) {
		hoaxService.deleteAllOfUser(event.userId());
	}
}
