package com.hoaxify.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Sadece bilgi mesajı dönen endpoint'ler için: { "message": "..." } */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoMessage {

	private String message;
}
