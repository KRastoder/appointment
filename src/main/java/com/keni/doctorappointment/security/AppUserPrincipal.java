package com.keni.doctorappointment.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The authenticated principal of the application.
 *
 * <p>Two kinds of accounts exist and are told apart by {@link AccountType}:
 * patients ({@code ROLE_USER}) and doctors ({@code ROLE_DOCTOR}). The
 * authorities are what the authorisation rules ({@code @PreAuthorize},
 * {@code authorizeHttpRequests}) are written against.</p>
 *
 * <p>The id of the account is carried in the principal so authorisation
 * checks can be done without a second database lookup.</p>
 */
public record AppUserPrincipal(
		Long id,
		String email,
		String passwordHash,
		AccountType accountType,
		Collection<? extends GrantedAuthority> authorities) implements UserDetails {

	public enum AccountType {
		PATIENT,
		DOCTOR
	}

	public static AppUserPrincipal ofDoctor(Long id, String email, String passwordHash) {
		return new AppUserPrincipal(id, email, passwordHash, AccountType.DOCTOR,
				List.of(new SimpleGrantedAuthority("ROLE_DOCTOR")));
	}

	public static AppUserPrincipal ofPatient(Long id, String email, String passwordHash) {
		return new AppUserPrincipal(id, email, passwordHash, AccountType.PATIENT,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

	public boolean isDoctor() {
		return this.accountType == AccountType.DOCTOR;
	}

	// --- UserDetails ---------------------------------------------------

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return this.authorities;
	}

	@Override
	public String getUsername() {
		return this.email;
	}

	@Override
	public String getPassword() {
		return this.passwordHash;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
