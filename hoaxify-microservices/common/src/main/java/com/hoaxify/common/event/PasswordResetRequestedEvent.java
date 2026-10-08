package com.hoaxify.common.event;

/** auth-service yayınlar -> notification-service şifre sıfırlama maili gönderir */
public record PasswordResetRequestedEvent(
		String email,
		String passwordResetToken,
		String locale) {
}
