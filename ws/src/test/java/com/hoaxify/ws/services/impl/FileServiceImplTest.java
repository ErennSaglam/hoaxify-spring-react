package com.hoaxify.ws.services.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.hoaxify.ws.configuration.HoaxifyProperties;
import com.hoaxify.ws.exception.BaseException;
import com.hoaxify.ws.exception.MessageType;

/**
 * FileServiceImpl dosya sistemiyle çalışır; mock'lanacak bir bağımlılığı yok.
 * Gerçek diske, JUnit'in her test için açıp sonra sildiği geçici klasöre (@TempDir) yazıyoruz.
 *
 * Tespit edilen senaryolar:
 *  createStorageDirectories : klasör oluşur
 *  saveProfileImage         : success | data URL'siz saf base64 | yazma hatası (IOException catch)
 *  detectType               : png | metin | uzantı yalan söylüyor
 *  deleteProfileImage       : success | null (erken return) | olmayan dosya | silme hatası (IOException catch)
 */
class FileServiceImplTest {

	/** 1x1 piksel geçerli PNG */
	private static final String PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
	private static final String PNG_DATA_URL = "data:image/png;base64," + PNG_BASE64;

	@TempDir
	Path tempDir;

	private HoaxifyProperties properties;

	private FileServiceImpl fileService;

	private Path profileDir;

	@BeforeEach
	void setUp() throws Exception {
		properties = new HoaxifyProperties();
		properties.getStorage().setRoot(tempDir.toString());
		properties.getStorage().setProfile("profile");
		fileService = new FileServiceImpl(properties);
		fileService.createStorageDirectories(); // normalde @PostConstruct ile Spring çağırır
		profileDir = tempDir.resolve("profile");
	}

	@Test
	void createStorageDirectories_success() {
		assertTrue(Files.isDirectory(profileDir));
	}

	@Test
	void saveProfileImage_success() throws Exception {
		// Act
		String filename = fileService.saveProfileImage(PNG_DATA_URL);

		// Assert
		assertNotNull(filename);
		Path saved = profileDir.resolve(filename);
		assertTrue(Files.exists(saved));
		// PNG dosyaları her zaman 0x89 'P' 'N' 'G' ile başlar
		byte[] header = Arrays.copyOf(Files.readAllBytes(saved), 4);
		assertArrayEquals(new byte[] { (byte) 0x89, 'P', 'N', 'G' }, header);
	}

	@Test
	void saveProfileImage_plainBase64WithoutDataUrlPrefix() {
		// Act
		String filename = fileService.saveProfileImage(PNG_BASE64);

		// Assert
		assertTrue(Files.exists(profileDir.resolve(filename)));
	}

	@Test
	void saveProfileImage_generatesUniqueNames() {
		assertNotEquals(fileService.saveProfileImage(PNG_DATA_URL), fileService.saveProfileImage(PNG_DATA_URL));
	}

	@Test
	void saveProfileImage_ioException_shouldThrowFileSaveFailure() throws Exception {
		// Arrange: profil klasörünü silip yerine aynı isimde bir DOSYA koyuyoruz -> yazma IOException verir
		Files.delete(profileDir);
		Files.createFile(profileDir);

		// Act
		BaseException exception = assertThrows(BaseException.class,
				() -> fileService.saveProfileImage(PNG_DATA_URL));

		// Assert
		assertEquals(MessageType.FILE_SAVE_FAILURE, exception.getMessageType());
	}

	@Test
	void detectType_png() {
		assertEquals("image/png", fileService.detectType(PNG_DATA_URL));
	}

	@Test
	void detectType_text() {
		assertEquals("text/plain", fileService.detectType("data:text/plain;base64,aGVsbG8="));
	}

	@Test
	void detectType_declaredTypeIgnored_contentWins() {
		// data URL "image/png" dese bile içerik metin: Tika byte'lara bakar
		assertEquals("text/plain", fileService.detectType("data:image/png;base64,aGVsbG8="));
	}

	@Test
	void deleteProfileImage_success() {
		// Arrange
		String filename = fileService.saveProfileImage(PNG_DATA_URL);

		// Act
		fileService.deleteProfileImage(filename);

		// Assert
		assertFalse(Files.exists(profileDir.resolve(filename)));
	}

	@Test
	void deleteProfileImage_nullFilename_shouldDoNothing() {
		assertDoesNotThrow(() -> fileService.deleteProfileImage(null));
	}

	@Test
	void deleteProfileImage_missingFile_shouldNotThrow() {
		assertDoesNotThrow(() -> fileService.deleteProfileImage("does-not-exist"));
	}

	@Test
	void deleteProfileImage_ioException_shouldBeLoggedNotThrown() throws Exception {
		// Arrange: silinecek isimde içi dolu bir KLASÖR var -> Files.deleteIfExists DirectoryNotEmptyException fırlatır
		Path directory = Files.createDirectory(profileDir.resolve("not-a-file"));
		Files.createFile(directory.resolve("child"));

		// Act & Assert: eski resmin silinememesi kullanıcı işlemini bozmamalı, sadece loglanır
		assertDoesNotThrow(() -> fileService.deleteProfileImage("not-a-file"));
		assertTrue(Files.exists(directory));
	}
}
