package com.hoaxify.user.services.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import com.hoaxify.common.web.exception.BaseException;
import com.hoaxify.user.configuration.UserProperties;
import com.hoaxify.user.exception.UserErrorType;
import com.hoaxify.user.services.IFileService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Monolith'teki FileServiceImpl'in aynısı. Not: birden fazla user-service kopyası çalışacaksa
 * yerel disk yetmez (her kopyanın diski ayrı); gerçek projelerde resimler S3 / MinIO gibi bir nesne deposuna yazılır.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements IFileService {

	private final UserProperties userProperties;

	private final Tika tika = new Tika();

	@PostConstruct
	void createStorageDirectories() throws IOException {
		Files.createDirectories(profileDirectory());
	}

	@Override
	public String saveProfileImage(String base64Image) {
		String filename = UUID.randomUUID().toString();
		try {
			Files.write(profileDirectory().resolve(filename), decode(base64Image));
			return filename;
		} catch (IOException ex) {
			throw new BaseException(UserErrorType.FILE_SAVE_FAILURE);
		}
	}

	@Override
	public String detectType(String base64Image) {
		return tika.detect(decode(base64Image));
	}

	@Override
	public void deleteProfileImage(String filename) {
		if (filename == null) {
			return;
		}
		try {
			Files.deleteIfExists(profileDirectory().resolve(filename));
		} catch (IOException ex) {
			log.warn("Profile image {} could not be deleted", filename, ex);
		}
	}

	private byte[] decode(String base64Image) {
		String data = base64Image.contains(",") ? base64Image.split(",", 2)[1] : base64Image;
		return Base64.getDecoder().decode(data);
	}

	private Path profileDirectory() {
		return Paths.get(userProperties.getStorage().getRoot(), userProperties.getStorage().getProfile());
	}
}
