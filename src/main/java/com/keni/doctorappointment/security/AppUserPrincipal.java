package com.keni.doctorappointment.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The authenticated principal of the application.
 *
 * <p>Three kinds of accounts exist and are told apart by {@link AccountType}:
 * patients ({@code ROLE_USER}), doctors ({@code ROLE_DOCTOR}) and staff
 * ({@code ROLE_ADMIN}). Authorities are what the authorisation rules
 * ({@code @PreAuthorize}, {@code authorizeHttpRequests}) are written
 * against.</p>
 *
 * <p>The account id is carried in the principal (and in the JWT claims) so
 * ownership checks need no second database lookup.</p>
 *
 * <p>A doctor always wins over a patient for the same e-mail address, which is
 * why e-mail uniqueness is enforced across both tables on write
 * (see {@code UserService#assertEmailAvailable}).</p>
 */
public record AppUserPrincipal(
		Long id,
		String email,
		String passwordHash,
		AccountType accountType,
		Collection<? extends GrantedAuthority> authorities) implements UserDetails {

	public enum AccountType {
		PATIENT,
		DOCTOR,
		ADMIN
	}

	public static AppUserPrincipal ofDoctor(Long id, String email, String passwordHash) {
		return new AppUserPrincipal(id, email, passwordHash, AccountType.DOCTOR,
				List.of(new SimpleGrantedAuthority("ROLE_DOCTOR")));
	}

	public static AppUserPrincipal ofPatient(Long id, String email, String passwordHash) {
		return new AppUserPrincipal(id, email, passwordHash, AccountType.PATIENT,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

	/** Staff account: manages the service catalogue and doctor accounts. */
	public static AppUserPrincipal ofAdmin(Long id, String email, String passwordHash) {
		return new AppUserPrincipal(id, email, passwordHash, AccountType.ADMIN,
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")));
	}

	public boolean isDoctor() {
		return this.accountType == AccountType.DOCTOR;
	}

	public boolean isAdmin() {
		return this.accountType == AccountType.ADMIN;
	}

	/** Role names without the {@code ROLE_} prefix, as stored in the JWT. */
	public List<String> roles() {
		return this.authorities.stream()
			.map(GrantedAuthority::getAuthority)
			.map(authority -> authority.startsWith("ROLE_") ? authority.substring("ROLE_".length()) : authority)
			.toList();
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
