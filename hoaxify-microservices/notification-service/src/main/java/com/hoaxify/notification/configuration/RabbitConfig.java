package com.hoaxify.notification.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hoaxify.common.event.HoaxifyEvents;
import com.hoaxify.common.messaging.HoaxifyQueues;

/**
 * user.registered olayını user-service de dinliyor. İkisinin AYRI kuyruğu olduğu için
 * aynı mesajın birer kopyasını alırlar (publish/subscribe). Tek kuyruğu paylaşsalardı
 * mesaj ikisinden sadece birine giderdi (competing consumers).
 */
@Configuration
public class RabbitConfig {

	public static final String USER_REGISTERED_QUEUE = "notification-service.user-registered";
	public static final String PASSWORD_RESET_QUEUE = "notification-service.password-reset-requested";

	@Bean
	public Queue notificationUserRegisteredQueue() {
		return HoaxifyQueues.durableQueue(USER_REGISTERED_QUEUE);
	}

	@Bean
	public Queue notificationPasswordResetQueue() {
		return HoaxifyQueues.durableQueue(PASSWORD_RESET_QUEUE);
	}

	@Bean
	public Binding notificationUserRegisteredBinding(Queue notificationUserRegisteredQueue,
			TopicExchange hoaxifyExchange) {
		return BindingBuilder.bind(notificationUserRegisteredQueue).to(hoaxifyExchange)
				.with(HoaxifyEvents.USER_REGISTERED);
	}

	@Bean
	public Binding notificationPasswordResetBinding(Queue notificationPasswordResetQueue,
			TopicExchange hoaxifyExchange) {
		return BindingBuilder.bind(notificationPasswordResetQueue).to(hoaxifyExchange)
				.with(HoaxifyEvents.PASSWORD_RESET_REQUESTED);
	}
}
