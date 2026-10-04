package com.hoaxify.ws.services.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import com.hoaxify.ws.configuration.HoaxifyProperties;
import com.hoaxify.ws.exception.BaseException;
import com.hoaxify.ws.exception.MessageType;
import com.hoaxify.ws.services.IFileService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements IFileService {

	private final HoaxifyProperties hoaxifyProperties;

	private final Tika tika = new Tika();

	/** Uygulama açılırken uploads/profile klasörlerini oluşturur */
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
			throw new BaseException(MessageType.FILE_SAVE_FAILURE);
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
			// Eski resmin silinememesi kullanıcı işlemini bozmamalı; sadece logluyoruz
			log.warn("Profile image {} could not be deleted", filename, ex);
		}
	}

	/** "data:image/png;base64,iVBOR..." -> byte[] */
	private byte[] decode(String base64Image) {
		String data = base64Image.contains(",") ? base64Image.split(",", 2)[1] : base64Image;
		return Base64.getDecoder().decode(data);
	}

	private Path profileDirectory() {
		return Paths.get(hoaxifyProperties.getStorage().getRoot(), hoaxifyProperties.getStorage().getProfile());
	}
}
