// services/demand-account/src/main/java/com/buildabank/account/web/TransferController.java
package com.buildabank.account.web;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.buildabank.account.domain.Account;
import com.buildabank.account.service.TransferService;

/** REST API for accounts and transfers. Money movement always uses the safe (pessimistic-lock) path. */
@RestController
public class TransferController {

    private final TransferService transfers;

    public TransferController(TransferService transfers) {
        this.transfers = transfers;
    }

    /** Open an account → 201 Created. */
    @PostMapping("/api/accounts")
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody OpenAccountRequest request) {
        Account account = transfers.openAccount(
                request.accountNumber(), request.currency(), request.openingBalance());
        return ResponseEntity
                .created(URI.create("/api/accounts/" + account.getAccountNumber()))
                .body(AccountResponse.from(account));
    }

    /** Read an account's balance → 200, or 404 if it doesn't exist. */
    @GetMapping("/api/accounts/{accountNumber}")
    public ResponseEntity<AccountResponse> balance(@PathVariable String accountNumber) {
        try {
            return ResponseEntity.ok(new AccountResponse(
                    accountNumber, null, transfers.balanceOf(accountNumber)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Move money → 200 with the transaction id (safe, pessimistic-lock transfer). */
    @PostMapping("/api/transfers")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        UUID transactionId = transfers.transfer(
                request.from(), request.to(), request.amount(), request.description());
        return ResponseEntity.ok(new TransferResponse(transactionId));
    }
}
