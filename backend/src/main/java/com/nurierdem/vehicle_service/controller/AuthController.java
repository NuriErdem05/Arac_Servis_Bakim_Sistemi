package com.nurierdem.vehicle_service.controller;

import com.nurierdem.vehicle_service.dto.AuthResponse;
import com.nurierdem.vehicle_service.dto.AuthResponse.UserInfo;
import com.nurierdem.vehicle_service.dto.ChangeInitialPasswordRequest;
import com.nurierdem.vehicle_service.dto.LoginRequest;
import com.nurierdem.vehicle_service.dto.RegisterRequest;
import com.nurierdem.vehicle_service.entity.User;
import com.nurierdem.vehicle_service.security.AccountAccess;
import com.nurierdem.vehicle_service.service.AccountService;
import com.nurierdem.vehicle_service.service.AuthService;
import com.nurierdem.vehicle_service.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final AccountService accounts;
    private final AccountAccess accountAccess;

    public AuthController(AuthService authService, JwtService jwtService, AccountService accounts,
                          AccountAccess accountAccess) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.accounts = accounts;
        this.accountAccess = accountAccess;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return session(authService.authenticate(request));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserInfo register(@Valid @RequestBody RegisterRequest request) {
        return accounts.register(request);
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfo> me() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(accountAccess.current().user());
    }

    @PostMapping("/change-password")
    public ResponseEntity<AuthResponse> changeInitialPassword(@Valid @RequestBody ChangeInitialPasswordRequest request) {
        return session(accounts.changeInitialPassword(accountAccess.current(), request.password()));
    }

    private ResponseEntity<AuthResponse> session(User user) {
        AuthResponse response = new AuthResponse(jwtService.createAccessToken(user), UserInfo.from(user));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }
}
