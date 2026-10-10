package com.hoaxify.user.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.hoaxify.common.event.HoaxifyEvents;
import com.hoaxify.common.event.UserDeletedEvent;

import lombok.RequiredArgsConstructor;

/** Profil silme transaction'ı commit olduktan sonra user.deleted olayını yayınlar (bkz. auth-service DomainEventRelay) */
@Component
@RequiredArgsConstructor
public class DomainEventRelay {

	private final RabbitTemplate rabbitTemplate;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void on(UserDeletedEvent event) {
		rabbitTemplate.convertAndSend(HoaxifyEvents.EXCHANGE, HoaxifyEvents.USER_DELETED, event);
	}
}
