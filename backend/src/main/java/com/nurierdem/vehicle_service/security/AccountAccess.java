package com.nurierdem.vehicle_service.security;

import com.nurierdem.vehicle_service.enums.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AccountAccess {
    public AccountPrincipal current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal account)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Oturum bulunamadı.");
        }
        return account;
    }

    public void requireSelf(Long id) {
        requireMatch(current(), id);
    }

    public void requireSelfOrAdmin(Long id) {
        AccountPrincipal account = current();
        if (account.user().role() != UserRole.ADMIN) {
            requireMatch(account, id);
        }
    }

    private void requireMatch(AccountPrincipal account, Long id) {
        if (!account.user().id().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu hesap adına işlem yapamazsınız.");
        }
    }
}
