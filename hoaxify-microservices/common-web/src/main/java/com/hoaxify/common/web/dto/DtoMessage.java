package com.hoaxify.common.web.dto;

/** Sadece bilgi mesajı dönen endpoint'ler için: { "message": "..." } */
public record DtoMessage(String message) {
}
