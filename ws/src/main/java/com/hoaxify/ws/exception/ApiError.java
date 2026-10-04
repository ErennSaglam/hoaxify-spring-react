package com.hoaxify.ws.exception;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tüm hata cevaplarının ortak formatı. Frontend status, message ve
 * validationErrors alanlarını okuyor, o yüzden bu alan adları sabit kalmalı.
 *
 * {
 *   "id": "c1b4...",              -> loglarda bu hatayı bulmak için
 *   "status": 400,
 *   "message": "Validation error",
 *   "path": "/api/v1/users",
 *   "timestamp": "2026-09-30T18:00:00Z",
 *   "validationErrors": { "email": "E-mail in use" }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class ApiError<T> {

	private String id;

	private int status;

	private String message;

	private String path;

	private Instant timestamp;

	private T validationErrors;
}
