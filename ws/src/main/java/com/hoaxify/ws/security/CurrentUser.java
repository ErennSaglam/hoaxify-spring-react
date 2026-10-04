package com.hoaxify.ws.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.hoaxify.ws.entities.User;

import lombok.Getter;

/**
 * SecurityContext'te tutulan giriş yapmış kullanıcı (principal).
 * Controller'da @AuthenticationPrincipal CurrentUser ile, @PreAuthorize'da principal.id ile erişilir.
 * Entity'nin kendisini değil, ihtiyaç duyulan alanların kopyasını tutar.
 */
@Getter
public class CurrentUser implements UserDetails {

	private final Long id;

	private final String username;

	private final String password;

	private final boolean enabled;

	public CurrentUser(User user) {
		this.id = user.getId();
		this.username = user.getUsername();
		this.password = user.getPassword();
		this.enabled = user.isActive();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_USER"));
	}
}
