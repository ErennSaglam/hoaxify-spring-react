package com.hoaxify.hoax.dto;

/** DAO'nun ham sonucu: bu serviste sadece userId ve sayı var; isim ve resim gRPC ile eklenir */
public record UserHoaxCount(Long userId, long hoaxCount) {
}
