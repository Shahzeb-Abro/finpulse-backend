package com.finpulse.config.security;

import com.finpulse.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bridge between our User entity and Spring Security.
 *
 * Implements THREE interfaces so it works seamlessly across all auth flows:
 * - UserDetails: For local username/password auth
 * - OAuth2User:  For plain OAuth2 flows
 * - OidcUser:    For OpenID Connect flows (Google with "openid" scope)
 *
 * This triple implementation means our success handler, JWT filter, and
 * controllers all get the same UserPrincipal type regardless of how
 * the user authenticated.
 */
@Getter
public class UserPrincipal implements UserDetails, OAuth2User, OidcUser {

    private Long id;
    private String email;
    private String password;
    private String fullName;
    private String avatarUrl;
    private boolean accountLocked;
    private Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    public UserPrincipal(Long id, String email, String password, String fullName,
                         String avatarUrl, boolean accountLocked,
                         Collection<? extends GrantedAuthority> authorities,
                         Map<String, Object> attributes) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.accountLocked = accountLocked;
        this.authorities = authorities;
        this.attributes = attributes;
    }

    /** Factory: local auth (no OAuth2 attributes) */
    public static UserPrincipal create(User user) {
        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toList());

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getAccountLocked(),
                authorities,
                null
        );
    }

    /** Factory: OAuth2 / OIDC auth (with provider attributes) */
    public static UserPrincipal create(User user, Map<String, Object> attributes) {
        UserPrincipal principal = create(user);
        principal.attributes = attributes;
        return principal;
    }

    // ── UserDetails methods ──────────────────────────────────

    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return !accountLocked; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    // ── OAuth2User methods ───────────────────────────────────

    @Override public Map<String, Object> getAttributes() { return attributes; }
    @Override public String getName() { return String.valueOf(id); }

    // ── OidcUser methods ─────────────────────────────────────
    // These return null because we don't need OIDC-specific data
    // after the initial authentication. Our JWT handles everything.

    @Override public Map<String, Object> getClaims() { return attributes; }
    @Override public OidcUserInfo getUserInfo() { return null; }
    @Override public OidcIdToken getIdToken() { return null; }
}