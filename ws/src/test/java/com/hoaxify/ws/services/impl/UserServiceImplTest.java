package com.hoaxify.ws.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;

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

/**
 * UserServiceImpl unit testleri.
 *
 * Tespit edilen senaryolar:
 *  createUser           : success | email unique değil (DataIntegrityViolation catch) | repository başka hata
 *                         | mail gönderilemedi (MailException catch) | mail servisi MailException dışı hata
 *  activateUser         : success | token bulunamadı | repository exception
 *  getUsers             : anonim (currentUserId null -> findAll) | giriş yapmış (-> findByIdNot) | boş sayfa
 *  getUserById          : success | not found | repository exception
 *  updateUser           : sadece username | bio dolu | image dolu (yeni kaydet + eskiyi sil) | profil null
 *                         | not found | dosya kaydetme hatası
 *  deleteUser           : resimli kullanıcı | profili olmayan kullanıcı | not found
 *  requestPasswordReset : kayıtlı e-posta | kayıtsız e-posta (sessizce geç) | mail hatası
 *  resetPassword        : success | geçersiz token
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private IEmailService emailService;

	@Mock
	private IFileService fileService;

	@Mock
	private UserMapper userMapper;

	@InjectMocks
	private UserServiceImpl userService;

	private EasyRandom easyRandom;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
	}

	// ------------------------------------------------------------------ createUser

	@Test
	void createUser_success() {
		// Arrange
		DtoUserIU request = easyRandom.nextObject(DtoUserIU.class);
		User user = easyRandom.nextObject(User.class);
		user.setActive(false);
		user.setActivationToken(null);
		DtoUser expected = easyRandom.nextObject(DtoUser.class);

		when(userMapper.toEntity(request)).thenReturn(user);
		when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
		when(userRepository.saveAndFlush(user)).thenReturn(user);
		when(userMapper.toDto(user)).thenReturn(expected);

		// Act
		DtoUser result = userService.createUser(request);

		// Assert
		assertNotNull(result);
		assertEquals(expected.getId(), result.getId());
		assertEquals(expected.getUsername(), result.getUsername());

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository, times(1)).saveAndFlush(captor.capture());
		User savedUser = captor.getValue();
		assertEquals("hashed-password", savedUser.getPassword());
		assertNotNull(savedUser.getActivationToken());
		assertFalse(savedUser.isActive());

		verify(emailService, times(1)).sendActivationEmail(user.getEmail(), savedUser.getActivationToken());
		verify(userMapper, times(1)).toDto(user);
	}

	@Test
	void createUser_emailNotUnique_shouldThrowFieldException() {
		// Arrange
		DtoUserIU request = easyRandom.nextObject(DtoUserIU.class);
		User user = easyRandom.nextObject(User.class);

		when(userMapper.toEntity(request)).thenReturn(user);
		when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
		when(userRepository.saveAndFlush(user)).thenThrow(new DataIntegrityViolationException("uk_users_email"));

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> userService.createUser(request));

		// Assert
		assertEquals(MessageType.EMAIL_NOT_UNIQUE, exception.getMessageType());
		assertEquals("email", exception.getField());

		verify(userRepository, times(1)).saveAndFlush(user);
		verify(emailService, never()).sendActivationEmail(any(), any());
		verify(userMapper, never()).toDto(any());
	}

	@Test
	void createUser_repositoryException_shouldPropagate() {
		// Arrange
		DtoUserIU request = easyRandom.nextObject(DtoUserIU.class);
		User user = easyRandom.nextObject(User.class);

		when(userMapper.toEntity(request)).thenReturn(user);
		when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
		when(userRepository.saveAndFlush(user)).thenThrow(new RuntimeException("DB error"));

		// Act
		RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.createUser(request));

		// Assert: sadece DataIntegrityViolationException yakalanıyor, diğer hatalar olduğu gibi yükselir
		assertEquals("DB error", exception.getMessage());
		assertFalse(exception instanceof BaseException);
		verifyNoInteractions(emailService);
	}

	@Test
	void createUser_mailException_shouldThrowActivationEmailFailure() {
		// Arrange
		DtoUserIU request = easyRandom.nextObject(DtoUserIU.class);
		User user = easyRandom.nextObject(User.class);

		when(userMapper.toEntity(request)).thenReturn(user);
		when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
		when(userRepository.saveAndFlush(user)).thenReturn(user);
		doThrow(new MailSendException("SMTP down"))
				.when(emailService)
				.sendActivationEmail(any(String.class), any(String.class));

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> userService.createUser(request));

		// Assert
		assertEquals(MessageType.ACTIVATION_EMAIL_FAILURE, exception.getMessageType());
		verify(userRepository, times(1)).saveAndFlush(user);
		verify(userMapper, never()).toDto(any());
	}

	@Test
	void createUser_emailServiceNonMailException_shouldPropagate() {
		// Arrange
		DtoUserIU request = easyRandom.nextObject(DtoUserIU.class);
		User user = easyRandom.nextObject(User.class);

		when(userMapper.toEntity(request)).thenReturn(user);
		when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
		when(userRepository.saveAndFlush(user)).thenReturn(user);
		doThrow(new IllegalStateException("unexpected"))
				.when(emailService)
				.sendActivationEmail(any(String.class), any(String.class));

		// Act & Assert: catch bloğu sadece MailException yakalar
		assertThrows(IllegalStateException.class, () -> userService.createUser(request));
		verify(userMapper, never()).toDto(any());
	}

	// ------------------------------------------------------------------ activateUser

	@Test
	void activateUser_success() {
		// Arrange
		String token = "activation-token";
		User user = easyRandom.nextObject(User.class);
		user.setActive(false);
		user.setActivationToken(token);

		when(userRepository.findByActivationToken(token)).thenReturn(Optional.of(user));

		// Act
		userService.activateUser(token);

		// Assert
		assertTrue(user.isActive());
		assertNull(user.getActivationToken());
		verify(userRepository, times(1)).findByActivationToken(token);
		// Dirty checking: save çağrılmadan transaction sonunda güncellenir
		verify(userRepository, never()).save(any());
	}

	@Test
	void activateUser_notFound_shouldThrowInvalidToken() {
		// Arrange
		String token = "unknown-token";
		when(userRepository.findByActivationToken(token)).thenReturn(Optional.empty());

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> userService.activateUser(token));

		// Assert
		assertEquals(MessageType.INVALID_ACTIVATION_TOKEN, exception.getMessageType());
		verify(userRepository, times(1)).findByActivationToken(token);
	}

	@Test
	void activateUser_repositoryException() {
		// Arrange
		String token = "activation-token";
		when(userRepository.findByActivationToken(token)).thenThrow(new RuntimeException("DB error"));

		// Act & Assert
		assertThrows(RuntimeException.class, () -> userService.activateUser(token));
		verify(userRepository, times(1)).findByActivationToken(token);
	}

	// ------------------------------------------------------------------ getUsers

	@Test
	void getUsers_anonymous_shouldReturnAllUsers() {
		// Arrange
		Pageable pageable = PageRequest.of(0, 2);
		User user1 = easyRandom.nextObject(User.class);
		User user2 = easyRandom.nextObject(User.class);
		DtoUser dto1 = easyRandom.nextObject(DtoUser.class);
		DtoUser dto2 = easyRandom.nextObject(DtoUser.class);
		Page<User> page = new PageImpl<>(List.of(user1, user2), pageable, 5);

		when(userRepository.findAll(pageable)).thenReturn(page);
		when(userMapper.toDto(user1)).thenReturn(dto1);
		when(userMapper.toDto(user2)).thenReturn(dto2);

		// Act
		DtoPage<DtoUser> result = userService.getUsers(pageable, null);

		// Assert
		assertNotNull(result);
		assertEquals(2, result.getContent().size());
		assertEquals(dto1, result.getContent().get(0));
		assertEquals(5, result.getTotalElements());
		assertTrue(result.isFirst());
		assertFalse(result.isLast());

		verify(userRepository, times(1)).findAll(pageable);
		verify(userRepository, never()).findByIdNot(any(), any());
	}

	@Test
	void getUsers_loggedIn_shouldExcludeCurrentUser() {
		// Arrange
		Long currentUserId = 1L;
		Pageable pageable = PageRequest.of(0, 3);
		User user = easyRandom.nextObject(User.class);
		DtoUser dto = easyRandom.nextObject(DtoUser.class);

		when(userRepository.findByIdNot(currentUserId, pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 1));
		when(userMapper.toDto(user)).thenReturn(dto);

		// Act
		DtoPage<DtoUser> result = userService.getUsers(pageable, currentUserId);

		// Assert
		assertEquals(1, result.getContent().size());
		assertEquals(dto.getId(), result.getContent().get(0).getId());

		verify(userRepository, times(1)).findByIdNot(currentUserId, pageable);
		verify(userRepository, never()).findAll(any(Pageable.class));
	}

	@Test
	void getUsers_emptyList() {
		// Arrange
		Pageable pageable = PageRequest.of(0, 3);
		when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

		// Act
		DtoPage<DtoUser> result = userService.getUsers(pageable, null);

		// Assert
		assertNotNull(result);
		assertTrue(result.getContent().isEmpty());
		assertEquals(0, result.getTotalElements());
		verifyNoInteractions(userMapper);
	}

	// ------------------------------------------------------------------ getUserById

	@Test
	void getUserById_success() {
		// Arrange
		Long userId = 1L;
		User user = easyRandom.nextObject(User.class);
		user.setId(userId);
		DtoUser expected = easyRandom.nextObject(DtoUser.class);
		expected.setId(userId);

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(userMapper.toDto(user)).thenReturn(expected);

		// Act
		DtoUser result = userService.getUserById(userId);

		// Assert
		assertNotNull(result);
		assertEquals(userId, result.getId());
		assertEquals(expected.getUsername(), result.getUsername());
		assertEquals(expected.getImage(), result.getImage());

		verify(userRepository, times(1)).findById(userId);
		verify(userMapper, times(1)).toDto(user);
	}

	@Test
	void getUserById_notFound() {
		// Arrange
		Long userId = 99L;
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> userService.getUserById(userId));

		// Assert
		assertEquals(MessageType.USER_NOT_FOUND, exception.getMessageType());
		assertEquals("99", exception.getArgs()[0]);
		verify(userRepository, times(1)).findById(userId);
		verifyNoInteractions(userMapper);
	}

	@Test
	void getUserById_repositoryException() {
		// Arrange
		Long userId = 1L;
		when(userRepository.findById(userId)).thenThrow(new RuntimeException("DB error"));

		// Act & Assert
		assertThrows(RuntimeException.class, () -> userService.getUserById(userId));
		verify(userRepository, times(1)).findById(userId);
	}

	// ------------------------------------------------------------------ updateUser

	@Test
	void updateUser_onlyUsername_shouldNotTouchBioOrImage() {
		// Arrange
		Long userId = 1L;
		User user = easyRandom.nextObject(User.class);
		String oldBio = user.getProfile().getBio();
		String oldImage = user.getProfile().getImage();
		DtoUserUpdateIU request = new DtoUserUpdateIU("new-username", null, null);
		DtoUser expected = easyRandom.nextObject(DtoUser.class);

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(userRepository.save(user)).thenReturn(user);
		when(userMapper.toDto(user)).thenReturn(expected);

		// Act
		DtoUser result = userService.updateUser(userId, request);

		// Assert
		assertEquals(expected, result);
		assertEquals("new-username", user.getUsername());
		assertEquals(oldBio, user.getProfile().getBio());
		assertEquals(oldImage, user.getProfile().getImage());

		verify(userRepository, times(1)).save(user);
		verifyNoInteractions(fileService);
	}

	@Test
	void updateUser_withBio_shouldUpdateBio() {
		// Arrange
		Long userId = 1L;
		User user = easyRandom.nextObject(User.class);
		DtoUserUpdateIU request = new DtoUserUpdateIU("username", null, "new bio");

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(userRepository.save(user)).thenReturn(user);
		when(userMapper.toDto(user)).thenReturn(easyRandom.nextObject(DtoUser.class));

		// Act
		userService.updateUser(userId, request);

		// Assert
		assertEquals("new bio", user.getProfile().getBio());
		verifyNoInteractions(fileService);
	}

	@Test
	void updateUser_withImage_shouldSaveNewImageThenDeleteOldOne() {
		// Arrange
		Long userId = 1L;
		User user = easyRandom.nextObject(User.class);
		user.getProfile().setImage("old.png");
		DtoUserUpdateIU request = new DtoUserUpdateIU("username", "data:image/png;base64,AAA", null);

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(fileService.saveProfileImage(request.getImage())).thenReturn("new.png");
		when(userRepository.save(user)).thenReturn(user);
		when(userMapper.toDto(user)).thenReturn(easyRandom.nextObject(DtoUser.class));

		// Act
		userService.updateUser(userId, request);

		// Assert
		assertEquals("new.png", user.getProfile().getImage());

		// Önce yeni dosya kaydedilmeli, eski dosya ancak sonra silinmeli (kaydetme patlarsa eski resim kaybolmasın)
		InOrder inOrder = inOrder(fileService);
		inOrder.verify(fileService).saveProfileImage(request.getImage());
		inOrder.verify(fileService).deleteProfileImage("old.png");
	}

	@Test
	void updateUser_profileNull_shouldCreateProfile() {
		// Arrange
		Long userId = 1L;
		User user = easyRandom.nextObject(User.class);
		user.setProfile(null);
		DtoUserUpdateIU request = new DtoUserUpdateIU("username", null, "hello");

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(userRepository.save(user)).thenReturn(user);
		when(userMapper.toDto(user)).thenReturn(easyRandom.nextObject(DtoUser.class));

		// Act
		userService.updateUser(userId, request);

		// Assert
		UserProfile profile = user.getProfile();
		assertNotNull(profile);
		assertEquals("hello", profile.getBio());
		assertSame(user, profile.getUser());
	}

	@Test
	void updateUser_notFound_shouldNotSave() {
		// Arrange
		Long userId = 1L;
		DtoUserUpdateIU request = easyRandom.nextObject(DtoUserUpdateIU.class);
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> userService.updateUser(userId, request));

		// Assert
		assertEquals(MessageType.USER_NOT_FOUND, exception.getMessageType());
		verify(userRepository, never()).save(any());
		verifyNoInteractions(fileService);
	}

	@Test
	void updateUser_fileSaveException_shouldNotDeleteOldImageOrSave() {
		// Arrange
		Long userId = 1L;
		User user = easyRandom.nextObject(User.class);
		user.getProfile().setImage("old.png");
		DtoUserUpdateIU request = new DtoUserUpdateIU("username", "data:image/png;base64,AAA", null);

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(fileService.saveProfileImage(request.getImage()))
				.thenThrow(new BaseException(MessageType.FILE_SAVE_FAILURE));

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> userService.updateUser(userId, request));

		// Assert
		assertEquals(MessageType.FILE_SAVE_FAILURE, exception.getMessageType());
		assertEquals("old.png", user.getProfile().getImage());
		verify(fileService, never()).deleteProfileImage(any());
		verify(userRepository, never()).save(any());
	}

	// ------------------------------------------------------------------ deleteUser

	@Test
	void deleteUser_success_shouldDeleteUserAndImage() {
		// Arrange
		Long userId = 1L;
		User user = easyRandom.nextObject(User.class);
		user.getProfile().setImage("avatar.png");

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));

		// Act
		userService.deleteUser(userId);

		// Assert
		verify(userRepository, times(1)).findById(userId);
		verify(userRepository, times(1)).delete(user);
		verify(fileService, times(1)).deleteProfileImage("avatar.png");
		verifyNoMoreInteractions(userRepository, fileService);
	}

	@Test
	void deleteUser_profileNull_shouldPassNullImageToFileService() {
		// Arrange
		Long userId = 1L;
		User user = easyRandom.nextObject(User.class);
		user.setProfile(null);

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));

		// Act
		userService.deleteUser(userId);

		// Assert
		verify(userRepository, times(1)).delete(user);
		verify(fileService, times(1)).deleteProfileImage(null);
	}

	@Test
	void deleteUser_notFound_shouldNotDeleteAnything() {
		// Arrange
		Long userId = 1L;
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		// Act
		assertThrows(BaseException.class, () -> userService.deleteUser(userId));

		// Assert
		verify(userRepository, never()).delete(any());
		verifyNoInteractions(fileService);
	}

	// ------------------------------------------------------------------ requestPasswordReset

	@Test
	void requestPasswordReset_success() {
		// Arrange
		DtoPasswordResetIU request = easyRandom.nextObject(DtoPasswordResetIU.class);
		User user = easyRandom.nextObject(User.class);
		user.setEmail(request.getEmail());
		user.setPasswordResetToken(null);

		when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

		// Act
		userService.requestPasswordReset(request);

		// Assert
		assertNotNull(user.getPasswordResetToken());
		verify(emailService, times(1)).sendPasswordResetEmail(request.getEmail(), user.getPasswordResetToken());
	}

	@Test
	void requestPasswordReset_unknownEmail_shouldNotSendMail() {
		// Arrange
		DtoPasswordResetIU request = easyRandom.nextObject(DtoPasswordResetIU.class);
		when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

		// Act
		userService.requestPasswordReset(request);

		// Assert: hata fırlatılmaz (user enumeration koruması), mail de gönderilmez
		verify(userRepository, times(1)).findByEmail(request.getEmail());
		verifyNoInteractions(emailService);
	}

	@Test
	void requestPasswordReset_mailException_shouldPropagate() {
		// Arrange
		DtoPasswordResetIU request = easyRandom.nextObject(DtoPasswordResetIU.class);
		User user = easyRandom.nextObject(User.class);

		when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
		doThrow(new MailSendException("SMTP down"))
				.when(emailService)
				.sendPasswordResetEmail(any(String.class), any(String.class));

		// Act & Assert: bu metotta catch yok, transaction rollback olsun diye hata yükselir
		assertThrows(MailSendException.class, () -> userService.requestPasswordReset(request));
	}

	// ------------------------------------------------------------------ resetPassword

	@Test
	void resetPassword_success() {
		// Arrange
		String token = "reset-token";
		DtoPasswordUpdateIU request = new DtoPasswordUpdateIU("N3wPassword");
		User user = easyRandom.nextObject(User.class);
		user.setActive(false);
		user.setPasswordResetToken(token);

		when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(user));
		when(passwordEncoder.encode("N3wPassword")).thenReturn("new-hash");

		// Act
		userService.resetPassword(token, request);

		// Assert
		assertEquals("new-hash", user.getPassword());
		assertNull(user.getPasswordResetToken());
		assertTrue(user.isActive());
		verify(passwordEncoder, times(1)).encode("N3wPassword");
	}

	@Test
	void resetPassword_invalidToken_shouldNotEncodePassword() {
		// Arrange
		String token = "bad-token";
		DtoPasswordUpdateIU request = new DtoPasswordUpdateIU("N3wPassword");
		when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.empty());

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> userService.resetPassword(token, request));

		// Assert
		assertEquals(MessageType.INVALID_PASSWORD_RESET_TOKEN, exception.getMessageType());
		verifyNoInteractions(passwordEncoder);
	}
}
