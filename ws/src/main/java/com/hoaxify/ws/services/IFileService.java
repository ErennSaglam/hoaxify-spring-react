package com.hoaxify.ws.services;

public interface IFileService {

	/** Base64 data URL'i diske yazar, oluşturulan dosya adını döner */
	String saveProfileImage(String base64Image);

	/** Tika ile içerikten MIME tipi tespiti (ör. image/png) */
	String detectType(String base64Image);

	void deleteProfileImage(String filename);
}
