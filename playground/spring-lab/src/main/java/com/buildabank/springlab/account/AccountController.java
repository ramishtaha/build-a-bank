// playground/spring-lab/src/main/java/com/buildabank/springlab/account/AccountController.java
package com.buildabank.springlab.account;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The "endpoint" half of the Phase-A capstone vertical slice: HTTP → service → in-memory store.
 * {@code GET /api/accounts/{id}} returns the account as JSON, or a clean 404 if it does not exist.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping
    public List<Account> all() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> byId(@PathVariable String id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
