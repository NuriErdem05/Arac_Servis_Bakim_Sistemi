package com.nurierdem.vehicle_service.service;

import com.nurierdem.vehicle_service.dto.AuthResponse.UserInfo;
import com.nurierdem.vehicle_service.dto.CreateUserRequest;
import com.nurierdem.vehicle_service.dto.RegisterRequest;
import com.nurierdem.vehicle_service.dto.TemporaryAccountResponse;
import com.nurierdem.vehicle_service.entity.User;
import com.nurierdem.vehicle_service.enums.UserRole;
import com.nurierdem.vehicle_service.repository.UserRepository;
import com.nurierdem.vehicle_service.security.AccountPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class AccountService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final SecureRandom random = new SecureRandom();

    public AccountService(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Transactional
    public UserInfo register(RegisterRequest request) {
        String hash = encodePassword(request.password());
        User user = newUser(request.fullName(), request.email(), UserRole.USER);
        user.setPasswordHash(hash);
        return UserInfo.from(saveNewAccount(user));
    }

    @Transactional(readOnly = true)
    public List<UserInfo> listAccounts() {
        return users.findAllByArchivedFalseOrderByFullNameAscIdAsc().stream().map(UserInfo::from).toList();
    }

    @Transactional
    public void archiveAccount(Long id) {
        User user = users.findAccountForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hesap bulunamadı."));
        if (user.getRole() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Yönetici hesapları silinemez.");
        }
        if (!user.isArchived()) {
            user.setArchived(true);
            user.setCredentialVersion(user.getCredentialVersion() + 1);
        }
    }

    @Transactional
    public TemporaryAccountResponse createManagedAccount(CreateUserRequest request) {
        if (request.role() != UserRole.STAFF && request.role() != UserRole.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Müşteri veya personel hesabı seçin.");
        }
        User user = newUser(request.fullName(), request.email(), request.role());
        String temporary = setTemporaryPassword(user);
        return new TemporaryAccountResponse(UserInfo.from(saveNewAccount(user)), temporary);
    }

    @Transactional
    public TemporaryAccountResponse renewTemporaryPassword(Long id) {
        User user = users.findAccountForUpdate(id)
                .filter(account -> !account.isArchived())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hesap bulunamadı."));
        if (user.getRole() == UserRole.ADMIN || (user.hasPassword() && !user.isMustChangePassword())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu hesap ilk şifresini zaten belirledi.");
        }
        String temporary = setTemporaryPassword(user);
        user.setCredentialVersion(user.getCredentialVersion() + 1);
        return new TemporaryAccountResponse(UserInfo.from(user), temporary);
    }

    @Transactional
    public User changeInitialPassword(AccountPrincipal account, String password) {
        User user = users.findAccountForUpdate(account.user().id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Hesap bulunamadı."));
        if (user.isArchived() || !user.isMustChangePassword() || user.getCredentialVersion() != account.credentialVersion()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Oturum geçersiz. Yeniden giriş yapın.");
        }
        String hash = encodePassword(password);
        if (passwords.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçici şifreden farklı bir şifre belirleyin.");
        }
        user.setPasswordHash(hash);
        user.setMustChangePassword(false);
        user.setCredentialVersion(user.getCredentialVersion() + 1);
        return user;
    }

    private User newUser(String name, String email, UserRole role) {
        User user = new User();
        user.setFullName(name.strip());
        user.setEmail(email.strip().toLowerCase(Locale.ROOT));
        user.setRole(role);
        return user;
    }

    private User saveNewAccount(User user) {
        if (users.findByEmailIgnoreCase(user.getEmail()).isPresent()) {
            throw emailUnavailable();
        }
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException failure) {
            if (failure.getMostSpecificCause() instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                throw emailUnavailable();
            }
            throw failure;
        }
    }

    private ResponseStatusException emailUnavailable() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Bu e-posta ile bir hesap zaten kayıtlı.");
    }

    private String setTemporaryPassword(User user) {
        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        String password = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        user.setPasswordHash(passwords.encode(password));
        user.setMustChangePassword(true);
        return password;
    }

    private String encodePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Şifre UTF-8 olarak en fazla 72 bayt olabilir.");
        }
        return passwords.encode(password);
    }
}
