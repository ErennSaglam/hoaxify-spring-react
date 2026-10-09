package com.hoaxify.gateway.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;

class ProtectedEndpointsTest {

	@ParameterizedTest(name = "{0} {1} -> giriş gerekli mi: {2}")
	@CsvSource({
			"PUT,    /api/v1/users/5,          true",
			"DELETE, /api/v1/users/5,          true",
			"POST,   /api/v1/hoaxes,           true",
			"DELETE, /api/v1/hoaxes/10,        true",
			"GET,    /api/v1/users/5,          false",
			"GET,    /api/v1/hoaxes,           false",
			"POST,   /api/v1/users,            false",   // kayıt anonim
			"PATCH,  /api/v1/users/abc/active, false",
			"POST,   /api/v1/auth,             false",
			"DELETE, /api/v1/users/5/hoaxes,   false"    // alt yol eşleşmemeli
	})
	void requiresAuthentication(String method, String path, boolean expected) {
		assertEquals(expected, ProtectedEndpoints.requiresAuthentication(HttpMethod.valueOf(method), path));
	}
}
