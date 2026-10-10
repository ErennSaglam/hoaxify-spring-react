package com.hoaxify.user.services;

public interface IFileService {

	String saveProfileImage(String base64Image);

	String detectType(String base64Image);

	void deleteProfileImage(String filename);
}
