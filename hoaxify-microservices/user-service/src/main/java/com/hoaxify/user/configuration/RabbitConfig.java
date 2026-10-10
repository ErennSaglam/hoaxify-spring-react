package com.hoaxify.user.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hoaxify.common.event.HoaxifyEvents;
import com.hoaxify.common.messaging.HoaxifyQueues;

@Configuration
public class RabbitConfig {

	/** Kuyruk adı "servis.olay": her servisin kendi kuyruğu olur, aynı olayı her biri ayrı ayrı alır */
	public static final String USER_REGISTERED_QUEUE = "user-service.user-registered";

	@Bean
	public Queue userRegisteredQueue() {
		return HoaxifyQueues.durableQueue(USER_REGISTERED_QUEUE);
	}

	@Bean
	public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange hoaxifyExchange) {
		return BindingBuilder.bind(userRegisteredQueue).to(hoaxifyExchange).with(HoaxifyEvents.USER_REGISTERED);
	}
}
