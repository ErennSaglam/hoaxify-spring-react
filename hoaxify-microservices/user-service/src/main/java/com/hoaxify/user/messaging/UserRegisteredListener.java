package com.hoaxify.user.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.hoaxify.common.event.UserRegisteredEvent;
import com.hoaxify.user.configuration.RabbitConfig;
import com.hoaxify.user.services.IUserProfileService;

import lombok.RequiredArgsConstructor;

/**
 * Kursundaki akışta auth-service, user-service'i Feign ile ÇAĞIRIYORDU: user-service kapalıysa kayıt da
 * patlıyordu. Olay tabanlı akışta user-service kapalıyken gelen mesajlar kuyrukta bekler, servis
 * açılınca işlenir. Kayıt işlemi user-service'in ayakta olmasına bağlı değildir.
 */
@Component
@RequiredArgsConstructor
public class UserRegisteredListener {

	private final IUserProfileService userProfileService;

	@RabbitListener(queues = RabbitConfig.USER_REGISTERED_QUEUE)
	public void onUserRegistered(UserRegisteredEvent event) {
		userProfileService.createProfile(event);
	}
}
