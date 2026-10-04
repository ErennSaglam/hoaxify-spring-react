package com.hoaxify.ws.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.hoaxify.ws.services.ITokenService;

/*
 * proxyTargetClass = true: @PreAuthorize kullanılan controller'lar (UserControllerImpl) bir arayüz
 * implement ediyor. Varsayılan JDK proxy sadece arayüz tipinde olur; @RestController ve @GetMapping
 * ise impl sınıfında durduğu için Spring MVC proxy'de hiçbir endpoint göremez (her istek 404).
 * Sınıf tabanlı (CGLIB) proxy bu anotasyonları korur. Spring Boot bunu normalde AopAutoConfiguration
 * ile zaten yapar, ama @WebMvcTest o konfigürasyonu yüklemediği için burada açıkça belirtiyoruz.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(proxyTargetClass = true)
public class SecurityConfiguration {

	private final ITokenService tokenService;
	private final HandlerExceptionResolver exceptionResolver;

	/*
	 * Constructor'ı elle yazdık: Spring'de HandlerExceptionResolver tipinde birden fazla bean var,
	 * hangisini istediğimizi @Qualifier ile belirtmemiz gerekiyor. Lombok'un ürettiği
	 * constructor bu anotasyonu parametreye kopyalamaz (ek lombok.config ayarı gerekir).
	 */
	public SecurityConfiguration(ITokenService tokenService,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
		this.tokenService = tokenService;
		this.exceptionResolver = exceptionResolver;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.PUT, "/api/v1/users/*").authenticated()
				.requestMatchers(HttpMethod.DELETE, "/api/v1/users/*").authenticated()
				.requestMatchers(HttpMethod.POST, "/api/v1/hoaxes").authenticated()
				.requestMatchers(HttpMethod.DELETE, "/api/v1/hoaxes/*").authenticated()
				.anyRequest().permitAll());

		// REST API: sunucuda oturum (HttpSession) tutulmaz, her istek token ile kimliğini kanıtlar
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.csrf(AbstractHttpConfigurer::disable);
		http.exceptionHandling(ex -> ex.authenticationEntryPoint(new AuthEntryPoint(exceptionResolver)));
		http.addFilterBefore(new TokenFilter(tokenService, exceptionResolver),
				UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
