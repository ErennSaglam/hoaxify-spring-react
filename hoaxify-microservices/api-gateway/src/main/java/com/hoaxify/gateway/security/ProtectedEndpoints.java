package com.hoaxify.gateway.security;

import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Giriş gerektiren endpoint'ler (monolith'teki SecurityConfiguration'daki authenticated() kuralları).
 * Gateway burada 401 döner ki istek servise hiç gitmesin. Servisler de @CurrentUserId ile ayrıca
 * kontrol eder (defense in depth: bir katman atlanırsa diğeri yakalar).
 */
public final class ProtectedEndpoints {

	private record Rule(HttpMethod method, PathPattern pattern) {
	}

	private static final PathPatternParser PARSER = PathPatternParser.defaultInstance;

	private static final List<Rule> RULES = List.of(
			new Rule(HttpMethod.PUT, PARSER.parse("/api/v1/users/{id}")),
			new Rule(HttpMethod.DELETE, PARSER.parse("/api/v1/users/{id}")),
			new Rule(HttpMethod.POST, PARSER.parse("/api/v1/hoaxes")),
			new Rule(HttpMethod.DELETE, PARSER.parse("/api/v1/hoaxes/{id}")));

	private ProtectedEndpoints() {
	}

	public static boolean requiresAuthentication(HttpMethod method, String path) {
		PathContainer container = PathContainer.parsePath(path);
		return RULES.stream().anyMatch(rule -> rule.method().equals(method) && rule.pattern().matches(container));
	}
}
