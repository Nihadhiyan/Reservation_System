package com.bookfair.backend.security;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.bookfair.backend.model.OrganizationMember;
import com.bookfair.backend.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomUserPrincipal implements UserDetails {

    private UUID id;
    private String username;
    private String password;
    private String systemRole;
    private Map<String, String> orgRoles = new HashMap<>();
    private Boolean active;

    public CustomUserPrincipal(User user, List<OrganizationMember> members) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.systemRole = user.getSystemRole() != null ? user.getSystemRole().name() : "CUSTOMER";
        this.active = user.getActive();
        if (members != null) {
            for (OrganizationMember member : members) {
                this.orgRoles.put(member.getOrganization().getId().toString(), member.getRole().name());
            }
        }
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + systemRole)
        );
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(active);
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
}
