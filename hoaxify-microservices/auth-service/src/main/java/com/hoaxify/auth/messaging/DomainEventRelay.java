package com.hoaxify.auth.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.hoaxify.common.event.HoaxifyEvents;
import com.hoaxify.common.event.PasswordResetRequestedEvent;
import com.hoaxify.common.event.UserRegisteredEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servis olayı Spring'in içine yayınlar (ApplicationEventPublisher); bu sınıf onu RabbitMQ'ya aktarır.
 *
 * Neden doğrudan RabbitMQ'ya göndermiyoruz? @TransactionalEventListener(AFTER_COMMIT):
 * mesaj ancak veritabanı transaction'ı BAŞARIYLA commit olduktan sonra gider. Aksi halde
 * "kullanıcı kaydedilemedi ama 'kayıt oldu' mesajı gitti" gibi tutarsızlıklar olurdu.
 *
 * Kalan risk: commit oldu ama RabbitMQ o an kapalıysa mesaj kaybolur. Üretimde bunun çözümü
 * "Transactional Outbox" pattern'idir: olay aynı transaction'da bir outbox tablosuna yazılır,
 * ayrı bir işlem tablodan okuyup kuyruğa gönderir. (Mülakatta sık sorulur.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventRelay {

	private final RabbitTemplate rabbitTemplate;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void on(UserRegisteredEvent event) {
		log.debug("Publishing {} for userId={}", HoaxifyEvents.USER_REGISTERED, event.userId());
		rabbitTemplate.convertAndSend(HoaxifyEvents.EXCHANGE, HoaxifyEvents.USER_REGISTERED, event);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void on(PasswordResetRequestedEvent event) {
		rabbitTemplate.convertAndSend(HoaxifyEvents.EXCHANGE, HoaxifyEvents.PASSWORD_RESET_REQUESTED, event);
	}
}
