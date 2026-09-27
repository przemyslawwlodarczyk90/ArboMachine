package com.example.arbomachine.infrastructure.security;

import com.example.arbomachine.domain.user.entity.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Adapter konta ArboMachine na interfejs UserDetails, ktorego wymaga Spring Security. */
public class UserAccountDetails implements UserDetails {

    private final UserAccount userAccount;

    public UserAccountDetails(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    /** Dokladnie jedno uprawnienie - rola konta, np. ROLE_ARBORIST. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userAccount.getRole() == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority(userAccount.getRole().toSpringSecurityAuthority()));
    }

    @Override
    public String getPassword() {
        return userAccount.getPasswordHash();
    }

    /** Nazwa uzytkownika w Spring Security = adres e-mail. */
    @Override
    public String getUsername() {
        return userAccount.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !userAccount.isLockedByAdmin();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Konto jest aktywne dopiero po potwierdzeniu adresu e-mail. */
    @Override
    public boolean isEnabled() {
        return userAccount.isEmailConfirmed();
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }
}
