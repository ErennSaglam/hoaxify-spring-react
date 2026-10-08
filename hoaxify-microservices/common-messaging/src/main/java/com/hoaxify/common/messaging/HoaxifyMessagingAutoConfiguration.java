package com.hoaxify.common.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoaxify.common.event.HoaxifyEvents;

/**
 * Tüm servislerde ortak RabbitMQ altyapısı (Spring Boot auto-configuration, bkz. common-web).
 *
 * Mesajın yolculuğu:
 *   yayıncı -> hoaxify.events (topic) -> servis-kuyruğu -> @RabbitListener
 *                                              │ 3 deneme de başarısız (bkz. config: listener.simple.retry)
 *                                              ▼
 *                              hoaxify.events.dlx (fanout) -> hoaxify.dead-letter
 *
 * DLQ'daki mesajlar RabbitMQ arayüzünden (http://localhost:15672) incelenip tekrar gönderilebilir.
 */
@AutoConfiguration
public class HoaxifyMessagingAutoConfiguration {

	@Bean
	public TopicExchange hoaxifyExchange() {
		return new TopicExchange(HoaxifyEvents.EXCHANGE, true, false);
	}

	@Bean
	public FanoutExchange hoaxifyDeadLetterExchange() {
		return new FanoutExchange(HoaxifyEvents.DEAD_LETTER_EXCHANGE, true, false);
	}

	@Bean
	public Queue hoaxifyDeadLetterQueue() {
		return QueueBuilder.durable(HoaxifyEvents.DEAD_LETTER_QUEUE).build();
	}

	@Bean
	public Binding hoaxifyDeadLetterBinding(Queue hoaxifyDeadLetterQueue, FanoutExchange hoaxifyDeadLetterExchange) {
		return BindingBuilder.bind(hoaxifyDeadLetterQueue).to(hoaxifyDeadLetterExchange);
	}

	/**
	 * Mesajlar JSON olarak taşınır (Java serileştirmesi değil): başka dilde yazılmış servisler de okuyabilir.
	 * Spring'in ObjectMapper'ı kullanılır ki LocalDateTime gibi tipler de doğru yazılsın.
	 */
	@Bean
	@ConditionalOnMissingBean(MessageConverter.class)
	public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
		return new Jackson2JsonMessageConverter(objectMapper);
	}
}
