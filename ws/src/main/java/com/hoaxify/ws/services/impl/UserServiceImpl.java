package com.hoaxify.ws.services.impl;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaxify.ws.dto.DtoPage;
import com.hoaxify.ws.dto.DtoPasswordResetIU;
import com.hoaxify.ws.dto.DtoPasswordUpdateIU;
import com.hoaxify.ws.dto.DtoUser;
import com.hoaxify.ws.dto.DtoUserIU;
import com.hoaxify.ws.dto.DtoUserUpdateIU;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.entities.UserProfile;
import com.hoaxify.ws.exception.BaseException;
import com.hoaxify.ws.exception.MessageType;
import com.hoaxify.ws.mapper.UserMapper;
import com.hoaxify.ws.repository.UserRepository;
import com.hoaxify.ws.services.IEmailService;
import com.hoaxify.ws.services.IFileService;
import com.hoaxify.ws.services.IUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Akış: Controller DTO verir -> burada Entity'ye çevrilir -> Repository ile kaydedilir
 * -> Entity tekrar DTO'ya çevrilip Controller'a döner.
 *
 * Constructor injection: bağımlılıklar private final, @RequiredArgsConstructor
 * bunlar için constructor üretir. Tek constructor olduğu için Spring onu otomatik kullanır, ek anotasyon gerekmez.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final IEmailService emailService;
	private final IFileService fileService;
	private final UserMapper userMapper;

	/**
	 * Mail gönderilemezse (MailException -> BaseException, ikisi de RuntimeException)
	 * transaction rollback olur ve kullanıcı kaydedilmemiş olur.
	 */
	@Override
	@Transactional
	public DtoUser createUser(DtoUserIU dtoUserIU) {
		User user = userMapper.toEntity(dtoUserIU);
		user.setPassword(passwordEncoder.encode(dtoUserIU.getPassword()));
		user.setActivationToken(UUID.randomUUID().toString());

		try {
			// @UniqueEmail validator'ından sonra aynı anda gelen iki istek için DB unique constraint'i son savunma hattı
			userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException ex) {
			throw BaseException.forField(MessageType.EMAIL_NOT_UNIQUE, "email");
		}

		try {
			emailService.sendActivationEmail(user.getEmail(), user.getActivationToken());
		} catch (MailException ex) {
			log.warn("Activation e-mail could not be sent to {}", user.getEmail(), ex);
			throw new BaseException(MessageType.ACTIVATION_EMAIL_FAILURE);
		}
		return userMapper.toDto(user);
	}

	@Override
	@Transactional
	public void activateUser(String activationToken) {
		User user = userRepository.findByActivationToken(activationToken)
				.orElseThrow(() -> new BaseException(MessageType.INVALID_ACTIVATION_TOKEN));
		user.setActive(true);
		user.setActivationToken(null);
		// save() çağırmaya gerek yok: transaction içindeki managed entity'deki değişiklikler
		// commit sırasında otomatik UPDATE'e dönüşür (dirty checking).
	}

	@Override
	@Transactional(readOnly = true)
	public DtoPage<DtoUser> getUsers(Pageable pageable, Long currentUserId) {
		Page<User> users = currentUserId == null
				? userRepository.findAll(pageable)
				: userRepository.findByIdNot(currentUserId, pageable);
		return DtoPage.of(users, userMapper::toDto);
	}

	@Override
	@Transactional(readOnly = true)
	public DtoUser getUserById(Long id) {
		return userMapper.toDto(findUserOrThrow(id));
	}

	@Override
	@Transactional
	public DtoUser updateUser(Long id, DtoUserUpdateIU dtoUserUpdateIU) {
		User user = findUserOrThrow(id);
		user.setUsername(dtoUserUpdateIU.getUsername());

		UserProfile profile = user.getProfile();
		if (profile == null) {
			profile = new UserProfile();
			user.setProfileBidirectional(profile);
		}
		if (dtoUserUpdateIU.getBio() != null) {
			profile.setBio(dtoUserUpdateIU.getBio());
		}
		if (dtoUserUpdateIU.getImage() != null) {
			String newImage = fileService.saveProfileImage(dtoUserUpdateIU.getImage());
			fileService.deleteProfileImage(profile.getImage());
			profile.setImage(newImage);
		}
		return userMapper.toDto(userRepository.save(user));
	}

	@Override
	@Transactional
	public void deleteUser(Long id) {
		User user = findUserOrThrow(id);
		String image = user.getProfile() != null ? user.getProfile().getImage() : null;
		// cascade: profil, hoax'lar ve token'lar da silinir
		userRepository.delete(user);
		fileService.deleteProfileImage(image);
	}

	/**
	 * E-posta kayıtlı olmasa bile hata dönmüyoruz: aksi halde saldırgan bu endpoint
	 * ile hangi e-postaların kayıtlı olduğunu öğrenebilir (user enumeration).
	 */
	@Override
	@Transactional
	public void requestPasswordReset(DtoPasswordResetIU dtoPasswordResetIU) {
		userRepository.findByEmail(dtoPasswordResetIU.getEmail()).ifPresentOrElse(user -> {
			user.setPasswordResetToken(UUID.randomUUID().toString());
			emailService.sendPasswordResetEmail(user.getEmail(), user.getPasswordResetToken());
		}, () -> log.debug("Password reset requested for unknown e-mail {}", dtoPasswordResetIU.getEmail()));
	}

	@Override
	@Transactional
	public void resetPassword(String passwordResetToken, DtoPasswordUpdateIU dtoPasswordUpdateIU) {
		User user = userRepository.findByPasswordResetToken(passwordResetToken)
				.orElseThrow(() -> new BaseException(MessageType.INVALID_PASSWORD_RESET_TOKEN));
		user.setPasswordResetToken(null);
		user.setPassword(passwordEncoder.encode(dtoPasswordUpdateIU.getPassword()));
		// Mail linkine tıklayabildiğine göre e-posta adresi doğrulanmış demektir
		user.setActive(true);
	}

	private User findUserOrThrow(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new BaseException(MessageType.USER_NOT_FOUND, String.valueOf(id)));
	}
}
