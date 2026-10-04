package com.hoaxify.ws.services;

public interface IEmailService {

	void sendActivationEmail(String email, String activationToken);

	void sendPasswordResetEmail(String email, String passwordResetToken);
}
