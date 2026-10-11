package com.hoaxify.hoax.configuration;

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

	public static final String USER_DELETED_QUEUE = "hoax-service.user-deleted";

	@Bean
	public Queue userDeletedQueue() {
		return HoaxifyQueues.durableQueue(USER_DELETED_QUEUE);
	}

	@Bean
	public Binding userDeletedBinding(Queue userDeletedQueue, TopicExchange hoaxifyExchange) {
		return BindingBuilder.bind(userDeletedQueue).to(hoaxifyExchange).with(HoaxifyEvents.USER_DELETED);
	}
}
