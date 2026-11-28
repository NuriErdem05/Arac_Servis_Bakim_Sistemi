package com.nurierdem.vehicle_service.controller;

import com.nurierdem.vehicle_service.dto.AuthResponse.UserInfo;
import com.nurierdem.vehicle_service.dto.CreateUserRequest;
import com.nurierdem.vehicle_service.dto.TemporaryAccountResponse;
import com.nurierdem.vehicle_service.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AccountService accounts;

    public UserController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public ResponseEntity<List<UserInfo>> list() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(accounts.listAccounts());
    }

    @PostMapping
    public ResponseEntity<TemporaryAccountResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(accounts.createManagedAccount(request));
    }

    @PostMapping("/{id}/temporary-password")
    public ResponseEntity<TemporaryAccountResponse> renewTemporaryPassword(@PathVariable("id") Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(accounts.renewTemporaryPassword(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        accounts.archiveAccount(id);
    }
}
