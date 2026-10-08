package com.hoaxify.common.messaging;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;

import com.hoaxify.common.event.HoaxifyEvents;

/**
 * Servislerin kendi kuyruklarını tanımlarken kullandığı fabrika. Her kuyruk:
 *  - durable: RabbitMQ yeniden başlasa da kaybolmaz
 *  - dead letter exchange'e bağlı: tüm denemelere rağmen işlenemeyen mesaj silinmez, DLQ'ya düşer
 */
public final class HoaxifyQueues {

	private HoaxifyQueues() {
	}

	public static Queue durableQueue(String name) {
		return QueueBuilder.durable(name)
				.deadLetterExchange(HoaxifyEvents.DEAD_LETTER_EXCHANGE)
				.build();
	}
}
